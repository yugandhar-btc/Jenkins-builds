/*
 * Copyright © 2017-2019 Harvard Pilgrim Health Care Institute (HPHCI) and its Contributors.
 * Copyright 2020 Google LLC
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * Funding Source: Food and Drug Administration (“Funding Agency”) effective 18 September 2014 as Contract no. HHSF22320140030I/HHSF22301006T (the “Prime Contract”).
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.harvard.studyappmodule.custom.question;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import com.harvard.R;
import com.harvard.studyappmodule.custom.QuestionStepCustom;
import com.harvard.utils.Logger;
import java.util.ArrayList;
import org.researchstack.backbone.result.StepResult;
import org.researchstack.backbone.step.Step;
import org.researchstack.backbone.ui.step.body.BodyAnswer;
import org.researchstack.backbone.ui.step.body.StepBody;

public class TimeIntervalQuestion implements StepBody {
  private QuestionStepCustom step;
  private StepResult<Double> result;
  private TimeIntervalAnswerFormat format;
  private NumberPicker minpicker;
  private NumberPicker hourpicker;
  private Double currentSelected;
  private ArrayList<String> minlist;

  public TimeIntervalQuestion(Step step, StepResult result) {
    this.step = (QuestionStepCustom) step;
    this.result = result == null ? new StepResult<>(this.step) : result;
    this.format = (TimeIntervalAnswerFormat) this.step.getAnswerFormat1();

    if (result != null) {
      currentSelected = this.result.getResult();
    }
  }

  @Override
  public View getBodyView(int viewType, LayoutInflater inflater, ViewGroup parent) {
    View view = getViewForType(viewType, inflater, parent);

    Resources res = parent.getResources();
    LinearLayout.MarginLayoutParams layoutParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    layoutParams.leftMargin =
        res.getDimensionPixelSize(org.researchstack.backbone.R.dimen.rsb_margin_left);
    layoutParams.rightMargin =
        res.getDimensionPixelSize(org.researchstack.backbone.R.dimen.rsb_margin_right);
    view.setLayoutParams(layoutParams);

    return view;
  }

  private View getViewForType(int viewType, LayoutInflater inflater, ViewGroup parent) {
    if (viewType == VIEW_TYPE_DEFAULT) {
      return initViewDefault(inflater, parent);
    } else if (viewType == VIEW_TYPE_COMPACT) {
      return initViewCompact(inflater, parent);
    } else {
      throw new IllegalArgumentException("Invalid View Type");
    }
  }

  private View initViewDefault(LayoutInflater inflater, ViewGroup parent) {
    LinearLayout linearLayout = new LinearLayout(inflater.getContext());
    linearLayout.setOrientation(LinearLayout.VERTICAL);

    linearLayout.setLayoutParams(
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    View timeintervallayout = inflater.inflate(R.layout.timeintervallayout, parent, false);
    minpicker = (NumberPicker) timeintervallayout.findViewById(R.id.min);
    hourpicker = (NumberPicker) timeintervallayout.findViewById(R.id.hours);

    minlist = new ArrayList<>();
    for (int i = 0; i < 60; i++) {
      if (i % format.getStep() == 0) {
        minlist.add("" + i);
      }
    }
    String[] minvalue = new String[minlist.size()];
    minlist.toArray(minvalue);

    minpicker.setMinValue(0);
    minpicker.setMaxValue(minlist.size() - 1);
    minpicker.setWrapSelectorWheel(false);
    minpicker.setDisplayedValues(minvalue);

    String[] hrsvalue = new String[25];
    for (int i = 0; i < hrsvalue.length; i++) {
      hrsvalue[i] = "" + i;
    }

    hourpicker.setMinValue(0);
    hourpicker.setMaxValue(24);
    hourpicker.setWrapSelectorWheel(false);
    hourpicker.setDisplayedValues(hrsvalue);

    hourpicker.setOnValueChangedListener(
        new NumberPicker.OnValueChangeListener() {
          @Override
          public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            if (picker.getValue() == 24) {
              minpicker.setValue(0);
            }
          }
        });
    minpicker.setOnValueChangedListener(
        new NumberPicker.OnValueChangeListener() {
          @Override
          public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            if (hourpicker.getValue() == 24) {
              minpicker.setValue(0);
            }
          }
        });

    if (currentSelected != null) {
      setpickerval(currentSelected);
    } else {
      long defaultval;
      if (format.getDefaultvalue() != null && !format.getDefaultvalue().equalsIgnoreCase("")) {
        try {
          defaultval = Integer.parseInt(format.getDefaultvalue());
        } catch (NumberFormatException e) {
          Logger.log(e);
          defaultval = 0;
        }
      } else {
        defaultval = 0;
      }
      double floorval = (int) defaultval / (format.getStep() * 60);
      double finalval = floorval * (format.getStep() * 60);
      double hour = finalval / 3600;
      setpickerval(hour);
    }
    linearLayout.addView(timeintervallayout);
    return linearLayout;
  }

  private View initViewCompact(LayoutInflater inflater, ViewGroup parent) {
    ViewGroup compactView = (ViewGroup) initViewDefault(inflater, parent);

    TextView label =
        (TextView) inflater.inflate(org.researchstack.backbone.R.layout.rsb_item_text_view_title_compact, compactView, false);
    label.setText(step.getTitle());

    compactView.addView(label, 0);

    return compactView;
  }

  private void setpickerval(Double currentSelected) {
    int seconds = (int) (currentSelected * 3600d);
    int hours = seconds / 3600;
    int minutes = (seconds % 3600) / 60;
    hourpicker.setValue(hours);
    minpicker.setValue(minlist.indexOf("" + minutes));
  }

  @Override
  public StepResult getStepResult(boolean skipped) {
    if (skipped) {
      result.setResult(null);
    } else {
      int seconds =
          (hourpicker.getValue() * 60 * 60)
              + (Integer.parseInt(minlist.get(minpicker.getValue())) * 60);
      double hour = Double.parseDouble("" + seconds) / 3600d;
      result.setResult(hour);
    }
    return result;
  }

  @Override
  public BodyAnswer getBodyAnswerState() {
    return BodyAnswer.VALID;
  }
}
