package com.harvard.studyappmodule.studymodel;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ConsentPdfData extends RealmObject {
  @PrimaryKey private String StudyId;
  private String pdfPath;
  private String DataSharingImage;

  public String getDataSharingImage() {
    return DataSharingImage;
  }

  public void setDataSharingImage(String dataSharingImage) {
    DataSharingImage = dataSharingImage;
  }

  public String getStudyId() {
    return StudyId;
  }

  public void setStudyId(String studyId) {
    StudyId = studyId;
  }

  public String getPdfPath() {
    return pdfPath;
  }

  public void setPdfPath(String pdfPath) {
    this.pdfPath = pdfPath;
  }
}
