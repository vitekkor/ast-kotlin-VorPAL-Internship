package com.vitekkor

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

fun main(args: Array<String>): Unit = runBlocking {
    var resultOutput: List<String>
    val files = mutableListOf<String>()
    mainBody {
        ArgParser(args).parseInto(::Args).run {
            println("Creates a file queue...")
            val queue: Deque<File> = ArrayDeque(kotlinProjectOrFile.map { File(it) })
            while (queue.isNotEmpty()) {
                val file = queue.poll()
                for (f in file.walk())
                    if (f.isFile && f.extension == "kt") {
                        files.add(f.absolutePath)
                        println(f)
                    }
            }
            resultOutput = listOf(xmlFile, jsonFile, yamlFile)
            if (files.isEmpty()) {
                println("The path to the project or file does not contain kotlin files")
                return@mainBody
            }
            println("Done!\nStart...")
            async {
                val analyzer = AstAnalyzer(files)
                val result = analyzer.analyze()
                toOutput(result, resultOutput)
            }
        }
    }
}