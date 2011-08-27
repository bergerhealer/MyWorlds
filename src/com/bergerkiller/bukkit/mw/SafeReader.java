package com.bergerkiller.bukkit.mw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;

public class SafeReader {
	private String filename;
	private BufferedReader r = null;
	
	public SafeReader(String filename) {
		this.filename = filename;
		try {
			 r = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			MyWorlds.log(Level.INFO, "File not found, it is not loaded: " + this.filename);
		} catch (Exception ex) {
			MyWorlds.log(Level.SEVERE, "Error while loading file: " + this.filename);
			ex.printStackTrace();
		}
	}
	
	public boolean exists() {
		return new File(this.filename).exists();
	}
	
	public String readNonEmptyLine() {
		String line = readLine();
		if (line == null) return null;
		if (line.equals("")) return null;
		return line;
	}
	public String readLine() {
		if (r == null) return null;
		try {
			return r.readLine();
		} catch (Exception ex) {
			MyWorlds.log(Level.SEVERE, "Error while reading: " + this.filename);
			ex.printStackTrace();
			return null;
		}
	}	
	public void close() {
		if (this.r == null) return;
		try {
			this.r.close();
		} catch (Exception ex) {
			MyWorlds.log(Level.SEVERE, "Error while closing stream: " + this.filename);
			ex.printStackTrace();
		}
	}

}
