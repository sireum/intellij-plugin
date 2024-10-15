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

package org.sireum.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Locale;

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
    protected JLabel bitsLabel;
    protected JRadioButton bitsUnboundedRadioButton;
    protected JRadioButton bits8RadioButton;
    protected JRadioButton bits16RadioButton;
    protected JRadioButton bits32RadioButton;
    protected JRadioButton bits64RadioButton;
    protected JCheckBox hintUnicodeCheckBox;
    protected JPanel logoPanel;
    protected JTextField timeoutTextField;
    protected JCheckBox useRealCheckBox;
    protected JLabel fpRoundingModeLabel;
    protected JRadioButton fpRNERadioButton;
    protected JRadioButton fpRNARadioButton;
    protected JRadioButton fpRTPRadioButton;
    protected JRadioButton fpRTNRadioButton;
    protected JRadioButton fpRTZRadioButton;
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
    protected JLabel hintMaxColumnLabel;
    protected JTextField hintMaxColumnTextField;
    protected JPanel additionalVerificationsPanel;
    protected JCheckBox infoFlowCheckBox;
    protected JLabel loopBoundLabel;
    protected JLabel callBoundLabel;
    protected JTextField loopBoundTextField;
    protected JTextField callBoundTextField;
    protected JCheckBox interpContractCheckBox;
    protected JCheckBox rawInscriptionCheckBox;
    protected JCheckBox elideEncodingCheckBox;
    protected JCheckBox transitionCacheCheckBox;
    protected JCheckBox coverageCheckBox;
    protected JTabbedPane tabbedPane;
    protected JPanel smt2BitWidthPanel;
    protected JPanel smt2FPPanel;
    protected JPanel smt2ConfigPanel;
    protected JPanel parPanel;
    protected JLabel parLabel;
    protected JSpinner coverageIntensitySpinner;
    protected JCheckBox patternExhaustiveCheckBox;
    protected JCheckBox pureFunCheckBox;
    protected JCheckBox detailedInfoCheckBox;
    protected JLabel spModeLabel;
    protected JRadioButton spModeDefaultRadioButton;
    protected JRadioButton spModeFlipRadioButton;
    protected JRadioButton spModeUninterpretedRadioButton;
    protected JCheckBox satTimeoutCheckBox;
    protected JLabel modeLabel;
    protected JRadioButton modeAutoRadioButton;
    protected JRadioButton modeManualRadioButton;
    protected JCheckBox hintAtRewriteCheckBox;
    protected JCheckBox searchPcCheckBox;
    protected JCheckBox rwTraceCheckBox;
    protected JLabel rwMaxLabel;
    protected JTextField rwMaxTextField;
    protected JCheckBox rwParCheckBox;
    protected JCheckBox rwEvalTraceCheckBox;

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        logikaPanel = new JPanel();
        logikaPanel.setLayout(new GridLayoutManager(5, 3, new Insets(0, 0, 0, 0), -1, -1));
        headerPanel = new JPanel();
        headerPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        logikaPanel.add(headerPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        subtitleLabel = new JLabel();
        Font subtitleLabelFont = this.$$$getFont$$$(null, Font.BOLD, 12, subtitleLabel.getFont());
        if (subtitleLabelFont != null) subtitleLabel.setFont(subtitleLabelFont);
        subtitleLabel.setText("The Sireum Verification Framework");
        headerPanel.add(subtitleLabel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleLabel = new JLabel();
        Font titleLabelFont = this.$$$getFont$$$("Arial Black", Font.BOLD, 18, titleLabel.getFont());
        if (titleLabelFont != null) titleLabel.setFont(titleLabelFont);
        titleLabel.setText("Logika");
        headerPanel.add(titleLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        logoPanel = new JPanel();
        logoPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        headerPanel.add(logoPanel, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        logoLabel = new JLabel();
        logoLabel.setText("");
        logoPanel.add(logoLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        headerPanel.add(spacer1, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        logikaPanel.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 5), new Dimension(250, 5), new Dimension(-1, 5), 0, false));
        tabbedPane = new JTabbedPane();
        logikaPanel.add(tabbedPane, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 402), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("General", panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(13, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel2.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, 1, null, null, null, 0, false));
        backgroundCheckBox = new JCheckBox();
        backgroundCheckBox.setSelected(true);
        backgroundCheckBox.setText("Background analysis");
        panel2.add(backgroundCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(154, 21), null, 0, false));
        hintCheckBox = new JCheckBox();
        hintCheckBox.setSelected(true);
        hintCheckBox.setText("Generate programming logic hints");
        hintCheckBox.setToolTipText("Expose statement pre/post claims");
        panel2.add(hintCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hintUnicodeCheckBox = new JCheckBox();
        hintUnicodeCheckBox.setSelected(true);
        hintUnicodeCheckBox.setText("Unicode hints");
        hintUnicodeCheckBox.setToolTipText("Use Unicode/ASCII for programming logic hints");
        panel2.add(hintUnicodeCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hintMaxColumnLabel = new JLabel();
        hintMaxColumnLabel.setText("Max column for programming hints");
        hintMaxColumnLabel.setToolTipText("Max column for programming hints (>= 0)");
        panel2.add(hintMaxColumnLabel, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hintMaxColumnTextField = new JTextField();
        panel2.add(hintMaxColumnTextField, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        coverageCheckBox = new JCheckBox();
        coverageCheckBox.setSelected(true);
        coverageCheckBox.setText("Highlight verification coverage");
        panel2.add(coverageCheckBox, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        transitionCacheCheckBox = new JCheckBox();
        transitionCacheCheckBox.setHideActionText(false);
        transitionCacheCheckBox.setSelected(true);
        transitionCacheCheckBox.setText("Transition caching");
        transitionCacheCheckBox.setToolTipText("Cache various state transitions");
        panel2.add(transitionCacheCheckBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        coverageIntensitySpinner = new JSpinner();
        coverageIntensitySpinner.setToolTipText("Coverage background color transparency");
        panel2.add(coverageIntensitySpinner, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel2.add(spacer4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 5), new Dimension(-1, 5), new Dimension(-1, 5), 0, false));
        detailedInfoCheckBox = new JCheckBox();
        detailedInfoCheckBox.setSelected(true);
        detailedInfoCheckBox.setText("Detailed justification explanation");
        panel2.add(detailedInfoCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hintLinesFreshCheckBox = new JCheckBox();
        hintLinesFreshCheckBox.setText("Display At(...) line and fresh # hints");
        panel2.add(hintLinesFreshCheckBox, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkSatCheckBox = new JCheckBox();
        checkSatCheckBox.setText("Check assumption satisfiability");
        checkSatCheckBox.setToolTipText("Check satisfiability of facts and contracts in programming logic");
        panel2.add(checkSatCheckBox, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(154, 21), null, 0, false));
        patternExhaustiveCheckBox = new JCheckBox();
        patternExhaustiveCheckBox.setSelected(true);
        patternExhaustiveCheckBox.setText("Check pattern exhaustiveness");
        panel2.add(patternExhaustiveCheckBox, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        splitConditionalsCheckBox = new JCheckBox();
        splitConditionalsCheckBox.setText("Split conditionals");
        panel2.add(splitConditionalsCheckBox, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        splitMatchCasesCheckBox = new JCheckBox();
        splitMatchCasesCheckBox.setText("Split match cases");
        panel2.add(splitMatchCasesCheckBox, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        splitContractCasesCheckBox = new JCheckBox();
        splitContractCasesCheckBox.setText("Split contract cases");
        panel2.add(splitContractCasesCheckBox, new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spModeFlipRadioButton = new JRadioButton();
        spModeFlipRadioButton.setText("Flip");
        spModeFlipRadioButton.setToolTipText("Enable inter-procedural verification of strict-pure methods if on compositional verification, and<br> enable compositional verification of strict-pure methods if on inter-procededural verification");
        panel2.add(spModeFlipRadioButton, new GridConstraints(7, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spModeUninterpretedRadioButton = new JRadioButton();
        spModeUninterpretedRadioButton.setText("Unintepreted");
        spModeUninterpretedRadioButton.setToolTipText("Treat @strictpure methods as uninterpreted proof functions");
        panel2.add(spModeUninterpretedRadioButton, new GridConstraints(8, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        interpContractCheckBox = new JCheckBox();
        interpContractCheckBox.setText("Use method contracts in interprocedural verification");
        panel2.add(interpContractCheckBox, new GridConstraints(12, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loopBoundLabel = new JLabel();
        loopBoundLabel.setText("Loop Bound (Interprocedural)");
        loopBoundLabel.setToolTipText("Loop bound for interprocedural verification");
        panel2.add(loopBoundLabel, new GridConstraints(9, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loopBoundTextField = new JTextField();
        panel2.add(loopBoundTextField, new GridConstraints(9, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        callBoundLabel = new JLabel();
        callBoundLabel.setText("Recursive Call Bound (Interprocedural)");
        callBoundLabel.setToolTipText("Recursive call bound for interprocedural analysis");
        panel2.add(callBoundLabel, new GridConstraints(10, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        callBoundTextField = new JTextField();
        panel2.add(callBoundTextField, new GridConstraints(10, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        pureFunCheckBox = new JCheckBox();
        pureFunCheckBox.setText("Always add proof function for pure methods");
        panel2.add(pureFunCheckBox, new GridConstraints(11, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hintAtRewriteCheckBox = new JCheckBox();
        hintAtRewriteCheckBox.setSelected(true);
        hintAtRewriteCheckBox.setText("Rewrite At(...) as Input(...)/Old(...)");
        panel2.add(hintAtRewriteCheckBox, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spModeLabel = new JLabel();
        spModeLabel.setText("@strictpure method mode");
        panel2.add(spModeLabel, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spModeDefaultRadioButton = new JRadioButton();
        spModeDefaultRadioButton.setText("Default");
        spModeDefaultRadioButton.setToolTipText("Treat @strictpure methods accordig to the compositional/verification mode");
        panel2.add(spModeDefaultRadioButton, new GridConstraints(6, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rwMaxLabel = new JLabel();
        rwMaxLabel.setText("Maximum number of rewrites");
        panel2.add(rwMaxLabel, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rwMaxTextField = new JTextField();
        rwMaxTextField.setText("100");
        panel2.add(rwMaxTextField, new GridConstraints(5, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        rwTraceCheckBox = new JCheckBox();
        rwTraceCheckBox.setSelected(true);
        rwTraceCheckBox.setText("Rewrite tracing");
        panel2.add(rwTraceCheckBox, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rwEvalTraceCheckBox = new JCheckBox();
        rwEvalTraceCheckBox.setSelected(true);
        rwEvalTraceCheckBox.setText("Rewrite evaluation tracing");
        panel2.add(rwEvalTraceCheckBox, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        parPanel = new JPanel();
        parPanel.setLayout(new GridLayoutManager(3, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(parPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        parLabel = new JLabel();
        parLabel.setText("Parallelization");
        parPanel.add(parLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        branchParCoresSpinner = new JSpinner();
        parPanel.add(branchParCoresSpinner, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        branchParCoresLabel = new JLabel();
        branchParCoresLabel.setText("CPU cores (max: XXX)");
        parPanel.add(branchParCoresLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        parPanel.add(spacer5, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        modeLabel = new JLabel();
        modeLabel.setText("Script verification mode");
        parPanel.add(modeLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        modeAutoRadioButton = new JRadioButton();
        modeAutoRadioButton.setSelected(true);
        modeAutoRadioButton.setText("Auto");
        parPanel.add(modeAutoRadioButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        modeManualRadioButton = new JRadioButton();
        modeManualRadioButton.setText("Manual");
        parPanel.add(modeManualRadioButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        branchParAllRadioButton = new JRadioButton();
        branchParAllRadioButton.setSelected(true);
        branchParAllRadioButton.setText("All branches");
        parPanel.add(branchParAllRadioButton, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        branchParReturnsRadioButton = new JRadioButton();
        branchParReturnsRadioButton.setText("When all branches return");
        parPanel.add(branchParReturnsRadioButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        branchParDisabledRadioButton = new JRadioButton();
        branchParDisabledRadioButton.setText("Disabled");
        parPanel.add(branchParDisabledRadioButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        branchParLabel = new JLabel();
        branchParLabel.setText("Branch parallelization");
        parPanel.add(branchParLabel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rwParCheckBox = new JCheckBox();
        rwParCheckBox.setSelected(true);
        rwParCheckBox.setText("Rewriting parallelization");
        parPanel.add(rwParCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        additionalVerificationsPanel = new JPanel();
        additionalVerificationsPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(additionalVerificationsPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        infoFlowCheckBox = new JCheckBox();
        infoFlowCheckBox.setText("Information flow verification");
        infoFlowCheckBox.setToolTipText("Additionally verify information flow contracts");
        additionalVerificationsPanel.add(infoFlowCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        additionalVerificationsPanel.add(spacer6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("SMT2 Solvers", panel3);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        smt2CacheCheckBox = new JCheckBox();
        smt2CacheCheckBox.setSelected(true);
        smt2CacheCheckBox.setText("Query caching");
        smt2CacheCheckBox.setToolTipText("Cache SMT2 queries");
        panel4.add(smt2CacheCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        smt2SeqCheckBox = new JCheckBox();
        smt2SeqCheckBox.setText("Sequentialize solver calls");
        smt2SeqCheckBox.setToolTipText("Disable SMT2 solver call parallelization");
        panel4.add(smt2SeqCheckBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        smt2SimplifyCheckBox = new JCheckBox();
        smt2SimplifyCheckBox.setText("Simplify queries");
        panel4.add(smt2SimplifyCheckBox, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        inscribeSummoningsCheckBox = new JCheckBox();
        inscribeSummoningsCheckBox.setSelected(true);
        inscribeSummoningsCheckBox.setText("Inscribe summonings");
        inscribeSummoningsCheckBox.setToolTipText("Expose incantations used in summonings");
        panel4.add(inscribeSummoningsCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rawInscriptionCheckBox = new JCheckBox();
        rawInscriptionCheckBox.setText("Raw summoning inscription");
        panel4.add(rawInscriptionCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        elideEncodingCheckBox = new JCheckBox();
        elideEncodingCheckBox.setText("Elide inscription details");
        panel4.add(elideEncodingCheckBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        panel4.add(spacer7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 5), new Dimension(-1, 5), new Dimension(-1, 5), 0, false));
        final Spacer spacer8 = new Spacer();
        panel3.add(spacer8, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        smt2BitWidthPanel = new JPanel();
        smt2BitWidthPanel.setLayout(new GridLayoutManager(1, 7, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(smt2BitWidthPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        bitsLabel = new JLabel();
        bitsLabel.setEnabled(false);
        bitsLabel.setText("Default bit-width");
        smt2BitWidthPanel.add(bitsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer9 = new Spacer();
        smt2BitWidthPanel.add(spacer9, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        bitsUnboundedRadioButton = new JRadioButton();
        bitsUnboundedRadioButton.setEnabled(false);
        bitsUnboundedRadioButton.setSelected(true);
        bitsUnboundedRadioButton.setText("Unbounded");
        smt2BitWidthPanel.add(bitsUnboundedRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bits8RadioButton = new JRadioButton();
        bits8RadioButton.setEnabled(false);
        bits8RadioButton.setText("8");
        smt2BitWidthPanel.add(bits8RadioButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bits16RadioButton = new JRadioButton();
        bits16RadioButton.setEnabled(false);
        bits16RadioButton.setText("16");
        smt2BitWidthPanel.add(bits16RadioButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bits32RadioButton = new JRadioButton();
        bits32RadioButton.setEnabled(false);
        bits32RadioButton.setText("32");
        smt2BitWidthPanel.add(bits32RadioButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bits64RadioButton = new JRadioButton();
        bits64RadioButton.setEnabled(false);
        bits64RadioButton.setText("64");
        smt2BitWidthPanel.add(bits64RadioButton, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        smt2FPPanel = new JPanel();
        smt2FPPanel.setLayout(new GridLayoutManager(3, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(smt2FPPanel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        useRealCheckBox = new JCheckBox();
        useRealCheckBox.setText("Use real for FP");
        smt2FPPanel.add(useRealCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer10 = new Spacer();
        smt2FPPanel.add(spacer10, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, 1, null, null, null, 0, false));
        fpRoundingModeLabel = new JLabel();
        fpRoundingModeLabel.setText("FP Rounding Mode");
        smt2FPPanel.add(fpRoundingModeLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fpRNERadioButton = new JRadioButton();
        fpRNERadioButton.setSelected(true);
        fpRNERadioButton.setText("Nearest ties to even (RNE)");
        smt2FPPanel.add(fpRNERadioButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fpRTPRadioButton = new JRadioButton();
        fpRTPRadioButton.setText("Toward positive (RTP)");
        smt2FPPanel.add(fpRTPRadioButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fpRTZRadioButton = new JRadioButton();
        fpRTZRadioButton.setText("Toward zero (RTZ)");
        smt2FPPanel.add(fpRTZRadioButton, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer11 = new Spacer();
        smt2FPPanel.add(spacer11, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        fpRTNRadioButton = new JRadioButton();
        fpRTNRadioButton.setText("Toward negative (RTN)");
        smt2FPPanel.add(fpRTNRadioButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fpRNARadioButton = new JRadioButton();
        fpRNARadioButton.setText("Nearest ties to away (RNA)");
        smt2FPPanel.add(fpRNARadioButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        smt2ConfigPanel = new JPanel();
        smt2ConfigPanel.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(smt2ConfigPanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        smt2ValidConfigsLabel = new JLabel();
        smt2ValidConfigsLabel.setText("Validity configs");
        smt2ValidConfigsLabel.setToolTipText("Solvers: alt-ergo (if installed), cvc4, cvc5, z5");
        smt2ConfigPanel.add(smt2ValidConfigsLabel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_SOUTHEAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        smt2SatConfigsLabel = new JLabel();
        smt2SatConfigsLabel.setText("Satisfiability configs");
        smt2SatConfigsLabel.setToolTipText("Solvers: alt-ergo (if installed), cvc4, cvc5, z5");
        smt2ConfigPanel.add(smt2SatConfigsLabel, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        defaultSmt2SatConfigsLabel = new JLabel();
        defaultSmt2SatConfigsLabel.setIcon(new ImageIcon(getClass().getResource("/icon/sync.png")));
        defaultSmt2SatConfigsLabel.setText("");
        defaultSmt2SatConfigsLabel.setToolTipText("Restore default configuration");
        smt2ConfigPanel.add(defaultSmt2SatConfigsLabel, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_NORTHEAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        defaultSmt2ValidConfigsLabel = new JLabel();
        defaultSmt2ValidConfigsLabel.setIcon(new ImageIcon(getClass().getResource("/icon/sync.png")));
        defaultSmt2ValidConfigsLabel.setText("");
        defaultSmt2ValidConfigsLabel.setToolTipText("Restore default configuration");
        smt2ConfigPanel.add(defaultSmt2ValidConfigsLabel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_NORTHEAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        smt2SatConfigsTextArea = new JTextArea();
        smt2SatConfigsTextArea.setTabSize(2);
        smt2ConfigPanel.add(smt2SatConfigsTextArea, new GridConstraints(2, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        smt2ValidConfigsTextArea = new JTextArea();
        smt2ValidConfigsTextArea.setTabSize(2);
        smt2ConfigPanel.add(smt2ValidConfigsTextArea, new GridConstraints(0, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        rlimitLabel = new JLabel();
        rlimitLabel.setText("Resource limit");
        rlimitLabel.setToolTipText("Resource limit for SMT2 solvers (>=0)");
        panel5.add(rlimitLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rlimitTextField = new JTextField();
        rlimitTextField.setToolTipText("SMT2 Solver Resource Limit");
        panel5.add(rlimitTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        timeoutLabel = new JLabel();
        timeoutLabel.setText("Timeout (ms)");
        timeoutLabel.setToolTipText("Timeout (>= 200) for SMT2 solvers");
        panel5.add(timeoutLabel, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        timeoutTextField = new JTextField();
        panel5.add(timeoutTextField, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer12 = new Spacer();
        panel5.add(spacer12, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        satTimeoutCheckBox = new JCheckBox();
        satTimeoutCheckBox.setText("Use timeout for satisfiability checking");
        satTimeoutCheckBox.setToolTipText("Use timeout for satisfiability checking (500 ms otherwise)");
        panel5.add(satTimeoutCheckBox, new GridConstraints(1, 3, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        searchPcCheckBox = new JCheckBox();
        searchPcCheckBox.setText("Search path conditions first before employing SMT2 solvers");
        panel5.add(searchPcCheckBox, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer13 = new Spacer();
        logikaPanel.add(spacer13, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer14 = new Spacer();
        logikaPanel.add(spacer14, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 5), new Dimension(-1, 5), new Dimension(-1, 5), 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(fpRTZRadioButton);
        buttonGroup.add(fpRNERadioButton);
        buttonGroup.add(fpRNARadioButton);
        buttonGroup.add(fpRTPRadioButton);
        buttonGroup.add(fpRTNRadioButton);
        buttonGroup = new ButtonGroup();
        buttonGroup.add(branchParDisabledRadioButton);
        buttonGroup.add(branchParReturnsRadioButton);
        buttonGroup.add(branchParAllRadioButton);
        buttonGroup = new ButtonGroup();
        buttonGroup.add(bitsUnboundedRadioButton);
        buttonGroup.add(bits8RadioButton);
        buttonGroup.add(bits16RadioButton);
        buttonGroup.add(bits32RadioButton);
        buttonGroup.add(bits64RadioButton);
        buttonGroup = new ButtonGroup();
        buttonGroup.add(spModeDefaultRadioButton);
        buttonGroup.add(spModeUninterpretedRadioButton);
        buttonGroup.add(spModeFlipRadioButton);
        buttonGroup = new ButtonGroup();
        buttonGroup.add(modeAutoRadioButton);
        buttonGroup.add(modeManualRadioButton);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return logikaPanel;
    }

}
