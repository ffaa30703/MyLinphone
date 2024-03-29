/*
AndroidVideoWindowImpl.java
Copyright (C) 2010  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.linphone.mediastream.video;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.linphone.mediastream.Log;
import org.linphone.mediastream.video.display.OpenGLESDisplay;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.opengl.GLSurfaceView;
import android.view.Surface;
import android.view.Surface.OutOfResourcesException;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
/**
 * AndroidVideoWindowImp
 * @author chris
 *
 */
public class AndroidVideoWindowImpl {
	/**
	 * 从网络上传输过来的视频
	 */
	private SurfaceView mVideoRenderingView;
	/**
	 * 从摄像头上获取到的视频
	 */
	private SurfaceView mVideoPreviewView;
	
	private boolean useGLrendering;
	private Bitmap mBitmap; 

	private Surface mSurface; 
	private VideoWindowListener mListener;
	private Renderer renderer;
	
	/**
	 * Utility listener interface providing callback for Android events
	 * useful to Mediastreamer.
	 */
	public static interface VideoWindowListener{
		//准备一个surfaceView用于接受从网络上传过来的视频的
		void onVideoRenderingSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface);
		//销毁掉存放网络传输过来的视频的surfaceView
		void onVideoRenderingSurfaceDestroyed(AndroidVideoWindowImpl vw);
		
		//准备一个surfaceView用来存放摄像头获取到的视频
		void onVideoPreviewSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface);
		//销毁掉存放摄像头获取到的视频
		void onVideoPreviewSurfaceDestroyed(AndroidVideoWindowImpl vw);
	};
	
	/**
	 * @param renderingSurface Surface created by the application that will be used to render decoded video stream（网络视频）
	 * @param previewSurface Surface created by the application used by Android's Camera preview framework（本地视频）
	 */
	public AndroidVideoWindowImpl(SurfaceView renderingSurface, SurfaceView previewSurface) {
		mVideoRenderingView = renderingSurface;
		mVideoPreviewView = previewSurface;
		//判断renderingSurface是否为GLSurfaceView的对象，用着个判断来区分是接电话还是打电话
		useGLrendering = (renderingSurface instanceof GLSurfaceView);
		
		mBitmap = null;
		mSurface = null;
		mListener = null;
	}
	
	public void init() {
		// register callback for rendering surface events
		mVideoRenderingView.getHolder().addCallback(new Callback(){
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
				Log.i("Video display surface is being changed.");
				if (!useGLrendering) {
					synchronized(AndroidVideoWindowImpl.this){
						mBitmap=Bitmap.createBitmap(width,height,Config.ARGB_8888);
						mSurface=holder.getSurface();
					}
				}
				if (mListener!=null) mListener.onVideoRenderingSurfaceReady(AndroidVideoWindowImpl.this, mVideoRenderingView);
				Log.w("Video display surface changed");
			}

			public void surfaceCreated(SurfaceHolder holder) {
				Log.w("Video display surface created");
			}

			public void surfaceDestroyed(SurfaceHolder holder) {
				if (!useGLrendering) {
					synchronized(AndroidVideoWindowImpl.this){
						mSurface=null;
						mBitmap=null;
					}
				}
				if (mListener!=null)
					mListener.onVideoRenderingSurfaceDestroyed(AndroidVideoWindowImpl.this);
				Log.d("Video display surface destroyed"); 
			}
		});
		// register callback for preview surface events
		if (mVideoPreviewView != null) {
			mVideoPreviewView.getHolder().addCallback(new Callback(){
				public void surfaceChanged(SurfaceHolder holder, int format,
						int width, int height) {
					Log.i("Video preview surface is being changed.");
					if (mListener!=null) 
						mListener.onVideoPreviewSurfaceReady(AndroidVideoWindowImpl.this, mVideoPreviewView);
					Log.w("Video preview surface changed");
				}

				public void surfaceCreated(SurfaceHolder holder) {
					Log.w("Video preview surface created");
				}

				public void surfaceDestroyed(SurfaceHolder holder) {
					if (mListener!=null)
						mListener.onVideoPreviewSurfaceDestroyed(AndroidVideoWindowImpl.this);
					Log.d("Video preview surface destroyed"); 
				}
			});
		}
		
		if (useGLrendering) {
			renderer = new Renderer();
			((GLSurfaceView)mVideoRenderingView).setRenderer(renderer);
			((GLSurfaceView)mVideoRenderingView).setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		}
	}
	
	public void release() {
		//mSensorMgr.unregisterListener(this);
	}

	public void setListener(VideoWindowListener l){
		mListener=l; 
	}
	public Surface getSurface(){
		if (useGLrendering)
			Log.e("View class does not match Video display filter used (you must use a non-GL View)");
		return mVideoRenderingView.getHolder().getSurface();
	}
	public Bitmap getBitmap(){
		if (useGLrendering)
			Log.e( "View class does not match Video display filter used (you must use a non-GL View)");
		return mBitmap;
	}
	 
	public void setOpenGLESDisplay(int ptr) {
		if (!useGLrendering)
			Log.e("View class does not match Video display filter used (you must use a GL View)");
		renderer.setOpenGLESDisplay(ptr);
	}
	
	public void requestRender() {
		((GLSurfaceView)mVideoRenderingView).requestRender();
	}
	
	//Called by the mediastreamer2 android display filter 
	public synchronized void update(){
		if (mSurface!=null){
			try {
				Canvas canvas=mSurface.lockCanvas(null); 
				canvas.drawBitmap(mBitmap, 0, 0, null);
				mSurface.unlockCanvasAndPost(canvas);
				 
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OutOfResourcesException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		} 
	}
	
    private static class Renderer implements GLSurfaceView.Renderer {
    	int ptr;
    	boolean initPending;
    	int width, height;
    	
    	public Renderer() {
    		ptr = 0;
    		initPending = false;
    	}
    	 
    	public void setOpenGLESDisplay(int ptr) {
    		/* 
    		 * Synchronize this with onDrawFrame:
    		 * - they are called from different threads (Rendering thread and Linphone's one)
    		 * - setOpenGLESDisplay can modify ptr while onDrawFrame is using it
    		 */
    		synchronized (this) {
	    		if (this.ptr != 0 && ptr != this.ptr) {
	    			initPending = true;
	    		}
	    		this.ptr = ptr;
    		}
    	}

        public void onDrawFrame(GL10 gl) {
        	/*
        	 * See comment in setOpenGLESDisplay
        	 */
        	synchronized (this) {
	        	if (ptr == 0)
	        		return;
	        	if (initPending) {
	            	OpenGLESDisplay.init(ptr, width, height);
	            	initPending = false;
	        	}
	            OpenGLESDisplay.render(ptr);
        	}
        }
        
        public void onSurfaceChanged(GL10 gl, int width, int height) {
        	/* delay init until ptr is set */
        	this.width = width;
        	this.height = height;
        	initPending = true;
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
           
        }
    }
    
	public static int rotationToAngle(int r) {
		switch (r) {
		case Surface.ROTATION_0:
			return 0;
		case Surface.ROTATION_90:
			return 90;
		case Surface.ROTATION_180:
			return 180;
		case Surface.ROTATION_270:
			return 270;
		}
		return 0;
	}
}


