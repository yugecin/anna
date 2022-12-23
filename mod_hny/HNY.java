package mod_hny;

import java.util.List;

import nuwani.core.Core;
import nuwani.core.Raws;

public class HNY {
	
	private long lastupdate;
	
	private final int NEWYEARS = 40;
	private NY[] newyears;
	
	private int next;
	
	private Core core;
	private List<String> channels;
	private String year;
	private int gmtepoch;
	
	public HNY(Core core) {
		this.core = core;
		
		lastupdate = Math.round(System.currentTimeMillis()/1000L);
	}
	
	public void setChannels(List<String> channels) {
		this.channels = channels;
	}
	
	public void setGMTEpoch(int gmtepoch) {
		this.gmtepoch = gmtepoch;
	}
	
	public void setYear(String year) {
		this.year = year;
	}
	
	public void fillNewyears() {
		next = 0;
		
		int i = 0;
		
		newyears = new NY[NEWYEARS];
		
		newyears[i++] = new NY(-840,	"LINT",		"Samoa and Christmas Island/Kiribati");
		newyears[i++] = new NY(-825,	"CHADT",	"Chatham Islands/New Zealand");
		newyears[i++] = new NY(-780,	"NZDT",		"New Zealand, McMurdo Station, Amundsen-Scott station (South Pole), Fiji, Tonga");
		newyears[i++] = new NY(-720,	"ANAT",		"parts of Russia, Tuvalu, Nauru, Kribati, Marshall Islands");
		newyears[i++] = new NY(-660,	"AEDT",		"parts of Australia, Tasmania, Victoria, Macquarie Island (Australia), Solomon Islands");
		newyears[i++] = new NY(-630,	"ACDT",		"south Australia");
		newyears[i++] = new NY(-600,	"AEST",		"Queensland/Australia, Papua New Guinea, Guam");
		newyears[i++] = new NY(-570,	"ACST",		"Northern Territory/Australia");
		newyears[i++] = new NY(-540,	"JST",		"Japan, South Korea, Timor-Leste, Palau, Yakutsk/Russia");
		newyears[i++] = new NY(-525,	"ACWST",	"Eucla/Western Australia");
		newyears[i++] = new NY(-510,	"PYT",		"North Korea");
		newyears[i++] = new NY(-480,	"CST",		"China, Hong Kong, Philippines, Singapore and others");
		newyears[i++] = new NY(-420,	"WIB",		"parts of Indonesia, Thailand, Vietnam, Cambodia and others");
		newyears[i++] = new NY(-390,	"MMT",		"Myanmar and Cocos Islands");
		newyears[i++] = new NY(-360,	"BST",		"Bangladesh, Kazakhstan, Kyrgyzstan, Bhutan, and others");
		newyears[i++] = new NY(-345,	"NPT",		"Nepal");
		newyears[i++] = new NY(-330,	"IST",		"India and Sri Lanka");
		newyears[i++] = new NY(-300,	"UZT",		"Pakistan, Uzbekistan and others");
		newyears[i++] = new NY(-270,	"AFT",		"Afghanistan");
		newyears[i++] = new NY(-240,	"GST",		"Azerbaijan, Armenia, United Arab Emirates, Oman, Seychelles, Mauritius and others");
		newyears[i++] = new NY(-210,	"IRST",		"Iran");
		newyears[i++] = new NY(-180,	"MSK",		"Belarus, Ethiopia, France, Iraq, Kenya, Kuwait, Madagascar, Russia, Somalia, South Africa, Sudan, Tanzania, Uganda, Yemen, and others");
		newyears[i++] = new NY(-120,	"EET",		"Bulgaria, Egypt, Estonia, Finland, Greece, Israel, Lithuania, Libya, Moldova, Romania, Rwanda, South Africa, Syria, Turkey, Ukraine, UK, Zimbabwe, and many others");
		newyears[i++] = new NY(-60,	"CET",		"Albania, Algeria, Austria, Belgium, Croatia, Czech Republic, Denmark, France, Germany, Hungary, Italy, Luxembourg, Malta, Monaco, Netherlands, Nigeria, Norway, Poland, Spain, Sweden, Switzerland, UK, and many others");
		newyears[i++] = new NY(0,	"GMT",		"Denmark, Gambia, Ghana, Guinea, Iceland, Ireland, Liberia, Mali, Mauritania, Morocco, Portugal, Spain, Senegal, Togo, UK, and others");
		newyears[i++] = new NY(60,	"CVT",		"Cape Verde, Denmark, Portugal");
		newyears[i++] = new NY(120,	"BRST",		"parts of Brazil, South Georgia/Sandwich Island");
		newyears[i++] = new NY(180,	"ART",		"Argentina, Brazil, Denmark, France, Suriname, UK, Uruguay");
		newyears[i++] = new NY(210,	"NST",		"Newfoundland and Labrador/Canada");
		newyears[i++] = new NY(240,	"BOT",		"Bolivia, parts of Canada, Caribbean countries/territories and others");
		newyears[i++] = new NY(270,	"VET",		"Venezuela");
		newyears[i++] = new NY(300,	"EST",		"Bahamas, Brazil, Canada, Colombia, Cuba, Ecuador, Haiti, Jamaica, Panama, Peru, UK, US");
		newyears[i++] = new NY(360,	"CST",		"Belize, Canada, Chile, Costa Rica, Ecuador, El Salvador, Guatemala, Honduras, Mexico, Nicaragua, US");
		newyears[i++] = new NY(420,	"MST",		"Canada, Mexico, US");
		newyears[i++] = new NY(480,	"PST",		"Canada, Clipperton Island, Pitcairn Islands, Mexico, US [CA, ID, NV, OR, WA]");
		newyears[i++] = new NY(540,	"AKST",		"French Polynesia, Gambier Islands, Alaska");
		newyears[i++] = new NY(570,	"MART",		"French Polynesia, Marquesas Islands");
		newyears[i++] = new NY(600,	"HAST",		"French Polynesia, Cook Islands, Alaska, Hawaii");
		newyears[i++] = new NY(660,	"NUT",		"American Samoa, Hawaii, Niue");
		newyears[i++] = new NY(720,	"AoE",		"Baker Island, Howland Island");
		

		for(i = 0; i < NEWYEARS; i++)
		{
			if(lastupdate < newyears[i].epoch)
			{
				next = i;
				return; // important
			}
		}
		next = newyears.length;
		
	}
	
	public void update() {
		lastupdate = System.currentTimeMillis()/1000L;
		if(next >= newyears.length) return;
		NY h;
		h = newyears[next];
		if(lastupdate > h.epoch)
		{
			send("Happy New Year to [" + formatGMT(h.gmt) + "] (" + h.timezone + ") " + h.countries);
			next++;
			send(getNext());
		}
	}
	
	/*
	public void update() {
		long time = System.currentTimeMillis()/1000L;
		NY h;
		for(int i = 0; i < NEWYEARS; i++)
		{
			h = newyears[i];
			if(lastupdate < h.epoch)
			{
				if(h.epoch < time) {
					send("Happy New Year to [" + formatGMT(h.gmt) + "] (" + h.timezone + ") " + h.countries);
					next = i + 1;
					send(getNext());
				} else {
					lastupdate = time;
					return;
				}
			}
		}
		lastupdate = time;
	}
	*/
	
	private String getNext() {
		if(next >= NEWYEARS) {
			return "That's all folks! Everyone should be in " + year + " now :> Happy New Year, everyone!"; // That was the last new year for this year! Happy New Year, everyone!
		} else {
			NY n = newyears[next];
			return "Next New Year is in " + countdownTime(n.epoch) + " at timezone " + n.timezone + " [" + formatGMT(n.gmt) + "]";
		}
	}
	
	public void sendNext(String sendto) {
		core.send(Raws.sendMessage(sendto, getNext()));
	}
	
	public void sendStatus(String sendto) {
		int done = 0;
		for(int i = 0; i < NEWYEARS; i++) {
			if(newyears[i].epoch < lastupdate) {
				done++;
			}
		}
		if(done == NEWYEARS) {
			core.send(Raws.sendMessage(sendto, "All of the timezones in my list are in " + year + "! :> Happy New Year, everyone!"));
		} else {
			core.send(Raws.sendMessage(sendto, done + " of " + NEWYEARS + " timezones in my list are already in " + year + "! Next one is " + newyears[next].timezone + " in " + countdownTime(newyears[next].epoch)));
		}
	}

	public void sendTimezone(String sendto, String params) {
		if(params.matches("^[gG][mM][tT][\\+-][0-9]+(:[0-9]+)?$")){
			sendTimezoneGMT(sendto, params);
			return;
		}
		params = params.toUpperCase();
		for(int i = 0; i < NEWYEARS; i++) {
			if(newyears[i].timezone.toUpperCase().equals(params)) {
				sendTimezone(sendto, newyears[i]);
				return;
			}
		}
		core.send(Raws.sendMessage(sendto, "What the hell is that timezone?"));
	}
	
	private void sendTimezoneGMT(String sendto, String params) {
		int number = 0;
		if(params.matches("^[gG][mM][tT][\\+-][0-9]+$")){
			number = Integer.parseInt(params.replaceAll("^[gG][mM][tT]([\\+-][0-9]+)$", "$1")) * 60;
		} else if(params.matches("^[gG][mM][tT][\\+-][0-9]+:[0-9]+$")){
			number = Integer.parseInt(params.replaceAll("^[gG][mM][tT]([\\+-][0-9]+):[0-9]+$", "$1")) * 60;
			number += Integer.parseInt(params.replaceAll("^[gG][mM][tT]([\\+-])[0-9]+:([0-9]+)$", "$1$2"));
		}
		for(int i = 0; i < NEWYEARS; i++) {
			if(newyears[i].gmt == -number) {
				sendTimezone(sendto, newyears[i]);
				return;
			}
		}
		core.send(Raws.sendMessage(sendto, "Sorry, I don't recognize that timezone"));
	}
	
	private void sendTimezone(String sendto, NY ny) {
		if(lastupdate < ny.epoch) {
			core.send(Raws.sendMessage(sendto, year + " will reach " + ny.timezone + " [" + formatGMT(ny.gmt) + "] (" + ny.countries + ") in " + countdownTime(ny.epoch)));
		} else {
			core.send(Raws.sendMessage(sendto, ny.timezone + " [" + formatGMT(ny.gmt) + "] (" + ny.countries + ") is already in " + year + " for " + countdownTime(ny.epoch)));
		}
	}
	
	private void send(String message) {
		for(String channel : channels) {
			core.send(Raws.sendMessage(channel, message));
		}
	}
	
	private String countdownTime(long epoch) {
		int t = (int)(epoch - (System.currentTimeMillis() / 1000L));
		if(t < 0) t = -t;
		int s = t % 60;
		int m = ((t - s) / 60) % 60;
		int h = (t - m * 60 - s) / 3600;
		StringBuilder b = new StringBuilder();
		if(h > 0) {
			b.append(h).append(" hour");
			if(h > 1) b.append("s");
			if(m > 0) b.append(" ");
		}
		if(m > 0) {
			b.append(m).append(" minute");
			if(m > 1) b.append("s");
		}
		return new String(b);
	}
	
	private String formatGMT(int gmt) {
		gmt = -gmt;
		StringBuilder b = new StringBuilder();
		b.append("GMT");
		if(gmt < 0) {
			b.append("-");
			gmt = -gmt;
		} else {
			b.append("+");
		}
		int m = (gmt % 60);
		int h = ((gmt - m) / 60) % 60;
		if(h < 10) b.append("0");
		b.append(h);
		b.append(":");
		if(m < 10) b.append("0");
		b.append(m);
		return new String(b);
	}
	
	protected class NY {

		public long epoch;
		public int gmt;
		public String timezone;
		public String countries;
		
		protected NY(long epoch, String timezone, String countries)
		{
			this.epoch = epoch;
			this.countries = countries;
			this.timezone = timezone;
			this.gmt = (int)epoch / 1000; // dirty, idc
		}
		
		protected NY(int epoch, String timezone, String countries)
		{
			this(gmtepoch + (long)(epoch * 60), timezone, countries); // 2016 // http://www.onlineconversion.com/unix_time.htm (GMT) epochconverter.com
			this.gmt = epoch;
		}
		
	}

}
