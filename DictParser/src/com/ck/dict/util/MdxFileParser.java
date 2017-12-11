package com.ck.dict.util;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ck.dict.model.CompressedRecord;
import com.ck.dict.model.Dictionary;


public class MdxFileParser {
	private ArrayList<String> keyNameList=new ArrayList<String>();
	private ArrayList<String> first_last_keys=new ArrayList<String>();
	
	private HashMap<String,Long> offsetMap=new HashMap<String,Long>();
	private LinkedHashMap<Long,CompressedRecord> records=new LinkedHashMap<Long,CompressedRecord>(); 
	
	private ArrayList<CompressedRecord> compressedRecords=new ArrayList<CompressedRecord>();
	public Dictionary parse(InputStream fins) {
		byte[]twoBytes=new byte[2];
		byte[]fourBytes=new byte[4];
		byte[]eightBytes=new byte[8];
		Dictionary dict=null;
		try {
/*----------------------------------标头部分开始--------------------------------------------------------
			https://github.com/zhansliu/writemdict/blob/master/fileformat.md#header-section
---------------------------------------------------------------------------------------------------*/
			
			//获取Header Section的header_str长度			
			fins.read(fourBytes);
			int headerStrLength=Utils.byteArrayToInt(fourBytes);
			
			//获取Header Section的header_str。
			byte[] header_str_bytes=new byte[headerStrLength];
			fins.read(header_str_bytes);
			String headerStr=new String(header_str_bytes, "UTF-16LE").trim();
			
			//解析headerStr，获取字典信息。			
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance(); 
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			StringReader sr = new StringReader(headerStr.trim()); 
			InputSource is = new InputSource(sr); 
			Document document = builder.parse(is);
			Element root=document.getDocumentElement();
			String encoding = root.getAttribute("Encoding");

			//跳过checksum校验信息
			fins.skip(4);
/*----------------------------------关键字部分开始--------------------------------------------------------
			https://github.com/zhansliu/writemdict/blob/master/fileformat.md#keyword-section
---------------------------------------------------------------------------------------------------*/
			fins.read(eightBytes);
		    long key_num_blocks=Utils.byteArrayToLong(eightBytes);//共有多少个key的压缩块
			
			fins.read(eightBytes);			
			long key_sum=Utils.byteArrayToLong(eightBytes);//共有多少个key（单词数目）
			
			fins.read(eightBytes);			
			long key_index_decomp_len=Utils.byteArrayToLong(eightBytes);//keyindex信息解压后的大小
			
			fins.read(eightBytes);			
			long key_index_comp_len=Utils.byteArrayToLong(eightBytes);//keyindex信息压缩时的大小
			
			fins.read(eightBytes);			
			long key_blocks_len=Utils.byteArrayToLong(eightBytes);//key的压缩块总共大小
			
			fins.skip(4);//跳过checksum校验信息
			
			/*----------------------------------关键字--索引信息开始--------------------------------------------------------
			https://github.com/zhansliu/writemdict/blob/master/fileformat.md#keyword-index
			-------------------------------------------------------------------------------------------*/
			//将压缩的key_index_comp_len长度的信息解压出来。
			InputStream index_ds=Utils.decompress(fins,key_index_comp_len);
			long[]comp_size=new long[(int) key_num_blocks];//各个压缩块的压缩长度
			long[]decomp_size=new long[(int) key_num_blocks];//各个压缩块的解压后长度
			long[]num_entries=new long[(int) key_num_blocks];//每个块上存储的key数量
			for(int i=0;i<key_num_blocks;i++) {
				index_ds.read(eightBytes);
				num_entries[i]=Utils.byteArrayToLong(eightBytes);//本块关键字数量
				
				//这个块第一个单词的大小
				index_ds.read(twoBytes);
				short firstSize=Utils.byteArrayToShort(twoBytes);
				
				//这个块的第一个单词
				byte[]wordBytes1=new byte[firstSize];
				index_ds.read(wordBytes1);
				String firstWord=new String(wordBytes1, encoding);
				first_last_keys.add(firstWord);
				index_ds.skip(1);//跳过单词结尾的空字符
				
				//这个块最后一个单词的大小
				index_ds.read(twoBytes);
				short lastSize=Utils.byteArrayToShort(twoBytes);
				
				//这个块最后一个单词
				byte[]wordBytes2=new byte[lastSize];
				index_ds.read(wordBytes2);
				String lastWord=new String(wordBytes2, encoding);
				first_last_keys.add(lastWord);
				index_ds.skip(1);//跳过单词结尾的空字符
				
				//这个块压缩后大小
				index_ds.read(eightBytes);
				comp_size[i]=Utils.byteArrayToLong(eightBytes);
				
				//这个块解压大小
				index_ds.read(eightBytes);
				decomp_size[i]=Utils.byteArrayToLong(eightBytes);
			}
			/*----------------------------------关键字--索引信息结束--------------------------------------------------------
			-------------------------------------------------------------------------------------------*/
			
			/*----------------------------------关键字--块信息开始--------------------------------------------------------
			https://github.com/zhansliu/writemdict/blob/master/fileformat.md#keyword-blocks
			-------------------------------------------------------------------------------------------*/
			for(int i=0;i<key_num_blocks;i++) {//遍历所有块信息，逐个解压缩
				InputStream block_ins=Utils.decompress(fins, comp_size[i]);
				//读取本块所有的关键字
				for(int j=0;j<num_entries[i];j++) {
					//偏移信息
					block_ins.read(eightBytes);
					Long off=Utils.byteArrayToLong(eightBytes);
					
					ByteArrayOutputStream baos=new ByteArrayOutputStream();
					int read=0;//读取到的每个字符
					while ((read = block_ins.read()) > 0) {  
						baos.write(read);
					}
					String keyword=new String(baos.toByteArray(), encoding);
					String keyName=getKeyName(keyword);
					offsetMap.put(keyName, off);
					keyNameList.add(keyName);
				}
			}
			
/*----------------------------------关键字部分结束--------------------------------------------------------
---------------------------------------------------------------------------------------------------*/
/*----------------------------------记录部分开始--------------------------------------------------------
			https://github.com/zhansliu/writemdict/blob/master/fileformat.md#record-section
---------------------------------------------------------------------------------------------------*/
			fins.read(eightBytes);
		    long record_num_blocks=Utils.byteArrayToLong(eightBytes);//记录压缩块总数
			
			fins.read(eightBytes);			
			long record_num_entries=Utils.byteArrayToLong(eightBytes);//字典中的记录总数。
			
			fins.read(eightBytes);			
			long record_index_len=Utils.byteArrayToLong(eightBytes);
			
			fins.read(eightBytes);			
			long record_blocks_len=Utils.byteArrayToLong(eightBytes);//所有记录块的总大小
			
			//获取每个记录块的压缩后和解压时大小
			long[]record_comp_size=new long[(int) record_num_blocks];
			long[]record_decomp_size=new long[(int) record_num_blocks];
			long preBytes=0;
			for(int i=0;i<record_num_blocks;i++) {
				//解析记录部分
				fins.read(eightBytes);
				long record_block_comp_size=Utils.byteArrayToLong(eightBytes);
				record_comp_size[i]=record_block_comp_size;
				
				CompressedRecord record=new CompressedRecord(null,encoding);
				compressedRecords.add(record);
				records.put(preBytes, record);
				
				fins.read(eightBytes);			
				long record_block_decomp_size=Utils.byteArrayToLong(eightBytes);
				preBytes+=record_block_decomp_size;				
			}
			
			//获取每个记录压缩块
			for(int i=0;i<record_num_blocks;i++) {
				byte[] compressedData=Utils.getStreamPart(fins, record_comp_size[i]);
				CompressedRecord record=compressedRecords.get(i);
				record.setCompressedData(compressedData);
			}
/*----------------------------------记录部分结束--------------------------------------------------------
---------------------------------------------------------------------------------------------------*/
		
		dict=new Dictionary(keyNameList, first_last_keys, offsetMap, records);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return dict;
	}
	//将同名词条，转换为编号形式
	private String getKeyName(String keyword) {
		String result=keyword;
		if(offsetMap.containsKey(keyword)) {
			for(int k=1;;k++) {
				String keywordWithNum=keyword+k;
				if(offsetMap.containsKey(keywordWithNum)) {
					continue;
				}else {
					result=keywordWithNum;
					break;
				}
			}
		}
		return result;
	}

}
