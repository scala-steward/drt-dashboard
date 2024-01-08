package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.{ContentType, MediaTypes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.MockHttpClient
import uk.gov.homeoffice.drt.ports.PortCode
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayInputStream

class ExportConfigRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  def mockHttpClient(csvContent: String): MockHttpClient = MockHttpClient(() => csvContent)

  "Request test port config" should {
    "e-gate schedule data" in {
      Get("/export-config") ~>
        ExportConfigRoutes(mockHttpClient(
          s"""E-gates schedule
             |Terminal,Effective from,OpenGates per bank
             |T1,2020-01-01T0000,bank-1  10/10
             |""".stripMargin), Seq(PortCode("TEST"))) ~>
        check {
          contentType should ===(ContentType(MediaTypes.`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`))
          val responseBytes = responseAs[Array[Byte]]
          val inputStream = new ByteArrayInputStream(responseBytes)
          val workbook = new XSSFWorkbook(inputStream)
          workbook.getNumberOfSheets should be > 0
          val eGatesScheduleSheet = workbook.getSheet("TEST")
          eGatesScheduleSheet.getRow(0).getCell(0).getStringCellValue should be("E-gates schedule")
          eGatesScheduleSheet.getRow(1).getCell(0).getStringCellValue should be("Terminal")
          eGatesScheduleSheet.getRow(1).getCell(1).getStringCellValue should be("Effective from")
          eGatesScheduleSheet.getRow(1).getCell(2).getStringCellValue should be("OpenGates per bank")
          eGatesScheduleSheet.getRow(2).getCell(0).getStringCellValue should be("T1")
          eGatesScheduleSheet.getRow(2).getCell(1).getStringCellValue should be("2020-01-01T0000")
          eGatesScheduleSheet.getRow(2).getCell(2).getStringCellValue should be("bank-1  10/10")
        }
    }

    "excluding commas as delimiter" in {
      Get("/export-config") ~>
        ExportConfigRoutes(mockHttpClient(
          s""""Processing, Times"""".stripMargin), Seq(PortCode("TEST"))) ~>
        check {
          contentType should ===(ContentType(MediaTypes.`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`))
          val responseBytes = responseAs[Array[Byte]]
          val inputStream = new ByteArrayInputStream(responseBytes)
          val workbook = new XSSFWorkbook(inputStream)
          workbook.getNumberOfSheets should be > 0
          val queueSLAsSheet = workbook.getSheet("TEST")
          queueSLAsSheet.getRow(0).getCell(0).getStringCellValue should be("Processing, Times")
        }
    }

  }
}
