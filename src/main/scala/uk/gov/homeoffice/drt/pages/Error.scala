package uk.gov.homeoffice.drt.pages

import scalatags.Text
import scalatags.Text.all._

object Error {

  def apply(message: String): Text.TypedTag[String] = div(h1("Dashboard Error"), div(message))

}
