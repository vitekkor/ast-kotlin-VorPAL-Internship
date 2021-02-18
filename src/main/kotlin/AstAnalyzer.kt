import kotlinx.ast.common.AstSource
import kotlinx.ast.common.ast.Ast
import kotlinx.ast.common.ast.DefaultAstNode
import kotlinx.ast.common.klass.KlassDeclaration
import kotlinx.ast.common.klass.KlassIdentifier
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser
import java.util.*

class AstAnalyzer(filesQueue: List<String> = emptyList()) {
    private val queue = filesQueue.map { AstSource.File(it) }
    private val klassList: Deque<KlassDeclaration> = ArrayDeque()
    private val inheritances = mutableMapOf<String, Set<String>>()
    private val overrides = mutableMapOf<String, Int>()
    private val fields = mutableMapOf<String, Int>()

    fun analyze(fileName: String) {
        val source = AstSource.File(fileName)
        val kotlinFile = KotlinGrammarAntlrKotlinParser.parseKotlinFile(source)

        kotlinFile.summary(false).onSuccess { astList ->
            println("Ast creation for $fileName: Successful!\nStart analyzing...")
            astList.forEach { ast ->
                if (ast is KlassDeclaration) {
                    when (ast.keyword) {
                        "class" -> analyzeKlass(ast)
                        "interface" -> analyzeInterface(ast)
                    }

                    (ast.raw?.ast as DefaultAstNode).children.filter { it.description == "functionBody" }
                }

            }
        }.onFailure { errors ->
            println("Can't create AST for file $fileName : $errors")
        }

    }

    private fun analyzeInterface(`interface`: KlassDeclaration) {
        `interface`.identifier?.identifier?.let { klass ->
            inheritances[klass] =
                `interface`.inheritance.map { it.children }.flatten().map { (it as KlassIdentifier).identifier }
                    .toHashSet()
        }
    }

    private fun analyzeKlass(klassDeclaration: KlassDeclaration) {
        klassDeclaration.identifier?.identifier?.let { klass ->
            inheritances[klass] =
                klassDeclaration.inheritance.map { it.children }.flatten().map { (it as KlassIdentifier).identifier }
                    .toHashSet()
        }

        //print(klassDeclaration.description + " ")

        val klassBody = klassDeclaration.expressions.asSequence().filter { it.description == "classBody" }
            .map { (it as DefaultAstNode).children }.flatten()

        overrides[klassDeclaration.description] = countOverrides(klassBody)
        fields[klassDeclaration.description] = countFields(klassBody, klassDeclaration)

        /* companion object klassDeclaration.expressions.filter { it.description == "classBody" }
            .map { (it as DefaultAstNode).children }.flatten().filter { it.description == "companionObject" }
            .map { (it as DefaultAstNode).children.filterIsInstance(DefaultAstNode::class.java) }.flatten()
            .map { it.children.filterIsInstance(DefaultAstNode::class.java) }.flatten()
            .map { it.children.filterIsInstance(DefaultAstNode::class.java) }.flatten().filter("classMemberDeclaration")
            .map { (it as DefaultAstNode).children }.flatten().filter("declaration")
            .map { (it as DefaultAstNode).children }.flatten()

         */
        expandKlassList(klassBody)
        /*ast.expressions.filter { it.description == "classBody" }.map { (it as DefaultAstNode).children }
            .flatten()
            .map { (it.rawAstOrNull()?.ast as DefaultAstNode).children.filter { it.description == "functionBody" } }
            .flatten().map { (it as DefaultAstNode).children }.flatten()
            .filter { it.description == "block" }.map { (it as DefaultAstNode).children }.flatten()
    */
    }

    private fun expandKlassList(klassBody: Sequence<Ast>) {
        klassList.addAll(
            klassBody.filterIsInstance(KlassDeclaration::class.java)
                .filter { klassBodyPart -> klassBodyPart.keyword == "class" }.asIterable()
        )
    }

    fun analyze() {
        for (file in queue) {
            analyze(file.filename)
        }
    }

    private fun countFields(astList: Sequence<Ast>, ast: KlassDeclaration): Int {
        val inConstructor = ast.children.filter { child -> child.description == "KlassDeclaration(constructor)" }
            .map { child ->
                (child as KlassDeclaration).children.filter {
                    val k = (it as KlassDeclaration).keyword; k == "val" || k == "var"
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

    fun getStatistic() {
        do {
            var notAllAdded = 0
            inheritances.mapValuesTo(inheritances) {
                val extendedInheritances = mutableSetOf<String>().also { list -> list.addAll(it.value) }
                for (inheritance in it.value) {
                    if (extendedInheritances.addAll(inheritances.getOrDefault(inheritance, emptySet()))) notAllAdded++
                }
                extendedInheritances
            }
        } while (notAllAdded != 0)

        val averageInheritances = inheritances.let {
            val klassCount = it.keys.count()
            if (klassCount == 0) 0 else it.values.fold(0, { acc, set -> acc + set.count() }) / klassCount
        }
        val maxInheritances = inheritances.maxOfOrNull { it.value.count() } ?: 0

        val averageOverrides = overrides.let {
            val klassCount = it.keys.count()
            if (klassCount == 0) 0 else it.values.fold(0, { acc, i -> acc + i }) / klassCount
        }

        val averageFields = fields.let {
            val klassCount = it.keys.count()
            if (klassCount == 0) 0 else it.values.fold(0, { acc, i -> acc + i }) / klassCount
        }
    }
}