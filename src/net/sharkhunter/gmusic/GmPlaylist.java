package net.sharkhunter.gmusic;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class GmPlaylist {

	// Fields
	public boolean Ok;
	private Gm parent;
	private String id;
	private String name;
	private String size;
	private String user;
	private boolean popular;
	
	public GmPlaylist(String parseData,Gm parent) {
		this.Ok=false;
		String[] s=Gm.jsonSplit(parseData);
		this.id=Gm.getField(s, "playlistId");
		this.name=Gm.getField(s, "title");
		if(StringUtils.isEmpty(id)||StringUtils.isEmpty(name))
			return;
		this.parent=parent;
		this.popular=false;
		this.Ok=true;
	}
	
	public GmPlaylist(String name,String id,String size,String user,Gm parent) {
		this.id=id;
		this.name=name;
		this.size=size;
		this.user=user;
		this.parent=parent;
		this.popular=false;
		this.Ok=true;
	}
	
	public GmSong[] getSongs() {
		String page=parent.request(parent.jsonHeader(parent.jsonString("id",id)), "loadplaylist");
		return GmSong.parseSongs(page, parent);
	}
	
	public void downloadAll() {
		GmSong[] s=getSongs();
		for(int i=0;i<s.length;i++)
			s[i].download();
	}
	
	public void setPopular(boolean b) {
		this.popular=b;
	}
	
	
	public String getName() {
		return name;
	}
	
	public String getID() {
		return id;
	}
	
	public String getUser() {
		return user;
	}
	
	public String saveFile() {
		return this.parent.savePath+File.separator+"gs_play_"+
				id+".gsp";
	}
	
	public Gm getParent() {
		return parent;
	}
	
	public String toString() {
		return "Playlist "+name+" from "+user+" has "+size+" songs id is "+id;
	}
	
	public static GmPlaylist[] parsePlaylist(String data,Gm parent) {
		Pattern re=Pattern.compile("\\{playlists:\\[(.*)\\]");
		Matcher m=re.matcher(data);
		GmPlaylist[] res=new GmPlaylist[1];
		if(!m.find()) {
			res[0]=null;
			return res;
		}
		String realData=m.group(1);	
		int size=0;
		int pos=0;
		for(;;) {
			int start=realData.indexOf('{',pos);
			if(start==-1)
				break;
			int stop=realData.indexOf('}',start);
			if(stop==-1)
				break;
			pos=stop;
			GmPlaylist playlist=new GmPlaylist(realData.substring(start+1,stop),parent);
			if(!playlist.Ok)
				continue;
			if(size!=0) {
				GmPlaylist[] newArr=new GmPlaylist[size+1];
				System.arraycopy(res, 0, newArr, 0, size);
				res=newArr;
			}
			res[size]=playlist;
			size++;
		}
		return res;
	}
}
