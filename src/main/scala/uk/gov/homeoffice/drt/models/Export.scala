package uk.gov.homeoffice.drt.models

import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike}

case class Export(email: String,
                  terminals: String,
                  startDate: LocalDate,
                  endDate: LocalDate,
                  status: String,
                  createdAt: SDateLike,
                 )
