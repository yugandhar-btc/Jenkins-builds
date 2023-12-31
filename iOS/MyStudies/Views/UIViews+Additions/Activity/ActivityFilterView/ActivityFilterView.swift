// License Agreement for FDA MyStudies
// Copyright © 2017-2023 Harvard Pilgrim Health Care Institute (HPHCI) and its Contributors. Permission is
// hereby granted, free of charge, to any person obtaining a copy of this software and associated
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
import FirebaseAnalytics

// MARK: - ActivitySchedules Class

enum ActivityFilterType: Int {
  case all = 0
  case surveys
  case tasks
}

protocol ActivityFilterViewDelegate: class {
  func setSelectedFilter(selectedIndex: ActivityFilterType)
}

class ActivityFilterView: UIView, UITableViewDelegate, UITableViewDataSource {

  // MARK: - Outlets
  @IBOutlet var tableview: UITableView?

  @IBOutlet var buttonCancel: UIButton!
  @IBOutlet var heightLayoutConstraint: NSLayoutConstraint!

  // MARK: - Properties
  weak var delegate: ActivityFilterViewDelegate?

  lazy var filterArray = ["All", "Surveys", "Tasks"]
  lazy var selectedIndex: ActivityFilterType = ActivityFilterType(rawValue: 0)!

  required init?(coder aDecoder: NSCoder) {
    super.init(coder: aDecoder)
    //fatalError("init(coder:) has not been implemented")
  }

  class func instanceFromNib(frame: CGRect, selectedIndex: ActivityFilterType)
    -> ActivityFilterView
  {
    let view =
      UINib(nibName: "ActivityFilterView", bundle: nil).instantiate(
        withOwner: nil,
        options: nil
      )[0] as! ActivityFilterView
    view.frame = frame

    view.selectedIndex = selectedIndex

    view.tableview?.register(UITableViewCell.self, forCellReuseIdentifier: "Cell")
    view.tableview?.delegate = view
    view.tableview?.dataSource = view
    let height = (3 * 44) + 104
    let maxViewHeight = Int(UIScreen.main.bounds.size.height - 67)
    view.heightLayoutConstraint.constant = CGFloat(
      (height > maxViewHeight) ? maxViewHeight : height
    )
    view.layoutIfNeeded()

    return view
  }

  // MARK: - Button Action
  @IBAction func buttonCancelClicked(_: UIButton) {
    Analytics.logEvent(analyticsButtonClickEventsName, parameters: [
      buttonClickReasonsKey: "Activity Filter Cancel"
    ])
    self.removeFromSuperview()
  }

}

extension ActivityFilterView {
  func numberOfSections(in tableView: UITableView) -> Int {
    return 1
  }

  func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
    return filterArray.count
  }

  func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

    let cell = tableView.dequeueReusableCell(withIdentifier: "Cell", for: indexPath)
    cell.textLabel?.font = UIFont(name: "HelveticaNeue-Light", size: 15)

    cell.textLabel?.text = filterArray[indexPath.row]

    if indexPath.row == self.selectedIndex.rawValue {
      cell.textLabel?.textColor = kBlueColor
    } else {
      cell.textLabel?.textColor = UIColor.gray
    }
    cell.textLabel?.textAlignment = .center
    return cell
  }

  func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
    tableView.deselectRow(at: indexPath, animated: true)
    Analytics.logEvent(analyticsButtonClickEventsName, parameters: [
      buttonClickReasonsKey: "ActivityFilterType Selected"
    ])
    self.selectedIndex = ActivityFilterType.init(rawValue: indexPath.row)!
    self.delegate?.setSelectedFilter(selectedIndex: self.selectedIndex)
    self.tableview?.reloadData()

    self.removeFromSuperview()
  }
}
