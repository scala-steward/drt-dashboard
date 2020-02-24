package uk.gov.homeoffice.drt.pages

import scalatags.Text
import scalatags.Text.all._

object Layout {

  def apply(content: Text.TypedTag[String]): String = {
    html(
      head(
        link( href:= "/public/css/main.css", `type` := "text/css", rel:="stylesheet")
      ),
      body(content)).render
  }

}
