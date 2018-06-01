package codacy.metrics

import java.io

import codacy.docker.api.metrics.{FileMetrics, LineComplexity, MetricsTool}
import codacy.docker.api.{MetricsConfiguration, Source}
import com.codacy.api.dtos.{Language, Languages}
import com.codacy.docker.api.utils.{CommandResult, CommandRunner}

import scala.collection.immutable
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

object GoCyclo extends MetricsTool {

  override def apply(source: Source.Directory,
                     language: Option[Language],
                     files: Option[Set[Source.File]],
                     options: Map[MetricsConfiguration.Key, MetricsConfiguration.Value]): Try[List[FileMetrics]] = {

    language match {
      case Some(lang) if lang != Languages.Go =>
        Failure(new Exception(s"GoCyclo only supports Go. Provided language: $lang"))
      case _ =>
        GoCyclo.calculateComplexity(source.path, files)
    }
  }

  private def calculateComplexity(directory: String, files: Option[Set[Source.File]]): Try[List[FileMetrics]] = {
    val raw: Try[CommandResult] = runTool(directory, complexityCommand(files))

    raw.map((y: CommandResult) => parseOutput(y))
  }

  private def runTool(directory: String, command: Seq[String]): Try[CommandResult] = {
    val directoryOpt = Option(new io.File(directory))
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

  private def parseOutput(results: CommandResult): List[FileMetrics] = {
    val regex: Regex = """(\d*?) (.*?) (.*?) (.*):(\d*):(\d*)""".r

    val lineComplexities: Seq[(String, LineComplexity)] = results.stdout.flatMap {
      case regex(complexity, _, _, fileName, row, _) =>
        Option((fileName, LineComplexity(row.toInt, complexity.toInt)))
      case _ => None
    }

    val lineComplexitiesWithFilename: Map[String, Seq[(String, LineComplexity)]] = lineComplexities.groupBy(_._1)
    val lineComplexitiesByFilename: Map[String, Set[LineComplexity]] =
      lineComplexitiesWithFilename.mapValues(_.map(_._2)(collection.breakOut))
    lineComplexitiesByFilename.map {
      case (fileName, fileLineComplexities) =>
        FileMetrics(
          fileName,
          complexity = Some((fileLineComplexities.map(_.value) ++ Set(0)).max),
          loc = None,
          cloc = None,
          lineComplexities = fileLineComplexities,
          nrMethods = Option(fileLineComplexities.size) // since each line is a method/function
        )
    }(collection.breakOut)
  }

}
