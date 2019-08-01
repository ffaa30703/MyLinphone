package com.android.flypigeon.home;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import com.android.flypigeon.util.Constant;
import com.android.flypigeon.util.FileName;
import com.keep.lin.R;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
public class MyFileManager extends ListActivity{
	private List<FileName> filePaths = new ArrayList<FileName>();//���浱ǰĿ¼�µ������ļ����ļ�·��
	private String rootPath = "/";//��Ŀ¼·��
	private String parentPath = "/";//��ʼ���ϼ�Ŀ¼·��
	private Button returnRootBtn = null;
	private Button returnParentBtn = null;
	private ArrayList<FileName> selectedFilePath = new ArrayList<FileName>();//���汻ѡ��������ļ�·��
	private TextView mPath;//������ʾ��ǰĿ¼·��
	private String currentPath = null;//��ǰ·��
	private int selectType = 0;
	private MyFileAdapter adapter = null;
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.fileselect_layout);

		Intent intent = getIntent();
		selectType = intent.getExtras().getInt("selectType");

		Button buttonConfirm = (Button) findViewById(R.id.buttonConfirm);
		buttonConfirm.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				if(selectType == Constant.SELECT_FILES){//���ǰΪѡ���ļ�ģʽ�򷵻ص�ǰѡ��������ļ�
					intent.putExtra("selectType", Constant.SELECT_FILES);
					intent.putExtra("files", selectedFilePath);
				}else if(selectType == Constant.SELECT_FILE_PATH){//���ǰΪ�ļ���ѡ��ģʽ�򷵻ص�ǰѡ����ļ���·��
					File file = new File(currentPath);
					intent.putExtra("selectType", Constant.SELECT_FILE_PATH);
					if(file.canWrite()){
						intent.putExtra("fileSavePath", currentPath);
					}
				}
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		Button buttonCancle = (Button) findViewById(R.id.buttonCancle);
		buttonCancle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
			/*	if(selectType == Constant.SELECT_FILE_PATH){//���ǰΪѡ���ļ���ģʽ��˵�������Ǳ����ļ�������������cancel��ť�����
					Intent intent = new Intent();			//˵���û��ܾ�����ļ�������Է�����һ���ܾ�����ļ���ָ�
					intent.setAction(Constant.refuseReceiveFileAction);
					sendBroadcast(intent);
				}  */
				finish();
			}
		});
		
		returnRootBtn = (Button)findViewById(R.id.return_root_path);
		returnRootBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View returnRootBtn) {
				getFileDir(rootPath);
			}
		});
		returnParentBtn = (Button)findViewById(R.id.return_parent_path);
		returnParentBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View returnParentBtn) {
				getFileDir(parentPath);
			}
		});
		
		mPath = (TextView) findViewById(R.id.mPath);
		TextView title = (TextView)findViewById(R.id.file_select_title);
		if(selectType == Constant.SELECT_FILE_PATH){
			title.setText(getString(R.string.select_path_for_save));
		}else{
			title.setText(getString(R.string.select_file_for_send));
		}
		
		getFileDir(rootPath);
	}
	
	/**
	 * @param filePath ��Ҫ�򿪵�Ŀ¼·��
	 * �򿪸�Ŀ¼���������������ļ���Ϣ������Ŀ¼���ļ�
	 * ���������ļ�������fileNames�б��У��������ļ�·�������filePaths�б���
	 */
	private void getFileDir(String filePath) {
		if(null==filePath)return;//����ǲ��ǳ����˸�Ŀ¼
		File dirFile = new File(filePath);
		parentPath = dirFile.getParent();//��õ�ǰĿ¼�ĸ�Ŀ¼
		File[] files = dirFile.listFiles();//��ȡ��ǰĿ¼�µ������ļ�
		if(null!=files){
			filePaths.clear();
			selectedFilePath.clear();
			currentPath = filePath;
			Constant.fileSelectedState.clear();
			mPath.setText(getString(R.string.current_path_label)+filePath);
			for (File file : files) {
				if(selectType == Constant.SELECT_FILE_PATH){//���ѡ��ģʽΪ�ļ���ģʽ��ֻ����ļ���
					if(file.isDirectory()){
						FileName fPath = new FileName(1,file.getPath());
						filePaths.add(fPath);
					}
				}else{//���ѡ��ģʽΪ�ļ�ģʽ���������ļ������ļ�
					if(file.isDirectory()){
						FileName fPath = new FileName(1,file.getPath());
						filePaths.add(fPath);
					}else{
						FileName fPath = new FileName(2,file.getPath(),file.length(),false);
						filePaths.add(fPath);
					}
				}
			}
			Collections.sort(filePaths);//�������򣬰��ļ�������ǰ�棬�ļ����ں���
			if(null==adapter){
				adapter = new MyFileAdapter(this,filePaths);
			}else{
				adapter.setDatasource(filePaths);
			}
			setListAdapter(adapter);//�ѻ�õ��ļ���Ϣ����List���������������������б���Ŀ
		}
	}
	
	/**
	 * ���б��е���Ŀ�����ʱ�ᴥ�����¼�
	 */
	@Override
	protected void onListItemClick(ListView listView, View itemView, int position, long id) {
		File file = new File(filePaths.get(position).fileName);//�����List�б���������item���Ӧ���ļ�
		if (file.isDirectory()) {//�����ļ�ΪĿ¼�ļ���򿪸�Ŀ¼
			getFileDir(filePaths.get(position).fileName);
		} else {//�����ļ���һ����ͨ�ļ����޸ĸ�����ѡ����״̬����ѡ�и��ļ���ȡ��ѡ��
			CheckBox cb = (CheckBox)itemView.findViewById(R.id.file_selected);
			cb.setChecked(!cb.isChecked());//ѡ����ļ���ȡ��ѡ��
			onCheck(cb);//����onCheck����������
		}
	}
	
	//������ܵ�״̬����ݸ�״̬�������ɾ���ļ���Ϣ
	public void onCheck(View fileSelectedCheckBox){
		CheckBox cb = (CheckBox)fileSelectedCheckBox;
		int fileIndex = (Integer)cb.getTag();//��øü������ļ��б��ж�Ӧ����ţ���������б��и���Ŀ�����һ��
		Constant.fileSelectedState.put(fileIndex, cb.isChecked());
		if(cb.isChecked()){//����Ǳ�ѡ���򱣴����Ŷ�Ӧ���ļ���Ϣ
			FileName fName = filePaths.get(fileIndex);
			if(!selectedFilePath.contains(fName))selectedFilePath.add(filePaths.get(fileIndex));
		}else{//���ȡ��ѡ����ӱ�����ļ���Ϣ��ɾ�����Ŷ�Ӧ���ļ���Ϣ
			selectedFilePath.remove(filePaths.get(fileIndex));
		}
	}

//=========================================================================================================================	
/*	private void openFile(File f) 
    {
      Intent intent = new Intent();
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.setAction(android.content.Intent.ACTION_VIEW);
      
      // ����getMIMEType()��ȡ��MimeType 
      String type = getMIMEType(f);
      // ����intent��file��MimeType 
      intent.setDataAndType(Uri.fromFile(f),type);
      startActivity(intent); 
    }

    // �ж��ļ�MimeType��method 
    private String getMIMEType(File f) 
    { 
      String type="";
      String fName=f.getName();
      // ȡ����չ�� 
      String end=fName.substring(fName.lastIndexOf(".")
      +1,fName.length()).toLowerCase(); 
      
      //����չ������;���MimeType 
      if(end.equals("m4a")||end.equals("mp3")||end.equals("mid")||
      end.equals("xmf")||end.equals("ogg")||end.equals("wav"))
      {
        type = "audio"; 
      }
      else if(end.equals("3gp")||end.equals("mp4"))
      {
        type = "video";
      }
      else if(end.equals("jpg")||end.equals("gif")||end.equals("png")||
      end.equals("jpeg")||end.equals("bmp"))
      {
        type = "image";
      }
      else if(end.equals("apk")) 
      { 
        // android.permission.INSTALL_PACKAGES  
        type = "application/vnd.android.package-archive"; 
      } 
      else
      {
        type="*";
      }
      //����޷�ֱ�Ӵ򿪣����������б���û�ѡ�� 
      if(end.equals("apk")) 
      { 
      } 
      else 
      { 
        type += "/*";  
      } 
      return type;  
    } */
}