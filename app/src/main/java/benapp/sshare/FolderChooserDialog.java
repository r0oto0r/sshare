/**
 * By Kirill Mikhailov (http://stackoverflow.com/users/1713511/kirill-mikhailov)
 * Modified by Benjamin Weigl
 * Origin: http://stackoverflow.com/questions/3592717/choose-file-dialog (Feb. 2016)
 */

package benapp.sshare;

import android.app.Activity;
import android.app.Dialog;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

public class FolderChooserDialog {
    private static final String PARENT_DIR = "..";

    private final Activity activity;
    private ListView list;
    private Dialog dialog;
    private File currentPath;

    public interface FolderSelectedListener {
        void fileSelected(File file);
    }
    public FolderChooserDialog setFolderListener(FolderSelectedListener folderListener) {
        this.folderListener = folderListener;
        return this;
    }
    private FolderSelectedListener folderListener;

    public FolderChooserDialog(Activity activity) {
        this.activity = activity;
        dialog = new Dialog(activity);
        dialog.setContentView(R.layout.folder_chooser_dialog_layout);
        list = (ListView) dialog.findViewById(R.id.folder_chooser_list_view);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int which, long id) {
                String fileChosen = (String) list.getItemAtPosition(which);
                File chosenFile = getChosenFile(fileChosen);
                if (chosenFile.isDirectory()) {
                    refresh(chosenFile);
                }
            }
        });

        dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        File store = getDefaultPath();
        refresh(store);
        connectButtonEvent();
    }

    public static File getDefaultPath()
    {
        File path = Environment.getExternalStorageDirectory();
        if(path == null)
            path = Environment.getRootDirectory();
        return path;
    }

    public void showDialog() {
        dialog.show();
    }

    private void connectButtonEvent()
    {
        Button folderChooserButton = (Button) dialog.findViewById(R.id.folder_chooser_button);
        folderChooserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (folderListener != null) {
                    folderListener.fileSelected(currentPath);
                }
                dialog.dismiss();
            }
        });
    }

    private void refresh(File path) {
        this.currentPath = path;
        if (path.exists()) {
            File[] dirs = path.listFiles(new FileFilter() {
                @Override public boolean accept(File file) {
                    return (file.isDirectory() && file.canRead());
                }
            });

            int i = 0;
            String[] fileList;
            if (path.getParentFile() == null) {
                fileList = new String[dirs.length];
            } else {
                fileList = new String[dirs.length + 1];
                fileList[i++] = PARENT_DIR;
            }
            Arrays.sort(dirs);
            for (File dir : dirs) { fileList[i++] = dir.getName(); }

            dialog.setTitle(currentPath.getPath());

            list.setAdapter(new ArrayAdapter(activity,
                    android.R.layout.simple_list_item_1, fileList) {
                @Override public View getView(int pos, View view, ViewGroup parent) {
                    view = super.getView(pos, view, parent);
                    ((TextView) view).setSingleLine(true);
                    return view;
                }
            });
        }
    }

    private File getChosenFile(String fileChosen) {
        if (fileChosen.equals(PARENT_DIR)) {
            return currentPath.getParentFile();
        } else {
            return new File(currentPath, fileChosen);
        }
    }
}