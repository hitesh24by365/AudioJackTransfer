/*		
* Copyright (C) 2011 Androino authors		
*		
* Licensed under the Apache License, Version 2.0 (the "License");		
* you may not use this file except in compliance with the License.		
* You may obtain a copy of the License at		
*		
*      http://www.apache.org/licenses/LICENSE-2.0		
*		
* Unless required by applicable law or agreed to in writing, software		
* distributed under the License is distributed on an "AS IS" BASIS,		
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.		
* See the License for the specific language governing permissions and		
* limitations under the License.		
*/
package com.slk.androidaudio.fsk;

public class AndroinoException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	public static final int TYPE_FSK_DECODING_ERROR = 1001;
	public static final int TYPE_FSK_DEBUG = 1002;
	
	private int type;
	private Object debugInfo;
	
	public int getType(){
		return this.type;
	}
	
	public AndroinoException(String detailMessage, Throwable throwable, int type) {
		super(detailMessage, throwable);
		this.type = type;
	}

	public AndroinoException(String detailMessage, int type) {
		super(detailMessage);
		this.type = type;
	}
	void setDebugInfo(Object o){
		this.debugInfo = o;
	}
	Object getDebugInfo(){
		return this.debugInfo;
	}
}
