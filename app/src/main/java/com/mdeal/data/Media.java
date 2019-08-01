package com.mdeal.data;

import java.io.Serializable;

public class Media  implements Serializable{

	private static final long serialVersionUID = 1L;
	private int id;
	private String  path;
	private String name;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getName(){
		return name;
	}
	
	public void setName(String nikeName){
		this.name = nikeName;
	}

}
