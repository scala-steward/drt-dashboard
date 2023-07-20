package uk.gov.homeoffice.drt.uploadTraining

import uk.gov.homeoffice.drt.db.{FeatureGuideDao, FeatureGuideRow, FeatureGuideViewDao}

import scala.concurrent.Future


case class FeatureGuideService(featureGuideDao: FeatureGuideDao, featureGuideViewDao: FeatureGuideViewDao) {
  def updatePublishFeatureGuide(featureId: String, publish: Boolean) = {
    featureGuideDao.updatePublishFeatureGuide(featureId, publish)
  }

  def updateFeatureGuide(featureId: String, title: String, markdownContent: String) = {
    featureGuideDao.updateFeatureGuide(featureId, title, markdownContent)
    featureGuideViewDao.deleteViewForFeature(featureId)
  }

  def deleteFeatureGuide(featureId: String): Future[Int] = {
    featureGuideDao.deleteFeatureGuide(featureId)
  }

  def getFeatureGuides(): Future[Seq[FeatureGuideRow]] = {
    featureGuideDao.getFeatureGuides()
  }

  def insertFeatureGuide(fileName: String, title: String, markdownContent: String): Unit = {
    featureGuideDao.insertFeatureGuide(fileName, title, markdownContent)
  }
}
