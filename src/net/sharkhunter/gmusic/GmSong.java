package net.sharkhunter.gmusic;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;


import org.apache.commons.lang.StringUtils;


public class GmSong implements Runnable,Comparable<Object> {
	// Special flag to indicate if parsing worked
	public boolean Ok;
	// Fields
	private String id;
	private String name;
	private String artist;
	private String artistId;
	private String album;
	private String albumId;
	private String coverURL;
	private String plays;
	private int trackNo;
	
	// Internals
	private String streamURL;
	private OutputStream[] outStream;
	private InputStream inStream;
	
	private Gm parent;
	private boolean streamFetched;
	private long length;
	
	
	public GmSong(String parseData,Gm parent) {
		this.Ok=false;

		//parent.debug("gs song parse "+parseData);
		String[] s=Gm.jsonSplit(parseData);
		this.id=Gm.getField(s,"id");
		this.name=Gm.getField(s,"name");
		if(name==null||name.length()==0)
			name=Gm.getField(s,"title"); // try this instead
//		parent.debug("name si "+name.replace("\\u00e4", "ä"));
		name=name.replace("\\u00e4", "ä").replace("\\u00e5", "å").replace("\\u00f6", "ö");
		this.artist=Gm.getField(s,"artist");
		this.album=Gm.getField(s,"album");
		coverURL=Gm.getField(s, "albumArtUrl");
		//this.plays=Gm.getField(s,"plays");
		try {
			this.trackNo=Integer.parseInt(Gm.getField(s,"track"));
		}
		catch (Exception e) {
			this.trackNo=0;
		}
		this.parent=parent;
		this.streamFetched=false;
		this.length=0;
		this.Ok=true;		
	}
	
	public String toString() {
		return "Song: "+this.name+" Album: "+this.album+" Artist: "+this.artist+
		       " SongId: "+this.id+" trackNum "+this.trackNo;
	}
	
	public void fetchStreamData() {
		if(this.streamFetched) // No need to fetch it twice
			return;
		String param="songid="+id;
		String page=parent.request(param, "play","");
		String[] kv=page.split(":",2);
		if(kv.length<2)
			return;
		if(!kv[0].contains("url"))
			return;
		streamURL=kv[1].replaceAll("\\}", "").trim();
		streamFetched=true;
	}
	
	public static GmSong[] parseSongs(String data,Gm parent) {
		int size=0;
		int pos=0;
		Pattern re;
		GmSong[] res=new GmSong[1];
		re=Pattern.compile("\\[(.*)\\]");
		Matcher m=re.matcher(data);
		if(!m.find()) {
			res[0]=null;
			return res;
		}
		data=m.group(1);
		for(;;) {
			int start=data.indexOf('{',pos);
			if(start==-1)
				break;
			int stop=data.indexOf('}',start+1);
			if(stop==-1)
				break;
			pos=stop;
			GmSong song=new GmSong(data.substring(start+1,stop),parent);
			if(!song.Ok)
				continue;
			if(size!=0) {
				GmSong[] newArr=new GmSong[size+1];
				System.arraycopy(res, 0, newArr, 0, size);
				res=newArr;
			}
			res[size]=song;
			size++;
		}
		return res;
	}
	
	public final void run() {
		retriveData(this.outStream,this.inStream);
	}
	
	private void retriveData(OutputStream[] os,InputStream is) {
		try {
			//Get Response	
			byte[] buf=new byte[1];
			for(;;) {
				if(is.read(buf,0,1)==-1)
					break;
				for(int i=0;i<os.length;i++) 
					os[i].write(buf,0,1);
			}	
			for(int i=0;i<os.length;i++) {
				os[i].flush();	
				os[i].close();
			}
			is.close();
		}
		catch (Exception e) {
			parent.error("GSsong error reading data "+e.toString());
			return ;
		}
   }
	
	public void download() {
		download(this.fileName(),false);
	}
	
	public void download(boolean spawn) {
		download(this.fileName(),spawn);
	}
	
	public void download(String file) {
		download(file,false);
	}
	
	public void download(String file,boolean spawn) {
		try {
			FileOutputStream out=new FileOutputStream(file);
			download(out,spawn);
		}
		catch (Exception e) {
			return ;
		}
	}
	
	public void download(OutputStream out) {
		download(out,false);
	}
	
	public void download(OutputStream[] out) {
		download(out,false);
	}
	
	public void download(OutputStream out,boolean spawn) {
		OutputStream[] os=new OutputStream[1];
		os[0]=out;
		download(os,spawn);
	}
	
	public void download(OutputStream[] out,boolean spawn) {
		if(!this.Ok)
			return;
		fetchStreamData();
		if(StringUtils.isEmpty(streamURL)) { // no stream key? give up
			return;
		}
		try {
			URL url=new URL(streamURL);
			HttpURLConnection conn =(HttpURLConnection)url.openConnection();
			HttpURLConnection.setFollowRedirects(true);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			this.inStream=conn.getInputStream();
		}
		catch (Exception e) {
			return;
		}
		if(spawn) {
			this.outStream=out;
			if(inStream==null)  // something is terrible wrong, give up early
				return;
			Thread t=new Thread(this);
			t.start();
		}
		else
			retriveData(out,inStream);
	}
	
	public String streamURL() {
		return streamURL;
	}
	
	public String getName() {
		return name.trim();
	}
	
	public String fileName() {
		String noSlashU=getName().replace("\\u", "_");
		return this.parent.savePath+File.separator+noSlashU+".mp3";
	}
	
	
	public String getAlbum() {
		return this.album;
	}
	
	public String getAlbumId() {
		return this.albumId;
	}
	
	public String getArtist() {
		return this.artist;	
	}
	
	public String getArtistId() {
		return this.artistId;
	}
	
	public String getId() {
		return id;
	}
	
	public boolean save() {
		return parent.saveSong();
	}
	
	public void setId(String id) {
		this.id=id;
	}
	
	public long getLength() {
		return length;
	}
	
	public String savePath() {
		return parent.savePath;
	}
	
	public int delay() {
		return parent.delay;
	}
	
	public String getCoverURL() {
		return parent.cover(coverURL);
	}
	
	public int trackNum(){
		return trackNo;
	}
	
	public void setTrack(int t) {
		trackNo=t;
	}
	
	// Compare function
	
	public int compareTo(Object o) {
		GmSong s=(GmSong)o;
		if(this.trackNo>s.trackNo)
			return 1;
		if(this.trackNo<s.trackNo)
			return -1;
		if(this.trackNo==s.trackNo) {
			return this.plays.compareTo(s.plays);
		}
		throw new ClassCastException();
	}
	
}
