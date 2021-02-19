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
    var h = 0
    h = 5

    fun kekw() {
        val tr = 10
    }

    val kotlinFile = KotlinGrammarAntlrKotlinParser.parseKotlinFile(source)
    if (!Helloworld.equals(true)) print("")
    Helloworld(55, 55, "").trysom
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

class Helloworld(val trysom: Int, var t: Int, helpMe: String) : ParentClass(), SomeClass {
    override fun trrrrya() {
        val source = 65
        if ("gfgfg" == source.toString()) print("true")
        val ggVp = ParentClass2()
    }

    override fun tr() {

    }
    companion object {
        const val helloworld = 55
        val hayHi = 65
        fun helloWorld2() {

        }
    }

    class internalClass() {
        operator fun plus(other: internalClass) {}
    }

}

open class ParentClass() : ParentClass2() {

    override fun tr() {
    }

    var openVal12 = 5
}

open class ParentClass2(): SomeAbstractClass() {

    var openVal = 5

    var openVal1 = 5
    open fun trrrrya() {
        val tttrr = 0
        openVal++
    }

    open fun tr() {

    }

    open fun trrr() {

    }

    open fun sldksdl() {

    }
}

interface SomeClass: SomeClass2

interface SomeClass2

abstract class SomeAbstractClass