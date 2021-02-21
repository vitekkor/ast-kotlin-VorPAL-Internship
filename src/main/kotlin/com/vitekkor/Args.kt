package com.vitekkor

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.io.File

class Args(parser: ArgParser) {
    val kotlinProjectOrFile by parser.positionalList(help = "Paths to kotlin projects or files")
    val xmlFile by parser.storing("-x", "--xml", help = "Path to xml report file").default("")
    val jsonFile by parser.storing("-j", "--json", help = "Path to json report file").default("")
    val yamlFile by parser.storing("-y", "--yaml", help = "Path to yaml report file").default("")
}

fun toOutput(
    statistic: AstAnalyzer.Statistic,
    files: List<String> = emptyList()
) {
    val yaml = """
        Ast:
          Max_Inheritances: ${statistic.maxInheritances}
          Average_Inheritances: ${statistic.averageInheritances}
          Average_Overrides: ${statistic.averageOverrides}
          Average_Fields: ${statistic.averageFields}
          Assignments: ${statistic.assignments}
          Branches: ${statistic.branches}
          Conditions: ${statistic.conditions}
        """.trimIndent()

    println(yaml)

    val xml = """
        <?xml version="1.0" encoding="utf-8" ?>
        <Ast>
            <Max_Inheritances>${statistic.maxInheritances}</Max_Inheritances>
            <Average_Inheritances>${statistic.averageInheritances}</Average_Inheritances>
            <Average_Overrides>${statistic.averageOverrides}</Average_Overrides>
            <Average_Fields>${statistic.averageFields}</Average_Fields>
            <Assignments>${statistic.assignments}</Assignments>
            <Branches>${statistic.branches}</Branches>
            <Conditions>${statistic.conditions}</Conditions>
        </Ast>
            """.trimIndent()

    val json = """
        { 
          "Ast": {
            "Max_Inheritances": ${statistic.maxInheritances},
            "Average_Inheritances": ${statistic.averageInheritances},
            "Average_Overrides": ${statistic.averageOverrides},
            "Average_Fields": ${statistic.averageFields},
            "Assignments": ${statistic.assignments},
            "Branches": ${statistic.branches},
            "Conditions": ${statistic.conditions}
          }
        }
        """.trimIndent()


    files.forEachIndexed { i, file ->
        if (file.isNotBlank()) {
            val f = File(file)
            if (!f.isDirectory)
                f.bufferedWriter().use {
                    val stat = when (i) {
                        0 -> xml
                        1 -> json
                        else -> yaml
                    }
                    it.write(stat)
                }
        }
    }
}