// Copyright 2021 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;

import net.basdon.anna.api.Message;

import static net.basdon.anna.api.Constants.*;
import static net.basdon.anna.api.Util.*;
import static javax.swing.SpringLayout.*;

public
class InteractiveTest extends JFrame implements
	Consumer<String>,
	ActionListener
{
static final Font GLOBALFONT = new Font("Courier New", Font.PLAIN, 12);

private static final String SERVERNAME = "interactive-test-server";

private static InteractiveTest instance;

private DebugSocket debugSocket;
private String annaNickname = "dummy";
private String annaName = "dummy";
private String annaUser = "dummy!dummy@" + SERVERNAME;

private JScrollPane scrollPane;
private DefaultListModel<RichMessage> listModel;
private JTextField txtPrivmsgSender;
private JTextField txtPrivmsgTarget;
private JTextField txtPrivmsgValue;
private JTextField txtActionSender;
private JTextField txtActionTarget;
private JTextField txtActionValue;
private JTextField txtRaw;

public static
void main(String[] args)
{
	try {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		SwingUtilities.invokeAndWait(InteractiveTest::new);
		instance.debugSocket = new DebugSocket(instance);
	} catch (Throwable e) {
		e.printStackTrace();
		instance.close();
		return;
	}

	Main.debugSocket = instance.debugSocket;
	Main.main(args);
}

private
InteractiveTest()
{
	instance = this;

	this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	this.setTitle("Anna^ interactive test");
	this.setMinimumSize(new Dimension(300, 200));
	this.setPreferredSize(new Dimension(900, 600));
	this.pack();
	this.setLocationRelativeTo(null);
	this.setVisible(true);

	Container root;
	JList<RichMessage> list;
	JTextField txtPrivmsgSender, txtPrivmsgTarget, txtPrivmsgValue;
	JTextField txtActionSender, txtActionTarget, txtActionValue;
	JTextField txtRaw;
	SpringLayout layout;
	JScrollPane scrollPane;

	root = this.getRootPane().getContentPane();
	root.setLayout(layout = new SpringLayout());

	list = new JList<>(this.listModel = new DefaultListModel<>());
	list.setCellRenderer(new RichMessageCellRenderer());
	list.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
	list.setBackground(new Color(0xD2D2D2));

	scrollPane = this.scrollPane = new JScrollPane(list);

	txtPrivmsgSender = this.txtPrivmsgSender = new JTextField("robin_be!robin@cyber.space");
	txtPrivmsgSender.addActionListener(this);
	txtPrivmsgSender.setFont(GLOBALFONT);

	txtPrivmsgTarget = this.txtPrivmsgTarget = new JTextField("#basdon.echo");
	txtPrivmsgTarget.addActionListener(this);
	txtPrivmsgTarget.setFont(GLOBALFONT);

	txtPrivmsgValue = this.txtPrivmsgValue = new JTextField("text");
	txtPrivmsgValue.addActionListener(this);
	txtPrivmsgValue.setFont(GLOBALFONT);

	txtActionSender = this.txtActionSender = new JTextField("robin_be!robin@cyber.space");
	txtActionSender.addActionListener(this);
	txtActionSender.setFont(GLOBALFONT);

	txtActionTarget = this.txtActionTarget = new JTextField("#basdon.echo");
	txtActionTarget.addActionListener(this);
	txtActionTarget.setFont(GLOBALFONT);

	txtActionValue = this.txtActionValue = new JTextField("text");
	txtActionValue.addActionListener(this);
	txtActionValue.setFont(GLOBALFONT);

	txtRaw = this.txtRaw = new JTextField("RAW");
	txtRaw.addActionListener(this);
	txtRaw.setFont(GLOBALFONT);

	root.add(scrollPane);
	root.add(txtPrivmsgSender);
	root.add(txtPrivmsgTarget);
	root.add(txtPrivmsgValue);
	root.add(txtActionSender);
	root.add(txtActionTarget);
	root.add(txtActionValue);
	root.add(txtRaw);

	this.setDimensions(scrollPane, 300, 300, -2, -2, Short.MAX_VALUE, Short.MAX_VALUE);
	this.setDimensions(txtPrivmsgSender, 200, -2, -2, -1, 300, -1);
	this.setDimensions(txtPrivmsgTarget, 200, -2, -2, -1, Short.MAX_VALUE, -1);
	this.setDimensions(txtPrivmsgValue, 200, -2, -2, -1, Short.MAX_VALUE, -1);
	this.setDimensions(txtActionSender, 200, -2, -2, -1, 300, -1);
	this.setDimensions(txtActionTarget, 200, -2, -2, -1, Short.MAX_VALUE, -1);
	this.setDimensions(txtActionValue, 200, -2, -2, -1, Short.MAX_VALUE, -1);
	this.setDimensions(txtRaw, -2, -2, -2, -1, Short.MAX_VALUE, -1);

	layout.putConstraint(NORTH, scrollPane, 12, NORTH, root);
	layout.putConstraint(EAST, scrollPane, -12, EAST, root);
	layout.putConstraint(WEST, scrollPane, 12, WEST, root);
	layout.putConstraint(SOUTH, scrollPane, -12, NORTH, txtPrivmsgSender);
	layout.putConstraint(WEST, txtPrivmsgSender, 0, WEST, scrollPane);
	layout.putConstraint(WEST, txtPrivmsgTarget, 12, EAST, txtPrivmsgSender);
	layout.putConstraint(WEST, txtPrivmsgValue, 12, EAST, txtPrivmsgTarget);
	layout.putConstraint(EAST, txtPrivmsgValue, 0, EAST, scrollPane);
	layout.putConstraint(NORTH, txtPrivmsgTarget, 0, NORTH, txtPrivmsgSender);
	layout.putConstraint(NORTH, txtPrivmsgValue, 0, NORTH, txtPrivmsgSender);
	layout.putConstraint(SOUTH, txtPrivmsgTarget, 0, SOUTH, txtPrivmsgSender);
	layout.putConstraint(SOUTH, txtPrivmsgValue, 0, SOUTH, txtPrivmsgSender);
	layout.putConstraint(SOUTH, txtPrivmsgSender, -12, NORTH, txtActionSender);
	layout.putConstraint(WEST, txtActionSender, 0, WEST, scrollPane);
	layout.putConstraint(WEST, txtActionTarget, 12, EAST, txtActionSender);
	layout.putConstraint(WEST, txtActionValue, 12, EAST, txtActionTarget);
	layout.putConstraint(EAST, txtActionValue, 0, EAST, scrollPane);
	layout.putConstraint(NORTH, txtActionTarget, 0, NORTH, txtActionSender);
	layout.putConstraint(NORTH, txtActionValue, 0, NORTH, txtActionSender);
	layout.putConstraint(SOUTH, txtActionTarget, 0, SOUTH, txtActionSender);
	layout.putConstraint(SOUTH, txtActionValue, 0, SOUTH, txtActionSender);
	layout.putConstraint(SOUTH, txtActionSender, -12, NORTH, txtRaw);
	layout.putConstraint(WEST, txtRaw, 0, WEST, txtPrivmsgSender);
	layout.putConstraint(EAST, txtRaw, 0, EAST, txtPrivmsgValue);
	layout.putConstraint(SOUTH, txtRaw, -12, SOUTH, root);
}

@Override
public
void actionPerformed(ActionEvent e)
{
	Object source = e.getSource();
	if (source == this.txtPrivmsgSender ||
		source == this.txtPrivmsgTarget ||
		source == this.txtPrivmsgValue)
	{
		String sender = this.txtPrivmsgSender.getText();
		String target = this.txtPrivmsgTarget.getText();
		String value = this.txtPrivmsgValue.getText();
		this.sendMessage(sender, "PRIVMSG", target, ':' + value);
		this.txtPrivmsgValue.setText("");
	} else if (source == this.txtActionSender ||
		source == this.txtActionTarget ||
		source == this.txtActionValue)
	{
		String sender = this.txtActionSender.getText();
		String target = this.txtActionTarget.getText();
		String value = this.txtActionValue.getText();
		this.sendMessage(sender, "PRIVMSG", target, ":\1ACTION " + value + "\1");
		this.txtActionValue.setText("");
	} else if (source == this.txtRaw) {
		this.send(this.txtRaw.getText());
		this.txtRaw.setText("");
	}
}

@Override
public
void accept(String line)
{
	// Line coming from Anna.
	SwingUtilities.invokeLater(() -> this.handleAnnaLine(line));
}

private
void setDimensions(Component component, int minw, int minh, int prefw, int prefh, int maxw, int maxh)
{
	Dimension pref = component.getPreferredSize();
	if (minw != -2 && minh != -2) {
		if (minw == -1) minw = pref.width;
		if (minh == -1) minh = pref.height;
		component.setMinimumSize(new Dimension(minw, minh));
	}
	if (prefw != -2 && prefh != -2) {
		if (prefw == -1) prefw = pref.width;
		if (prefh == -1) prefh = pref.height;
		component.setPreferredSize(new Dimension(prefw, prefh));
	}
	if (maxw != -2 && maxh != -2) {
		if (maxw == -1) maxw = pref.width;
		if (maxh == -1) maxh = pref.height;
		component.setMaximumSize(new Dimension(maxw, maxh));
	}
}

private
void close()
{
	this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
}

private
void sendMessage(String sender, int num, String...params)
{
	this.sendMessage(sender, String.valueOf(num), params);
}

private
void sendMessage(String sender, String command, String...params)
{
	StringBuilder sb = new StringBuilder(512);
	if (sender != null) {
		sb.append(":" + sender + " ");
	}
	sb.append(command);
	for (String param : params) {
		sb.append(' ').append(param);
	}
	this.send(sb.toString());
}

private
void send(String message)
{
	this.displayMessage("-> " + message);
	this.debugSocket.is.accept(message + "\r\n");
}

private
void handleAnnaLine(String line)
{
	this.displayMessage("<- " + line);

	char[] chars = line.toCharArray();
	Message message = Message.parse(chars, chars.length);

	if (strcmp(message.cmd, 'N','I','C','K')) {
		this.annaNickname = new String(message.paramv[0]);
		this.annaUser = annaNickname + '!' + this.annaName + '@' + SERVERNAME;
	} else if (strcmp(message.cmd, 'U','S','E','R')) {
		this.annaName = new String(message.paramv[0]);
		this.annaUser = annaNickname + '!' + this.annaName + '@' + SERVERNAME;
		this.sendMessage(null, RPL_ENDOFMOTD, ":end of motd");
	} else if (strcmp(message.cmd, 'J','O','I','N')) {
		String channel = new String(message.paramv[0]);
		this.sendMessage(this.annaUser, "JOIN", ":" + channel);
		String names = ":" + this.annaNickname + " fakeUser1 @fakeUser2";
		this.sendMessage(null, RPL_NAMREPLY, this.annaNickname, "=", channel, names);
		this.sendMessage(null, RPL_ENDOFNAMES, this.annaNickname, "=", channel, ":End of /NAMES list.");
	}
}

private
void displayMessage(String message)
{
	JScrollBar sb = this.scrollPane.getVerticalScrollBar();
	boolean wasAtEnd = sb.getValue() == sb.getMaximum() - sb.getModel().getExtent();
	this.listModel.addElement(RichMessage.parse(message.toCharArray()));
	if (wasAtEnd) {
		SwingUtilities.invokeLater(() -> sb.setValue(sb.getMaximum()));
	}
}
} /*InteractiveTest*/
