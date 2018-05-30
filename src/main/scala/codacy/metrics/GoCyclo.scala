package codacy.metrics

import java.io

import codacy.docker.api.metrics.{FileMetrics, LineComplexity, MetricsTool}
import codacy.docker.api.{MetricsConfiguration, Source}
import com.codacy.api.dtos.Language
import com.codacy.docker.api.utils.{CommandResult, CommandRunner}

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

case class GoCycloFileComplexity(fileName: String, methods: Seq[GoCycloMethodComplexity])

case class GoCycloMethodComplexity(complexity: Int,
                                   packageName: String,
                                   functionName: String,
                                   fileName: String,
                                   row: Int,
                                   column: Int)

object GoCyclo extends MetricsTool {

  override def apply(source: Source.Directory,
                     language: Option[Language],
                     files: Option[Set[Source.File]],
                     options: Map[MetricsConfiguration.Key, MetricsConfiguration.Value]): Try[List[FileMetrics]] = {

    val filesComplexity: Try[Seq[GoCycloFileComplexity]] = GoCyclo.calculateComplexity(source.path, files)
    filesComplexity.foreach(_.foreach(println(_)))
    val analysedFiles: Try[Set[String]] = filesComplexity.map(_.map(_.fileName)(collection.breakOut))

    for {
      theFiles <- analysedFiles
      complexityResults <- filesComplexity
    } yield {
      theFiles.map(file => {
        val fileComplexity: Option[GoCycloFileComplexity] = complexityResults.find(_.fileName == file)
        val linesComplexity: Set[LineComplexity] =
          (for {
            complexity <- fileComplexity.toSeq
            methodComplexity <- complexity.methods
          } yield LineComplexity(methodComplexity.row, methodComplexity.complexity))(collection.breakOut)

        FileMetrics(
          file,
          complexity = fileComplexity.map(file => file.methods.map(_.complexity).:+(0).max),
          loc = None,
          cloc = None,
          lineComplexities = linesComplexity,
          nrMethods = Option(linesComplexity.size) // since each line is a method/function
        )
      })
    }.toList
  }

  def calculateComplexity(directory: String, files: Option[Set[Source.File]]): Try[Seq[GoCycloFileComplexity]] = {
    val raw: Try[CommandResult] = runTool(directory, complexityCommand(files))

    raw.map((y: CommandResult) => parseOutput(y))
  }

  private def runTool(directory: String, command: Seq[String]): Try[CommandResult] = {
    val directoryOpt = safeCall(s"Could not create java.io.File from $directory", new io.File(directory)).toOption
    CommandRunner.exec(command.toList, directoryOpt) match {
      case Right(output) =>
        Success(output)
      case Left(s) =>
        Failure(new Exception(s"GoCyclo::runTool could not run goCyclo on $directory: $s"))
    }
  }

  private def complexityCommand(files: Option[Set[Source.File]]): Seq[String] = {
    files match {
      case Some(paths) => {
        val relativePaths = paths.map(_.path)
        Seq("gocyclo") ++ relativePaths
      }
      case None => Seq("gocyclo", ".")
    }
  }

  private def parseOutput(results: CommandResult): Seq[GoCycloFileComplexity] = {
    val regex: Regex = """(\d*?) (.*?) (.*?) (.*):(\d*):(\d*)""".r

    val parsedLines: Seq[GoCycloMethodComplexity] = results.stdout.flatMap {
      case regex(complexity, packageName, functionName, fileName, row, column) =>
        Option(GoCycloMethodComplexity(complexity.toInt, packageName, functionName, fileName, row.toInt, column.toInt))
      case _ => None
    }

    parsedLines.groupBy(_.fileName).map(GoCycloFileComplexity.tupled(_))(collection.breakOut)
  }

  private def defaultException(message: String): Throwable = new Exception(message)

  def safeCall[T](message: String, call: => T): Try[T] = {
    Try {
      Option(call).fold[Try[T]](Failure(defaultException(message)))(Success(_))
    }.flatten
  }

}
