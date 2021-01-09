// Copyright 2021 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public
class RichMessageCellRenderer implements ListCellRenderer<RichMessage>
{
@Override
public
Component getListCellRendererComponent(
	JList<? extends RichMessage> list,
	RichMessage value,
	int index,
	boolean isSelected,
	boolean cellHasFocus)
{
	JLabel lbl = new JLabel();
	lbl.setFont(InteractiveTest.GLOBALFONT);
	if (isSelected) {
		lbl.setText(value.plain);
		lbl.setForeground(Color.WHITE);
		lbl.setBackground(Color.BLACK);
		lbl.setOpaque(true);
	} else {
		lbl.setText(value.rich);
		lbl.setOpaque(false);
	}
	return lbl;
}
}
