// License Agreement for FDA MyStudies
// Copyright © 2017-2023 Harvard Pilgrim Health Care Institute (HPHCI) and its Contributors.
// Copyright 2023 Google LLC
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// documentation files (the &quot;Software&quot;), to deal in the Software without restriction, including without
// limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
// Software, and to permit persons to whom the Software is furnished to do so, subject to the following
// conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial
// portions of the Software.
// Funding Source: Food and Drug Administration (“Funding Agency”) effective 18 September 2014 as
// Contract no. HHSF22320140030I/HHSF22301006T (the “Prime Contract”).
// THE SOFTWARE IS PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
// INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
// PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
// OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.

import Foundation
import RealmSwift

class SyncUpdate {

  enum ServerType: String {
    case response, enrollment
  }

  static var sharedInstance = SyncUpdate()
  
  var syncCalled = false
  private init() {}

  private var lastSyncedObject: DBDataOfflineSync?

  @objc func updateData(isReachable: Bool) {
    if isReachable {
      // Start syncing Data.
      DispatchQueue.main.asyncAfter(deadline: .now()) {
        self.syncDataToServer()
      }
    }
  }

  /// SyncData to server, called to sync responses stored in offline mode to server.
  func syncDataToServer() {
    if !syncCalled {
      syncCalled = true
    guard let realm = DBHandler.getRealmObject(),
      let toBeSyncedData = realm.objects(DBDataOfflineSync.self).sorted(
        byKeyPath: "date",
        ascending: true
      ).first
    else {
      syncCalled = false
      return }
    lastSyncedObject = toBeSyncedData
    // Request params
    var params: [String: Any]?

    if let requestParams = toBeSyncedData.requestParams {
      params =
        try? JSONSerialization.jsonObject(
          with: requestParams,
          options: []
        ) as? JSONDictionary
    }

    // Header params
    var headers: [String: String]?

    if let requestHeaders = toBeSyncedData.headerParams {
      headers =
        try? JSONSerialization.jsonObject(
          with: requestHeaders,
          options: []
        ) as? [String: String]
    }

    let methodString = toBeSyncedData.method
    let server = toBeSyncedData.server

    if server == SyncUpdate.ServerType.enrollment.rawValue {
      let methodName = methodString?.components(separatedBy: ".").first ?? ""
      let enrollmentMethod = EnrollmentMethods(rawValue: methodName)
      if let method = enrollmentMethod?.method {
        EnrollServices().syncOfflineSavedData(
          method: method,
          params: params,
          headers: headers,
          delegate: self
        )
      }
    } else if server == SyncUpdate.ServerType.response.rawValue {
      let methodName = methodString?.components(separatedBy: "/").last ?? ""
      let registrationMethod = ResponseMethods(rawValue: methodName)
      if let method = registrationMethod?.method {
        ResponseServices().syncOfflineSavedData(
          method: method,
          params: params,
          headers: headers,
          delegate: self
        )
      }
    } else {
      syncCalled = false
    }
  }
  }

  private func deleteSyncedObject() {
    if let realm = DBHandler.getRealmObject() {
      if let syncObj = lastSyncedObject {
        
        // Delete Synced object from DB
        try? realm.write {
          realm.delete(syncObj)
        }
      } else {
        guard let toBeSyncedData = realm.objects(DBDataOfflineSync.self).sorted(
          byKeyPath: "date",
          ascending: true
        ).first
        else { return }
        //        lastSyncedObject = toBeSyncedData
        try? realm.write {
          realm.delete(toBeSyncedData)
        }
        
      }
    }
  }
}

// MARK: - Webservices Delegates
extension SyncUpdate: NMWebServiceDelegate {

  func startedRequest(_ manager: NetworkManager, requestName: NSString) {
    
    UserDefaults.standard.setValue("started", forKey: "sync")
    UserDefaults.standard.synchronize()
    
  }

  func finishedRequest(_ manager: NetworkManager, requestName: NSString, response: AnyObject?) {
    UserDefaults.standard.setValue("finished", forKey: "sync")
    UserDefaults.standard.synchronize()
    syncCalled = false
    deleteSyncedObject()
    syncDataToServer()
  }

  func failedRequest(_ manager: NetworkManager, requestName: NSString, error: NSError) {
    UserDefaults.standard.setValue("finished", forKey: "sync")
    UserDefaults.standard.synchronize()
    syncCalled = false
  }

}
