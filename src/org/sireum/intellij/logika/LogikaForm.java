/*
 Copyright (c) 2021, Robby, Kansas State University
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sireum.intellij.logika;

import javax.swing.*;

public abstract class LogikaForm {
    protected JPanel logikaPanel;
    protected JLabel timeoutLabel;
    protected JCheckBox checkSatCheckBox;
    protected JLabel logoLabel;
    protected JPanel headerPanel;
    protected JLabel titleLabel;
    protected JLabel subtitleLabel;
    protected JCheckBox backgroundCheckBox;
    protected JCheckBox hintCheckBox;
    protected JCheckBox inscribeSummoningsCheckBox;
    protected JPanel devPanel;
    protected JLabel bitsLabel;
    protected JRadioButton bitsUnboundedRadioButton;
    protected JPanel bitWidthPanel;
    protected JRadioButton bits8RadioButton;
    protected JRadioButton bits16RadioButton;
    protected JRadioButton bits32RadioButton;
    protected JRadioButton bits64RadioButton;
    protected JCheckBox hintUnicodeCheckBox;
    protected JPanel logoPanel;
    protected JTextField timeoutTextField;
    protected JCheckBox useRealCheckBox;
    protected JLabel fpRoundingModeLabel;
    protected JPanel fpPanel;
    protected JPanel fpRoundingModePanel;
    protected JPanel fpRoundingModeChoicePanel;
    protected JRadioButton fpRNERadioButton;
    protected JRadioButton fpRNARadioButton;
    protected JRadioButton fpRTPRadioButton;
    protected JRadioButton fpRTNRadioButton;
    protected JRadioButton fpRTZRadioButton;
    protected JPanel smt2Panel;
    protected JCheckBox smt2CacheCheckBox;
    protected JTextArea smt2ValidConfigsTextArea;
    protected JLabel smt2SatConfigsLabel;
    protected JLabel smt2ValidConfigsLabel;
    protected JTextArea smt2SatConfigsTextArea;
    protected JCheckBox smt2SeqCheckBox;
    protected JLabel rlimitLabel;
    protected JTextField rlimitTextField;
    protected JCheckBox smt2SimplifyCheckBox;
    protected JLabel defaultSmt2ValidConfigsLabel;
    protected JLabel defaultSmt2SatConfigsLabel;
    protected JPanel branchParPanel;
    protected JRadioButton branchParDisabledRadioButton;
    protected JLabel branchParLabel;
    protected JRadioButton branchParReturnsRadioButton;
    protected JRadioButton branchParAllRadioButton;
    protected JLabel branchParCoresLabel;
    protected JSpinner branchParCoresSpinner;
    protected JCheckBox splitConditionalsCheckBox;
    protected JCheckBox splitMatchCasesCheckBox;
    protected JCheckBox splitContractCasesCheckBox;
    protected JCheckBox hintLinesFreshCheckBox;
}
