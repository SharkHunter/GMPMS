package net.sharkhunter.gmusic;

import net.pms.dlna.virtual.*;


public class GmMore extends VirtualFolder{
	private Object[] list;
	private int startIndex;
	
	public GmMore(Object[] objs,int startIndex) {
		super("More",null);
		this.list=objs;
		this.startIndex=startIndex;
	}
	
	public void discoverChildren() {
		int j=0;
		for(int i=startIndex;i<list.length;i++) {
			if(j>Gm.DisplayLimit) {
				addChild(new GmMore(list,i));
				break;
			}
			j++;
			if(list[i] instanceof GmSong)
				addChild(new GmPMSSong((GmSong) list[i]));
			if(list[i] instanceof GmPlaylist)
				addChild(new GmPMSPlaylist((GmPlaylist) list[i]));
		}
	}
	
	public boolean isSearched()  {
		return true;
	}
}
