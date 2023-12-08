package uk.gov.homeoffice.drt.notifications

import org.mockito.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.db.UserAccessRequest
import uk.gov.service.notify.NotificationClientApi

class EmailNotificationsSpec extends Specification {

  "EmailNotification dropInLink" >> {
    "for user with single port access requested" >> {
      val emailClient = Mockito.mock(classOf[NotificationClientApi])
      val emailNotifications = EmailNotifications(List.empty, emailClient)
      emailNotifications.getDropInBookingUrlForAPort(
        UserAccessRequest(email = "test@test.com",
          portsRequested = "LHR",
          allPorts = false,
          regionsRequested = "",
          staffEditing = false,
          lineManager = "",
          agreeDeclaration = true,
          accountType = "port",
          portOrRegionText = "testing",
          staffText = "",
          status = "Approved",
          requestTime = java.sql.Timestamp.valueOf("2021-01-01 00:00:00.0")
        ).portsRequested,
        "drt-test.gov.uk") === "https://lhr.drt-test.gov.uk/#trainingHub/dropInBooking"
    }

    "for user with multiple ports access requested" >> {
      val emailClient = Mockito.mock(classOf[NotificationClientApi])
      val emailNotifications = EmailNotifications(List.empty, emailClient)
      emailNotifications.getDropInBookingUrlForAPort(
        UserAccessRequest(email = "test@test.com",
          portsRequested = "LGW,NQY,CWL,SOU,SEN,BRS,EXT,BOH",
          allPorts = false,
          regionsRequested = "South",
          staffEditing = false,
          lineManager = "",
          agreeDeclaration = true,
          accountType = "port",
          portOrRegionText = "testing",
          staffText = "",
          status = "Approved",
          requestTime = java.sql.Timestamp.valueOf("2021-01-01 00:00:00.0")
        ).portsRequested,
        "drt-test.gov.uk") === "https://lgw.drt-test.gov.uk/#trainingHub/dropInBooking"
    }

    "for user with regional port access requested" >> {
      val emailClient = Mockito.mock(classOf[NotificationClientApi])
      val emailNotifications = EmailNotifications(List.empty, emailClient)
      emailNotifications.getDropInBookingUrlForAPort(
        UserAccessRequest(email = "test@test.com",
          portsRequested = "GLA,LPL,MME,MAN,LBA,NCL,HUY,ABZ,EDI,PIK,INV,BHD,BFS",
          allPorts = false,
          regionsRequested = "North",
          staffEditing = false,
          lineManager = "",
          agreeDeclaration = true,
          accountType = "rccu",
          portOrRegionText = "testing",
          staffText = "",
          status = "Approved",
          requestTime = java.sql.Timestamp.valueOf("2021-01-01 00:00:00.0")
        ).portsRequested,
        "drt-test.gov.uk") === "https://gla.drt-test.gov.uk/#trainingHub/dropInBooking"
    }
  }
}
