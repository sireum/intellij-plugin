package org.sireum.intellij;

import javax.swing.*;
import java.awt.*;

public class SlangAstToolWindowForm {
    protected JTree slangAstTree;
    protected JPanel slangAstPanel;
    protected JScrollPane slangAstScrollPane;

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
        slangAstPanel = new JPanel();
        slangAstPanel.setLayout(new BorderLayout(0, 0));
        slangAstScrollPane = new JScrollPane();
        slangAstPanel.add(slangAstScrollPane, BorderLayout.CENTER);
        slangAstTree = new JTree();
        slangAstScrollPane.setViewportView(slangAstTree);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return slangAstPanel;
    }

}
