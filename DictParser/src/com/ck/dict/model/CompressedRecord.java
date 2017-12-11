package com.ck.dict.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.ck.dict.util.Utils;

public class CompressedRecord {
	private byte[] compressedData;
	private String encoding;
	public String getString(long position){
		String result=null;
		byte[] bytes=getRecordData(position);
		try {
			result=new String(bytes, encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}
	public InputStream getFile(long position){
		return new ByteArrayInputStream(getRecordData(position));
	}
	private byte[] getRecordData(long position){
		InputStream record_ds=Utils.decompress(compressedData, compressedData.length);
		byte[] result=null;
		try {
			record_ds.skip(position);
			ByteArrayOutputStream baos=new ByteArrayOutputStream();
			int read=0;
			while ((read = record_ds.read()) > 0) {
				baos.write(read);
			}
			result=baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return result;
	}
	public CompressedRecord(byte[] compressedData,String encoding) {
		super();
		this.compressedData = compressedData;
		this.encoding = encoding;
	}
	public byte[] getCompressedData() {
		return compressedData;
	}
	public void setCompressedData(byte[] compressedData) {
		this.compressedData = compressedData;
	}
	
}
