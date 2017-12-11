package com.ck.dict.model;

import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.crypto.dsig.keyinfo.KeyInfo;

public class Dictionary {
	private ArrayList<String> oriKeys;
	private ArrayList<String> first_last_keys;
	
	private HashMap<String, Long> offsets; 
	private HashMap<Long,CompressedRecord> records;
	public Dictionary(ArrayList<String> oriKeys, ArrayList<String> first_last_keys, HashMap<String, Long> offsetMap,
			HashMap<Long, CompressedRecord> records) {
		super();
		this.oriKeys = oriKeys;
		this.first_last_keys = first_last_keys;
		this.offsets = offsetMap;
		this.records = records;
	}
	public ArrayList<String> getOriKeys() {
		return oriKeys;
	}
	public void setOriKeys(ArrayList<String> oriKeys) {
		this.oriKeys = oriKeys;
	}
	public ArrayList<String> getFirst_last_keys() {
		return first_last_keys;
	}
	public void setFirst_last_keys(ArrayList<String> first_last_keys) {
		this.first_last_keys = first_last_keys;
	}
	public HashMap<String, Long> getOffsets() {
		return offsets;
	}
	public void setOffsets(HashMap<String, Long> offsets) {
		this.offsets = offsets;
	}
	public HashMap<Long, CompressedRecord> getRecords() {
		return records;
	}
	public void setRecords(HashMap<Long, CompressedRecord> records) {
		this.records = records;
	} 
	
	
}

