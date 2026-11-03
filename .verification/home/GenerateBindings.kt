import android.databinding.tool.LayoutXmlProcessor
import android.databinding.tool.util.RelativizableFile
import android.databinding.tool.writer.BaseLayoutModel
import android.databinding.tool.writer.JavaFileWriter
import android.databinding.tool.writer.toJavaFile
import android.databinding.tool.writer.toViewBinder
import java.io.File

fun main(args: Array<String>) {
    val destination = File(args[1]).apply { mkdirs() }
    val writer = object : JavaFileWriter() {
        override fun writeToFile(canonicalName: String, contents: String) {
            File(destination, canonicalName.replace('.', '/') + ".java").apply {
                parentFile.mkdirs()
                writeText(contents)
            }
        }
        override fun deleteFile(canonicalName: String) = Unit
    }
    val processor = LayoutXmlProcessor("com.htdev.movie.htplay", writer, { it }, true)
    val processed = File(destination.parentFile, "processed/layout").apply { mkdirs() }
    File(args[0]).listFiles()!!.filter { it.extension == "xml" }.forEach { input ->
        processor.processSingleFile(RelativizableFile.fromAbsoluteFile(input, null), File(processed, input.name), true, false)
    }
    processor.resourceBundle.validateAndRegisterErrors()
    processor.resourceBundle.allLayoutFileBundlesInSource.groupBy { it.fileName }.values.forEach {
        writer.writeToFile(BaseLayoutModel(it, null).toViewBinder().toJavaFile(false))
    }
    println("Generated ${processor.resourceBundle.allLayoutFileBundlesInSource.size} ViewBinding classes")
}
