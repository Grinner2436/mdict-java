package com.ck.dict.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.ck.dict.model.CompressedRecord;
import com.ck.dict.model.Dictionary;


public class DictionaryQuerier {
	private static HashMap<String, Dictionary> dicts=new HashMap<String, Dictionary>();
	static{
		String filePath="D:/MDictPC/doc/牛津高阶8简体.mdx";
		try {
			FileInputStream fins=new FileInputStream(filePath);
			MdxFileParser parser=new MdxFileParser();
			Dictionary dict=parser.parse(fins);
			dicts.put("牛津高阶8简体", dict);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	public static String query(String query) {
		return query("牛津高阶8简体",query);
	}
	public static String query(String dictName,String query) {
		String result;
		Dictionary dict=dicts.get(dictName);
		if(dict==null) {
			result="词典不存在！";
		}else {
			List<String> keys=dict.getOriKeys();
			HashMap<Long, CompressedRecord> recordsMap=dict.getRecords();
			
			//定位到词条或者最相近的词条
			int start=0,end=keys.size(),mid;
			while(end-start>1) {
				mid=(start+end)/2;
				if(end>start) {
					String midWord=keys.get(mid);
					int flag=query.compareTo(midWord);
					if(flag>0) {
						start=mid;
					}else if(flag<0) {
						end=mid;
					}else {
						start=mid;
						break;
					}
				}else{
					break;
				}
			}
			
			//确定要显示的词，拿到偏移量
			String item=keys.get(start);
			Long wordOffset=dict.getOffsets().get(item);
			
			//根据偏移量定位到块
			long pre=0;
			Set<Long> offSets=dict.getRecords().keySet();
			for(Long offSet:offSets) {
				if(wordOffset<offSet) {
					break;
				}else if(wordOffset>=offSet) {
					pre=offSet;
					continue;
				}
			}
			
			//拿出记录块，从里面解压出对应的词条
			long position=wordOffset-pre;
			CompressedRecord record=recordsMap.get(pre);
			result=record.getString(position);
		}
		return result;
	}
	public static void main(String[] args) {
		String record=DictionaryQuerier.query("apple");
		System.out.println(record);
	}
}
