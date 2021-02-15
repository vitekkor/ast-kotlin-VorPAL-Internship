import kotlinx.ast.common.AstSource
import kotlinx.ast.common.ast.Ast
import kotlinx.ast.common.ast.DefaultAstNode
import kotlinx.ast.common.ast.rawAstOrNull
import kotlinx.ast.common.klass.KlassDeclaration
import kotlinx.ast.common.klass.KlassIdentifier
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser

class AstAnalyzer(filesQueue: List<String> = emptyList()) {
    private var assignments = 0
    private var branches = 0
    private var conditions = 0
    private val inheritances = mutableMapOf<String, List<String>>()
    private var queue = filesQueue.map { AstSource.File(it) }

    fun analyze(fileName: String) {
        val source = AstSource.File(fileName)
        val kotlinFile = KotlinGrammarAntlrKotlinParser.parseKotlinFile(source)

        kotlinFile.summary(false).onSuccess { astList ->
            println("Ast creation for $fileName: Successful!\nStart analyzing...")
            astList.forEach { ast ->
                if (ast is KlassDeclaration) {
                    if (ast.keyword == "class") {
                        ast.identifier?.identifier?.let { klass ->
                            inheritances[klass] =
                                ast.inheritance.map { it.children }.flatten().map { (it as KlassIdentifier).identifier }
                        }

                        print(ast.description + " ")

                        val classBody = ast.expressions.asSequence().filter { it.description == "classBody" }
                            .map { (it as DefaultAstNode).children }.flatten()
                        println(countOverrides(classBody))
                        println(countFields(classBody))
                        ast.expressions.filter { it.description == "classBody" }.map { (it as DefaultAstNode).children }
                            .flatten()
                            .map { (it.rawAstOrNull()?.ast as DefaultAstNode).children.filter { it.description == "functionBody" } }
                            .flatten().map { (it as DefaultAstNode).children }.flatten()
                            .filter { it.description == "block" }.map { (it as DefaultAstNode).children }.flatten()
                    }

                    (ast.raw?.ast as DefaultAstNode).children.filter { it.description == "functionBody" }
                }

            }
        }.onFailure { errors ->
            println("Can't create AST for file $fileName: $errors")
        }
    }

    fun analyze() {
        for (file in queue) {
            analyze(file.filename)
        }
    }

    private fun countFields(astList: Sequence<Ast>): Int {
        return astList.filter {
            val keyWord = (it as KlassDeclaration).keyword
            keyWord == "var" || keyWord == "val"
        }.count()
    }

    private fun countOverrides(astList: Sequence<Ast>): Int {
        return astList.filter { (it as KlassDeclaration).keyword == "fun" }
            .map { (it as KlassDeclaration).modifiers.filter { modifier -> modifier.modifier == "override" } }
            .count { it.isNotEmpty() }
    }
}