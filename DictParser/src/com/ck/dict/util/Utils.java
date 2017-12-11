package com.ck.dict.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.InflaterOutputStream;

import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoDecompressor;
import org.anarres.lzo.LzoInputStream;
import org.anarres.lzo.LzoLibrary;

public class Utils {
	/*----------------------------------解压缩部分--------------------------------------------------------
	https://github.com/zhansliu/writemdict/blob/master/fileformat.md#compression
---------------------------------------------------------------------------------------------------*/	
	//用于在解析词典阶段解压缩的方法
	public static InputStream decompress(InputStream fins,long length){
		byte[]  data=getStreamPart(fins,length);
		return decompress(data,data.length);
	}
	
	//用于在查询词条时解压缩记录块
	public static InputStream decompress(byte[] data,long length){
		InputStream ds=null;
		//截取压缩类型
		int flag=data[0];
		//将压缩文件包装为流
		if(flag==0) {
			 ds=new ByteArrayInputStream(data,8,data.length-8);
		}else if(flag==1) {
			//lzo
			LzoAlgorithm algorithm = LzoAlgorithm.LZO1X;  
	        LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(algorithm, null);  
	        ds = new LzoInputStream(new ByteArrayInputStream(data,8,data.length-8), decompressor); 
		}else if(flag==2) {
			//zlib
	        ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		    InflaterOutputStream inf = new InflaterOutputStream(out); 
		    try {
				inf.write(data,8,data.length-8);
				inf.close(); 
			} catch (IOException e) {
				e.printStackTrace();
			}
		    data=out.toByteArray();
		    ds=new ByteArrayInputStream(data);
		}
		return ds;
	}
	
	//用于从词典文件流中截取被压缩的数据部分。
	public static byte[] getStreamPart(InputStream fins,long length) {
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		byte[] temp=new byte[1024];
		long total=length;int i=0;
		while(total!=0){         
			try {
				if(total>1024) {
						i=fins.read(temp);
				}else {
					i=fins.read(temp,0,(int)total);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(i>0) {
				baos.write(temp, 0, i);
				total-=i;
			}
		}
		byte[] compressedData=baos.toByteArray();
		return compressedData;
	}
	
	//用于估计基于当前词典的编码类型的每个关键字的长度，对于UTF8，标记长度就是存储长度，对于UTF-16标记长度是编码单元长度，存储长度还要乘以2.其它以此类推，但是没有加以实现
	private static int lengthOfEncoding(int length,String encoding){
		int result = 0;
		switch (encoding) {
			case "UTF-8":
				result  = length;
				break;
			case "UTF-16":
				result =  length*2;
				break;
		}
		return result;
	}
	
	public static long byteArrayToLong(byte[] eightBytes) {
		ByteBuffer buffer = ByteBuffer.wrap(eightBytes);
		long result=buffer.getLong();
		return result;
	}
	public static int byteArrayToInt(byte[] fourBytes) {
		ByteBuffer buffer = ByteBuffer.wrap(fourBytes);
		int result=buffer.getInt();
		return result;
	}
	public static short byteArrayToShort(byte[] twoBytes) {
		ByteBuffer buffer = ByteBuffer.wrap(twoBytes);
		short result=buffer.getShort();
		return result;
	}
}
