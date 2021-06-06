package com.vitekkor

import kotlinx.ast.common.AstSource
import kotlinx.ast.common.ast.Ast
import kotlinx.ast.common.ast.DefaultAstNode
import kotlinx.ast.common.ast.rawAstOrNull
import kotlinx.ast.common.klass.KlassDeclaration
import kotlinx.ast.common.klass.KlassIdentifier
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class AstAnalyzer(filesQueue: List<String> = emptyList()) {
    private val functionList = ConcurrentLinkedQueue<DefaultAstNode>()

    @Volatile
    private var assignments = AtomicInteger(0)

    @Volatile
    private var branches = AtomicInteger(0)

    @Volatile
    private var conditions = AtomicInteger(0)

    private val queue = filesQueue.map { AstSource.File(it) }
    private val klassList = ConcurrentLinkedQueue<KlassDeclaration>()
    private val inheritances = ConcurrentHashMap<String, Set<String>>()
    private val overrides = ConcurrentHashMap<String, Int>()
    private val fields = ConcurrentHashMap<String, Int>()

    suspend fun analyze(): Statistic {
        val start = System.currentTimeMillis()
        coroutineScope {
            queue.map {
                async(Dispatchers.Default) {
                    analyze(it)
                }
            }.awaitAll()
        }
        val end = System.currentTimeMillis()
        println("\nTime - ${(end - start) / 1000.0}s\n")
        return getStatistic()
    }

    private fun analyze(source: AstSource.File) {
        try {
            val kotlinFile = KotlinGrammarAntlrKotlinParser.parseKotlinFile(source)
            kotlinFile.summary(false).onSuccess { astList ->
                println("Ast creation for ${source.filename} : Successful!\nStart analyzing...")
                astList.forEach { ast ->
                    if (ast is KlassDeclaration) {
                        when (ast.keyword) {
                            "class" -> analyzeKlass(ast)
                            "object" -> analyzeKlass(ast)
                            "interface" -> analyzeInterface(ast)
                            "fun" -> analyzeFun(ast as Ast)
                        }
                    }
                }
            }.onFailure { errors ->
                println("Can't create AST for file $source : $errors")
            }
            while (klassList.isNotEmpty() || functionList.isNotEmpty()) {
                analyzeKlass(klassList.poll())
                analyzeFun(functionList.poll())
            }
            println("Analysis ${source.filename} completed!")
        } catch (e: Exception) {
            println("Can't analyze ${source.filename}: ${e.message}")
        }
    }

    private fun analyzeFun(function: Ast?) {
        if (function == null) return
        val functionBodies =
            ((function.rawAstOrNull()?.ast ?: function) as DefaultAstNode).children.filterIsInstance<DefaultAstNode>()
                .filter { it.description == "functionBody" }.map { it.children }.flatten()
                .filterIsInstance<DefaultAstNode>()
                .map { it.children.filterIsInstance<DefaultAstNode>() }.flatten()
                .map { it.children }
                .flatMap {
                    val result = ArrayList<DefaultAstNode>()
                    for (element in it) {
                        if (element is DefaultAstNode && element.description == "statement") result.add(element)
                    }
                    result
                }.map { it.children }.flatten()

        assignments.addAndGet(functionBodies.count { it.description == "assignment" })

        var ast = functionBodies

        val result = mutableListOf<Ast>()

        while (ast.isNotEmpty()) {
            ast = ast.filterIsInstance<DefaultAstNode>().map { it.children }.flatten()
            ast.forEach { if (it.description == "primaryExpression") result.add(it) }
        }

        conditions.addAndGet(result.count { (it as DefaultAstNode).children[0].description == "ifExpression" })
        branches.addAndGet(result.count { (it as DefaultAstNode).children[0].description == "simpleIdentifier" })

        functionList.addAll(functionBodies.map { functionBody -> (functionBody as DefaultAstNode).children.filter { it.description == "functionDeclaration" } }
            .flatten().map { it as DefaultAstNode })
    }

    private fun analyzeInterface(`interface`: KlassDeclaration) {
        `interface`.identifier?.identifier?.let { klass ->
            inheritances[klass] =
                `interface`.inheritance.map { it.children }.flatten().map { (it as KlassIdentifier).identifier }
                    .toHashSet()
        }
    }

    private fun analyzeKlass(klassDeclaration: KlassDeclaration?) {
        if (klassDeclaration == null) return
        klassDeclaration.identifier?.identifier?.let { klass ->
            inheritances[klass] =
                klassDeclaration.inheritance.map { it.children }.flatten().map { (it as KlassIdentifier).identifier }
                    .toHashSet()
        }

        val klassBody = klassDeclaration.expressions.asSequence().filter { it.description == "classBody" }
            .map { (it as DefaultAstNode).children }.flatten()

        overrides[klassDeclaration.description] = countOverrides(klassBody)

        val companionObject = klassDeclaration.expressions.asSequence().filter { it.description == "classBody" }
            .map { (it as DefaultAstNode).children }.flatten().filter { it.description == "companionObject" }
            .map { (it as DefaultAstNode).children.filterIsInstance<DefaultAstNode>() }.flatten()
            .map { it.children.filterIsInstance<DefaultAstNode>() }.flatten()
            .map { it.children.filterIsInstance<DefaultAstNode>() }.flatten()
            .map { it.children.filterIsInstance<DefaultAstNode>() }.flatten()
            .map { it.children.filterIsInstance<DefaultAstNode>() }.flatten().toList()

        var ast = companionObject.map { it.children }.flatten()
        val fieldsInCompanionObj: MutableList<Ast> = mutableListOf()
        while (ast.isNotEmpty()) {
            ast = ast.filterIsInstance<DefaultAstNode>().map { it.children }.flatten()
            ast.forEach { if (it.description == "primaryExpression") fieldsInCompanionObj.add(it) }
        }

        fields[klassDeclaration.description] = countFields(klassBody, klassDeclaration) + fieldsInCompanionObj.count()

        functionList.addAll(klassBody.filter { it is KlassDeclaration && it.keyword == "fun" }
            .map { it.rawAstOrNull()?.ast as DefaultAstNode })
        functionList.addAll(
            companionObject.filter { it.description == "functionDeclaration" }
                .map { funDeclaration -> funDeclaration.children.filter { it.description == "functionBody" } }.flatten()
                .filterIsInstance<DefaultAstNode>()
        )

        klassList.addAll(
            klassBody.filterIsInstance<KlassDeclaration>().filter { klassBodyPart -> klassBodyPart.keyword == "class" }
                .asIterable()
        )
    }

    private fun countFields(astList: Sequence<Ast>, ast: KlassDeclaration): Int {
        val inConstructor = ast.children.filter { child -> child.description == "KlassDeclaration(constructor)" }
            .map { child ->
                (child as KlassDeclaration).children.filter {
                    it is KlassDeclaration && (it.keyword == "val" || it.keyword == "var")
                }
            }.flatten().count()
        val inClassBody = astList.filter {
            it is KlassDeclaration && (it.keyword == "var" || it.keyword == "val")
        }.count()
        return inConstructor + inClassBody
    }

    private fun countOverrides(astList: Sequence<Ast>): Int {
        return astList.filter { it is KlassDeclaration && it.keyword == "fun" }
            .map { (it as KlassDeclaration).modifiers.filter { modifier -> modifier.modifier == "override" } }
            .count { it.isNotEmpty() }
    }

    private fun getStatistic(): Statistic {
        do {
            var inheritancesLeft = 0
            inheritances.mapValuesTo(inheritances) {
                val extendedInheritances = mutableSetOf<String>().also { list -> list.addAll(it.value) }
                for (inheritance in it.value) {
                    if (extendedInheritances.addAll(
                            inheritances.getOrDefault(
                                inheritance,
                                emptySet()
                            )
                        )
                    ) inheritancesLeft++
                }
                extendedInheritances
            }
        } while (inheritancesLeft != 0)

        val averageInheritances = inheritances.let {
            val klassCount = it.keys.count().toDouble()
            if (klassCount == 0.0) 0.0 else it.values.fold(0, { acc, set -> acc + set.count() }) / klassCount
        }
        val maxInheritances = inheritances.maxOfOrNull { it.value.count() } ?: 0

        val averageOverrides = overrides.let {
            val klassCount = it.keys.count().toDouble()
            if (klassCount == 0.0) 0.0 else it.values.fold(0, { acc, i -> acc + i }) / klassCount
        }

        val averageFields = fields.let {
            val klassCount = it.keys.count().toDouble()
            if (klassCount == 0.0) 0.0 else it.values.fold(0, { acc, i -> acc + i }) / klassCount
        }

        return Statistic(
            maxInheritances,
            averageInheritances,
            averageOverrides,
            averageFields,
            assignments.toInt(),
            branches.toInt(),
            conditions.toInt()
        )
    }

    data class Statistic(
        val maxInheritances: Int,
        val averageInheritances: Double,
        val averageOverrides: Double,
        val averageFields: Double,
        val assignments: Int,
        val branches: Int,
        val conditions: Int
    )
}