package uk.gov.homeoffice.drt.services.bx

import org.apache.poi.ss.usermodel.{DataFormatter, WorkbookFactory}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.tables.BorderCrossing
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.{A1, A2, N, S, T1, T2, T3, T4, T5}
import uk.gov.homeoffice.drt.time.{DateRange, SDate}

import java.io.File
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Try


class BxImporterTest extends AnyWordSpec with Matchers {

  "BxImporter" should {
    "import the BX data" in {
      val monthYearRegex = """.+Gate Type and Hour between 01 ([a-zA-Z]+) ([0-9]{4}).+""".r
      val f = new File("/home/rich/Downloads/PRAU BF Data Cell - DrT Monthly Report - Oct24.xlsx")
      val workbook = WorkbookFactory.create(f)

      val sheet = workbook.iterator().asScala.find(_.getSheetName == "Data Response").getOrElse(throw new Exception("Sheet not found"))

      val formatter = new DataFormatter()

      val fromMonthRow = sheet.iterator().asScala.dropWhile { row =>
        println(s"checking row ${row.getRowNum}")
        !row.cellIterator().asScala.exists { cell =>
          formatter.formatCellValue(cell) match {
            case monthYearRegex(month, year) =>
//              println(s"Month: $month, Year: $year")
              true
            case _ =>
              false
          }
        }
      }
      val (month, year) = fromMonthRow.next().cellIterator().asScala.toSeq.headOption match {
        case Some(cell) =>
          formatter.formatCellValue(cell) match {
            case monthYearRegex(month, year) =>
              (month, year)
            case _ =>
              throw new Exception("Month and year not found")
          }
        case None =>
          throw new Exception("Month and year not found")
      }
      println(s"Month: $month, Year: $year")

      val fromHeadingsRow = fromMonthRow.dropWhile { row =>
        println(s"checking row ${row.getRowNum}")
        val cells = row.cellIterator().asScala.toIndexedSeq

        if (cells.size < 2) {
          false
        } else {
          val cellMatch1 = formatter.formatCellValue(cells(0)) == "Port"
          val cellMatch2 = formatter.formatCellValue(cells(1)) == "Terminal"
          !(cellMatch1 && cellMatch2)
        }
      }

      println(s"Headings: ${fromHeadingsRow.next().cellIterator().asScala.map(formatter.formatCellValue).mkString(", ")}")

      val startDate = SDate(f"$year-${SDate.monthsOfTheYear.indexOf(month) + 1}%02d-01")
      val endDate = startDate.addMonths(1).addDays(-1)
      val dateRange = DateRange(startDate.toUtcDate, endDate.toUtcDate)

      val dataRows = fromHeadingsRow.drop(1)
      val cellOffset = 2

      val hourData = dataRows.toSeq.map { row =>
        val port = formatter.formatCellValue(row.getCell(cellOffset + 0))
        val gateType = formatter.formatCellValue(row.getCell(cellOffset + 2))
        val hour = formatter.formatCellValue(row.getCell(cellOffset + 3)).toInt
        val (portCode, terminal) = BxPorts.nameToCodeAndTerminal(port)

        val pax = dateRange.zipWithIndex.map { case (date, idx) =>
          Try(formatter.formatCellValue(row.getCell(cellOffset + 4 + idx)).toDouble.toInt).toOption.map { count =>
            (date, count)
          }
        }

        val data = BorderCrossing(PortCode(portCode), terminal, gateType, hour, pax.toList)
        println(s"Row: $data")
        data
      }
    }
  }
}

object BxPorts {
  val nameToCodeAndTerminal = Map(
    "Aberdeen Airport" -> ("ABZ", T1),
    "Belfast" -> ("BFS", T1),
    "Birmingham Airport T1" -> ("BHX", T1),
    "Birmingham Airport T2" -> ("BHX", T2),
    "Bournemouth Airport" -> ("BOH", T1),
    "Bristol Airport" -> ("BRS", T1),
    "Brize Norton" -> ("BZZ", T1),
    "Cardiff (Wales) Airport" -> ("CWL", T1),
    "East Midlands Airport" -> ("EMA", T1),
    "Edinburgh Airport" -> ("EDI", A1),
    "Edinburgh Airport T2" -> ("EDI", A2),
    "Exeter Airport" -> ("EXT", T1),
    "Gatwick Airport North" -> ("LGW", N),
    "Gatwick Airport South" -> ("LGW", S),
    "George Best Belfast City Airport" -> ("BHD", T1),
    "Glasgow Airport" -> ("GLA", T1),
    "Heathrow Airport T2" -> ("LHR", T2),
    "Heathrow Airport T3" -> ("LHR", T3),
    "Heathrow Airport T4" -> ("LHR", T4),
    "Heathrow Airport T5" -> ("LHR", T5),
    "Humberside Airport" -> ("HUY", T1),
    "Inverness Airport" -> ("INV", T1),
    "Leeds Bradford Airport" -> ("LBA", T1),
    "Liverpool John Lennon Airport" -> ("LPL", T1),
    "London Biggin Hill Airport" -> ("BQH", T1),
    "London City Airport" -> ("LCY", T1),
    "Luton Airport" -> ("LTN", T1),
    "Manchester Airport T1" -> ("MAN", T1),
    "Manchester Airport T2" -> ("MAN", T2),
    "Manchester Airport T3" -> ("MAN", T3),
    "Newcastle Airport" -> ("NCL", T1),
    "Newquay Airport" -> ("NQY", T1),
    "Norwich Airport" -> ("NWI", T1),
    "Prestwick Airport" -> ("PIK", T1),
    "Southampton Airport" -> ("SOU", T1),
    "Stansted Airport" -> ("STN", T1),
    "Teesside Airport" -> ("MME", T1),
  )
}
