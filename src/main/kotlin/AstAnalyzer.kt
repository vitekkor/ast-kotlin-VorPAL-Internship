import kotlinx.ast.common.AstSource
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
                if (ast is KlassDeclaration && ast.keyword == "class") {
                    ast.identifier?.identifier?.let { klass ->
                        inheritances.put(
                            klass,
                            ast.inheritance.map { it.children }.flatten().map { (it as KlassIdentifier).identifier })
                    }
                }
                ((ast as KlassDeclaration).raw?.ast as DefaultAstNode).children.filter {it.description == "functionBody"}
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
}