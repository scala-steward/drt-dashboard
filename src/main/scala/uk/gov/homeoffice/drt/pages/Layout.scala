package uk.gov.homeoffice.drt.pages

import scalatags.Text
import scalatags.Text.all._

object Layout {

  def apply(pageContent: Text.TypedTag[String]): String = {
    html(
      head(
        meta(httpEquiv := "refresh", content := "60"),
        link(href := "/public/css/main.css", `type` := "text/css", rel := "stylesheet"),
        link(
          href := "https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css",
          `type` := "text/css",
          rel := "stylesheet")),
      body(cls := "drt-dashboard", pageContent)).render
  }

}
