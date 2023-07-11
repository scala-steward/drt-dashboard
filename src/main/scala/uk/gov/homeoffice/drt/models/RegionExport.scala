package uk.gov.homeoffice.drt.models

import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike}

case class RegionExport(email: String,
                        region: String,
                        startDate: LocalDate,
                        endDate: LocalDate,
                        status: String,
                        createdAt: SDateLike,
                       )
