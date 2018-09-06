package codacy.metrics

import com.codacy.plugins.api.Source
import com.codacy.plugins.api.metrics.{FileMetrics, LineComplexity}
import org.specs2.mutable.Specification

class GoCycloSpec extends Specification {

  val expectedFileMetric =
    FileMetrics("codacy/metrics/hello.go", Some(1), None, None, Some(1), None, Set(LineComplexity(4, 1)))
  val expectedFileMetrics = List(expectedFileMetric)

  val targetDir = "src/test/resources"

  "GoCyclo" should {
    "get metrics" in {
      "all files within a directory" in {

        val fileMetricsMap =
          GoCyclo(source = Source.Directory(targetDir), language = None, files = None, options = Map.empty)

        fileMetricsMap.get should beEqualTo(expectedFileMetrics)
      }

      "specific files" in {

        val fileMetricsMap = GoCyclo(
          source = Source.Directory(targetDir),
          language = None,
          files = Some(Set(Source.File(expectedFileMetric.filename))),
          options = Map.empty)

        fileMetricsMap.get should beEqualTo(expectedFileMetrics)
      }
    }
  }
}
