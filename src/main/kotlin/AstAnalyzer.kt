import kotlinx.ast.common.AstSource
import kotlinx.ast.common.ast.Ast
import kotlinx.ast.common.ast.DefaultAstNode
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
                    if (ast.keyword == "class") ast.identifier?.identifier?.let { klass ->
                        inheritances[klass] =
                            ast.inheritance.map { it.children }.flatten().map { (it as KlassIdentifier).identifier }

                        print(ast.description + " ")

                        var a = ast.expressions.asSequence().filter { it.description == "classBody" }
                            .map { (it as DefaultAstNode).children }
                            .flatten()
                        println(countOverrides(a))
                        println(countFields(a))
                        var b = 0
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
        return astList.filter { (it as KlassDeclaration).keyword == "var" || it.keyword == "val" }.count()
    }

    private fun countOverrides(astList: Sequence<Ast>): Int {
       return astList.filter { (it as KlassDeclaration).keyword == "fun" }
            .map { (it as KlassDeclaration).modifiers.filter { it.modifier == "override" } }
            .count { it.isNotEmpty() }
    }
}