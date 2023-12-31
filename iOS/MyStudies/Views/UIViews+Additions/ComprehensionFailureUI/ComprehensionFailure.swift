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

let kComprehensionFailureViewIdentifier = "ComprehensionFailure"

protocol ComprehensionFailureDelegate: class {
  func didTapOnRetry()
  func didTapOnCancel()
}

class ComprehensionFailure: UIView {

  @IBOutlet weak var buttonCancel: UIButton!
  @IBOutlet weak var buttonRetry: UIButton!
  @IBOutlet weak var labelDescription: UILabel!

  weak var delegate: ComprehensionFailureDelegate?

  required init?(coder aDecoder: NSCoder) {
    super.init(coder: aDecoder)
    // sets border color for bottom view
    buttonRetry?.layer.borderColor = kUicolorForButtonBackground
  }

  class func instanceFromNib(frame: CGRect, detail: [String: Any]?) -> ComprehensionFailure {

    let view =
      UINib(nibName: kComprehensionFailureViewIdentifier, bundle: nil).instantiate(
        withOwner: nil,
        options: nil
      )[0] as! ComprehensionFailure
    view.frame = frame
    view.buttonRetry?.layer.borderColor = kUicolorForButtonBackground

    view.layoutIfNeeded()
    return view

  }
    
  override func layoutSubviews() {
    buttonCancel.translatesAutoresizingMaskIntoConstraints = false
    buttonCancel.topAnchor.constraint(equalTo: self.topAnchor, constant: 40).isActive = true
    buttonCancel.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -16).isActive = true
  }
 
  // MARK: - Button Actions

  @IBAction func buttonCancelAction() {
    Analytics.logEvent(analyticsButtonClickEventsName, parameters: [
      buttonClickReasonsKey: "ComprehensionFailure Cancel"
    ])
    self.delegate?.didTapOnCancel()
    self.isHidden = true
    self.removeFromSuperview()
  }

  @IBAction func buttonRetryAction() {
    Analytics.logEvent(analyticsButtonClickEventsName, parameters: [
      buttonClickReasonsKey: "ComprehensionFailure Retry"
    ])
    self.delegate?.didTapOnRetry()
    self.isHidden = true
    self.removeFromSuperview()
  }
}
