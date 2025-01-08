package uk.gov.homeoffice.drt.services.bx

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import org.apache.poi.ss.usermodel.{DataFormatter, Row, Sheet, WorkbookFactory}
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.db.tables.{BorderCrossing, GateType}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{DateRange, SDate, UtcDate}

import java.io.File
import scala.concurrent.Future
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.{Failure, Success, Try}

object ImportBorderCrossings {
  private val log = LoggerFactory.getLogger(getClass)

  private val monthYearRegex = """.+Gate Type and Hour between 01 ([a-zA-Z]+) ([0-9]{4}).+""".r
  private val cellOffset = 2
  private val dateStartOffset = 4

  def apply(replaceHoursForPortTerminal: (PortCode, Terminal, GateType, Iterable[BorderCrossing]) => Future[Int])
           (implicit mat: Materializer): String => Future[Int] =
    filePath => {
      val file = new File(filePath)
      val workbook = WorkbookFactory.create(file)

      val sheet = workbook.iterator().asScala.find(_.getSheetName == "Data Response").getOrElse(throw new Exception("Sheet not found"))
      val formatter: DataFormatter = new DataFormatter()

      val fromMonthRow = findMonthRow(sheet, formatter)
      val (month, year) = extractMonthAndYear(formatter, fromMonthRow)
      val fromHeadingsRow = findHeadingsRow(fromMonthRow, formatter)

      val startDate = SDate(f"$year-${SDate.monthsOfTheYear.indexOf(month) + 1}%02d-01")
      val endDate = startDate.addMonths(1).addDays(-1)
      val dateRange = DateRange(startDate.toUtcDate, endDate.toUtcDate)

      Source(fromHeadingsRow.drop(1).toSeq)
        .flatMapConcat { row =>
          Try {
            val bxPort = formatter.formatCellValue(row.getCell(cellOffset + 0))
            val bxTerminal = formatter.formatCellValue(row.getCell(cellOffset + 1))
            val gateType = formatter.formatCellValue(row.getCell(cellOffset + 2))
            val hour = formatter.formatCellValue(row.getCell(cellOffset + 3)).toInt
            val (portCode, terminal) = getDrtPortAndTerminal(bxPort, bxTerminal)

            Source(dateRange.zipWithIndex)
              .mapAsync(1) { case (date, idx) =>
                val paxCountCellIdx = cellOffset + dateStartOffset + idx
                val cellStr = formatter.formatCellValue(row.getCell(paxCountCellIdx))
                if (cellStr.nonEmpty)
                  parseAndRecordPax(gateType, hour, portCode, terminal, date, cellStr, replaceHoursForPortTerminal)
                else
                  Future.successful(0)
              }
          } match {
            case Success(source) => source
            case Failure(exception) =>
              log.info(s"Skipping row ${row.getRowNum}: ${exception.getMessage}")
              Source.empty
          }
        }
        .runWith(Sink.fold(0)(_ + _))
    }

  private def parseAndRecordPax(gateType: String,
                                hour: Int,
                                portCode: String,
                                terminal: String,
                                date: UtcDate,
                                cellStr: String,
                                replaceHoursForPortTerminal: (PortCode, Terminal, GateType, Iterable[BorderCrossing]) => Future[Int],
                               ): Future[Int] = {
    Try(cellStr.toDouble.toInt).map { count =>
      (date, count)
    } match {
      case Success((date, count)) =>
        val crossing = BorderCrossing(PortCode(portCode), Terminal(terminal), date, GateType(gateType), hour, count)
        replaceHoursForPortTerminal(PortCode(portCode), Terminal(terminal), GateType(gateType), Seq(crossing))
      case Failure(exception) =>
        log.error(s"Failed to parse count for $portCode, $terminal, $date, $gateType, $hour", exception)
        Future.successful(0)
    }
  }

  private def findHeadingsRow(fromMonthRow: Iterator[Row], formatter: DataFormatter): Iterator[Row] = {
    fromMonthRow.dropWhile { row =>
      val cells = row.cellIterator().asScala.toIndexedSeq

      if (cells.size < (cellOffset + 2)) {
        false
      } else {
        val cellMatch1 = formatter.formatCellValue(cells(cellOffset + 0)) == "Port"
        val cellMatch2 = formatter.formatCellValue(cells(cellOffset + 1)) == "Terminal"
        !(cellMatch1 && cellMatch2)
      }
    }
  }

  private def extractMonthAndYear(formatter: DataFormatter, fromMonthRow: Iterator[Row]): (String, String) = {
    val maybeContentCell = fromMonthRow
      .next().cellIterator().asScala.toSeq
      .dropWhile(_.getColumnIndex < cellOffset)
      .headOption

    maybeContentCell match {
      case Some(cell) =>
        formatter.formatCellValue(cell) match {
          case monthYearRegex(month, year) =>
            (month, year)
          case _ =>
            throw new Exception("Month and year not found")
        }
      case None =>
        throw new Exception("Month and year not found - no cells found")
    }
  }

  private def findMonthRow(sheet: Sheet, formatter: DataFormatter): Iterator[Row] = {
    sheet.iterator().asScala.dropWhile { row =>
      !row.cellIterator().asScala.exists { cell =>
        formatter.formatCellValue(cell) match {
          case monthYearRegex(_, _) =>
            true
          case _ =>
            false
        }
      }
    }
  }

  private val bxToDrtPortAndTerminal: Map[(String, String), (String, String)] = Map(
    ("Aberdeen", "") -> ("ABZ", "T1"),
    ("Belfast", "") -> ("BFS", "T1"),
    ("Birmingham", "Terminal 1") -> ("BHX", "T1"),
    ("Birmingham", "Terminal 2") -> ("BHX", "T2"),
    ("Bournemouth", "") -> ("BOH", "T1"),
    ("Bristol", "") -> ("BRS", "T1"),
    ("Brize Norton", "") -> ("BZZ", "T1"),
    ("Cardiff", "") -> ("CWL", "T1"),
    ("East Midlands", "") -> ("EMA", "T1"),
    ("Edinburgh", "Terminal 1") -> ("EDI", "A1"),
    ("Edinburgh", "Terminal 2") -> ("EDI", "A2"),
    ("Exeter", "") -> ("EXT", "T1"),
    ("Gatwick", "North") -> ("LGW", "N"),
    ("Gatwick", "South") -> ("LGW", "S"),
    ("George Best Belfast City", "") -> ("BHD", "T1"),
    ("Glasgow", "") -> ("GLA", "T1"),
    ("Heathrow", "Terminal 2") -> ("LHR", "T2"),
    ("Heathrow", "Terminal 3") -> ("LHR", "T3"),
    ("Heathrow", "Terminal 4") -> ("LHR", "T4"),
    ("Heathrow", "Terminal 5") -> ("LHR", "T5"),
    ("Humberside", "") -> ("HUY", "T1"),
    ("Inverness", "") -> ("INV", "T1"),
    ("Leeds Bradford", "") -> ("LBA", "T1"),
    ("Liverpool John Lennon", "") -> ("LPL", "T1"),
    ("London Biggin Hill", "") -> ("BQH", "T1"),
    ("London City", "") -> ("LCY", "T1"),
    ("Luton", "") -> ("LTN", "T1"),
    ("Manchester", "Terminal 1") -> ("MAN", "T1"),
    ("Manchester", "Terminal 2") -> ("MAN", "T2"),
    ("Manchester", "Terminal 3") -> ("MAN", "T3"),
    ("Newcastle", "") -> ("NCL", "T1"),
    ("Newquay", "") -> ("NQY", "T1"),
    ("Norwich", "") -> ("NWI", "T1"),
    ("Prestwick", "") -> ("PIK", "T1"),
    ("Southampton", "") -> ("SOU", "T1"),
    ("Southend", "") -> ("SEN", "T1"),
    ("Stansted", "") -> ("STN", "T1"),
    ("Teesside", "") -> ("MME", "T1"),
  )

  private def getDrtPortAndTerminal(bxPort: String, bxTerminal: String): (String, String) =
    bxToDrtPortAndTerminal.keys
      .find {
        case (p, t) => bxPort.indexOf(p) == 0 && (bxTerminal.contains(t) || t.isEmpty)
      }
      .map(bxToDrtPortAndTerminal)
      .getOrElse(throw new Exception(s"Port not found for $bxPort, $bxTerminal"))
}
