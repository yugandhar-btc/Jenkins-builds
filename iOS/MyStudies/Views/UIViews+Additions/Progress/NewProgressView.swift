//  Copyright 2023 Google LLC
//
//  Use of this source code is governed by an MIT-style
//  license that can be found in the LICENSE file or at
//  https://opensource.org/licenses/MIT.

import UIKit

class NewProgressView: UIView {

  // MARK: - Outlets
  @IBOutlet var gifView: UIImageView!
  @IBOutlet var messageLbl: UILabel!

  class func instanceFromNib(frame: CGRect) -> NewProgressView? {
    let view =
      UINib(nibName: "NewProgressView", bundle: nil).instantiate(
        withOwner: nil,
        options: nil
      )[0] as? NewProgressView
    view?.frame = frame
    view?.layoutIfNeeded()
    return view
  }

  func showLoader(with message: String = "") {
    self.gifView.image = UIImage.gifImageWithName(kResourceName)
    self.messageLbl.isHidden = message.isEmpty
    self.messageLbl.text = message
  }

}

class New2ProgressView: UIView {

  // MARK: - Outlets
  @IBOutlet var gifView: UIImageView!
  @IBOutlet var messageLbl: UILabel!

  class func instanceFromNib(frame: CGRect) -> New2ProgressView? {
    let view =
      UINib(nibName: "New2ProgressView", bundle: nil).instantiate(
        withOwner: nil,
        options: nil
      )[0] as? New2ProgressView
    view?.frame = frame
    view?.layoutIfNeeded()
    return view
  }

  func showLoader(with message: String = "") {
    self.gifView.image = UIImage.gifImageWithName(kResourceName)
    self.messageLbl.isHidden = message.isEmpty
    self.messageLbl.text = message
  }

}

class AnchorDateProgressView: UIView {

  // MARK: - Outlets
  @IBOutlet var gifView: UIImageView!
  @IBOutlet var messageLbl: UILabel!

  class func instanceFromNib(frame: CGRect) -> AnchorDateProgressView? {
    let view =
      UINib(nibName: "AnchorDateProgressView", bundle: nil).instantiate(
        withOwner: nil,
        options: nil
      )[0] as? AnchorDateProgressView
    view?.frame = frame
    view?.layoutIfNeeded()
    return view
  }

  func showLoader(with message: String = "") {
    self.gifView.image = UIImage.gifImageWithName(kResourceName)
    self.messageLbl.isHidden = message.isEmpty
    self.messageLbl.text = message
  }

}
