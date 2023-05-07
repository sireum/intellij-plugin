// From https://stackoverflow.com/a/27845347
package org.sireum.intellij;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * @author xehpuk
 */
public class PlaceholderTextField extends JTextField {
    private static final long serialVersionUID = -5529071085971698388L;

    /**
     * The placeholder to be displayed if the text field is empty.
     */
    private String placeholder;
    /**
     * Determines whether the placeholder should be displayed even on focus.
     */
    private boolean paintingOnFocus;
    /**
     * The color the placeholder should be displayed in.
     */
    private Color placeholderColor;

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(final String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    public boolean isPaintingOnFocus() {
        return paintingOnFocus;
    }

    public void setPaintingOnFocus(final boolean paintingOnFocus) {
        this.paintingOnFocus = paintingOnFocus;
        repaint();
    }

    public Color getPlaceholderColor() {
        return placeholderColor;
    }

    public void setPlaceholderColor(final Color placeholderColor) {
        this.placeholderColor = placeholderColor;
        repaint();
    }

    public PlaceholderTextField() {
        super();
    }

    public PlaceholderTextField(final Document doc, final String text, final int columns) {
        super(doc, text, columns);
    }

    public PlaceholderTextField(final int columns) {
        super(columns);
    }

    public PlaceholderTextField(final String text, final int columns) {
        super(text, columns);
    }

    public PlaceholderTextField(final String text) {
        super(text);
    }

    {
        addFocusListener(new RepaintFocusListener());
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (getPlaceholder() != null && getText().isEmpty() && (isPaintingOnFocus() || !isFocusOwner())) {
            try {
                final Rectangle rect = getUI().modelToView(this, 0);
                final Insets insets = getInsets();
                g.setFont(getFont());
                g.setColor(getPlaceholderColor() == null ? getForeground() : getPlaceholderColor());
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.drawString(getPlaceholder(), rect.x, getHeight() - insets.top - insets.bottom - rect.y);
            } catch (final BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class RepaintFocusListener implements FocusListener {
        @Override
        public void focusGained(final FocusEvent e) {
            repaint();
        }

        @Override
        public void focusLost(final FocusEvent e) {
            repaint();
        }
    }
}

