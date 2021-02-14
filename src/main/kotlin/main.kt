import kotlinx.ast.common.AstSource
import kotlinx.ast.common.ast.DefaultAstNode
import kotlinx.ast.common.klass.KlassDeclaration
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser

fun main(args: Array<String>) {
    AstAnalyzer().analyze("D:\\IdeaProjects\\ast-kotlin\\src\\main\\kotlin\\main.kt")
    val source = AstSource.File(
        "D:\\IdeaProjects\\ast-kotlin\\src\\main\\kotlin\\main.kt"
    )

    fun kekw() {
        val tr = 10
    }

    val kotlinFile = KotlinGrammarAntlrKotlinParser.parseKotlinFile(source)
    val a = kotlinFile.summary(true).onSuccess { list ->
        list.forEach {
            if (it is KlassDeclaration) {
                val b = (it.raw?.ast as DefaultAstNode).children.filter { it.description == "functionBody" }
                    .map { (it as DefaultAstNode).children }.flatten().filter { it.description == "block" }
                //println(b)
                b.map { (it as DefaultAstNode).children }.flatten().filter { it.description == "statements" }
                    .map { (it as DefaultAstNode).children }.flatten().filter { it.description == "statement" }
            }
        }
    }

}

class Helloworld() : ParentClass() {


}

open class ParentClass(): ParentClass2() {

}

open class ParentClass2() {

}