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

import ResearchKit
import UIKit
import FirebaseAnalytics

class InEligibilityStep: ORKStep {

  func showsProgress() -> Bool {
    return false
  }
}

class InEligibilityStepViewController: ORKStepViewController {

  // MARK: - Outlets
  @IBOutlet weak var buttonDone: UIButton?

  @IBOutlet weak var labelDescription: UILabel?

  var descriptionText: String?

  // MARK: - ORKStepViewController Intitialization Methods

  override init(step: ORKStep?) {
    super.init(step: step)
  }

  required init?(coder aDecoder: NSCoder) {
    super.init(coder: aDecoder)
  }

  override func hasNextStep() -> Bool {
    super.hasNextStep()
    return true
  }

  override func goForward() {
    NotificationCenter.default.post(name: Notification.Name("GoForward"), object: nil)
    super.goForward()
  }

  // MARK: - LifeCycle
  override func viewDidLoad() {
    super.viewDidLoad()
    buttonDone?.layer.borderColor = kUicolorForButtonBackground
  }

  // MARK: - Actions
  @IBAction func buttonActionDone(sender: UIButton?) {
    Analytics.logEvent(analyticsButtonClickEventsName, parameters: [
      buttonClickReasonsKey: "InEligibility Done"
    ])
    self.goForward()
    self.taskViewController?.dismiss(animated: true, completion: nil)
  }
}
