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

package org.sireum.intellij;

import javax.swing.*;

public abstract class SireumForm {
    protected JPanel sireumPanel;
    protected JLabel sireumHomeLabel;
    protected JTextField sireumHomeTextField;
    protected JButton sireumHomeButton;
    protected JLabel vmArgsLabel;
    protected JTextField vmArgsTextField;
    protected JLabel envVarsLabel;
    protected JTextArea envVarsTextArea;
    protected JScrollPane envVarsScrollPane;
    protected JPanel headerPanel;
    protected JLabel titleLabel;
    protected JLabel subtitleLabel;
    protected JPanel logoPanel;
    protected JLabel logoLabel;
    protected JCheckBox backgroundCheckBox;
    protected JPanel idlePanel;
    protected JLabel idleLabel;
    protected JTextField idleTextField;
    protected JLabel parLabel;
    protected JSpinner parSpinner;
}
