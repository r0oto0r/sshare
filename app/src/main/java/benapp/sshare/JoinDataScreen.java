/**
 * The MIT License (MIT)
 * Copyright (c) 2016 Benjamin Weigl
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package benapp.sshare;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.UUID;

import secret.sharing.Dealer;
import secret.sharing.ShareHolder;

public class JoinDataScreen extends Activity {

    private ClipboardManager mClipboard;

    private LinearLayout mShareListLinearLayout;
    private ShareAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.join_data_activity);

        mClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        mShareListLinearLayout = (LinearLayout) findViewById(R.id.join_data_share_linear_layout);

        mAdapter = new ShareAdapter(this, R.layout.share_adapter_layout, new ArrayList<ShareHolder>());

        for (int i = 0; i < mAdapter.getCount(); i++) {
            View item = mAdapter.getView(i, null, null);
            mShareListLinearLayout.addView(item);
        }

        String randomName = UUID.randomUUID().toString().replaceAll("-", "");
        ((EditText) findViewById(R.id.join_data_output_file_name_text_edit)).setText(randomName);

        connectButtonEvents();
    }

    private void connectButtonEvents() {
        (findViewById(R.id.join_data_from_clipboard_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClipboard.hasPrimaryClip()) {
                    ClipData data = mClipboard.getPrimaryClip();
                    addShareholder(Base64.decode(data.getItemAt(0).getText().toString()));
                } else {
                    Toast.makeText(JoinDataScreen.this, "Nothing saved on clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        });

        (findViewById(R.id.join_data_from_file_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileChooserDialog fileChooserDialog = new FileChooserDialog(JoinDataScreen.this);
                fileChooserDialog.setFileListener(new FileChooserDialog.FileSelectedListener() {
                    @Override
                    public void fileSelected(final File file) {
                        try{
                            FileInputStream fileInputStream = new FileInputStream(file);
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            while(fileInputStream.available() > 0)
                                byteArrayOutputStream.write(fileInputStream.read());
                            byte[] share = byteArrayOutputStream.toByteArray();
                            addShareholder(Base64.decode(share));
                        }
                        catch (IOException e)
                        {
                            Toast.makeText(JoinDataScreen.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                fileChooserDialog.showDialog();
            }
        });

        final EditText folderEditText = (EditText) findViewById(R.id.join_data_output_folder_text_edit);
        folderEditText.setText(FolderChooserDialog.getDefaultPath().getAbsolutePath());
        (findViewById(R.id.join_data_open_folder_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FolderChooserDialog folderChooserDialog = new FolderChooserDialog(JoinDataScreen.this);
                folderChooserDialog.setFolderListener(new FolderChooserDialog.FolderSelectedListener() {
                    @Override
                    public void fileSelected(final File file) {
                        folderEditText.setText(file.getPath());
                    }
                });
                folderChooserDialog.showDialog();
            }
        });

        (findViewById(R.id.join_data_join_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startJoining();
            }
        });
    }

    private void addShareholder(byte[] byteSet)
    {
        try
        {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteSet);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            ShareHolder shareHolder = (ShareHolder) objectInputStream.readObject();

            mAdapter.add(shareHolder);
            View item = mAdapter.getView(mAdapter.getCount() - 1, null, null);
            mShareListLinearLayout.addView(item);
        }
        catch (Exception e)
        {
            Toast.makeText(JoinDataScreen.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private class DealerSharePackage
    {
        public Dealer dealer;
        public ShareHolder[] shareHolders;
    }

    private class JoinSecretToScreenTask extends AsyncTask<DealerSharePackage, Void, byte[]> {

        ProgressDialog mProcessDialog;

        public void start(DealerSharePackage dealerSharePackage)
        {
            mProcessDialog = new ProgressDialog(JoinDataScreen.this);
            mProcessDialog.setIndeterminate(true);
            mProcessDialog.show();
            this.execute(dealerSharePackage);
        }

        protected byte[] doInBackground(DealerSharePackage... dealerSharePackages) {
            byte[] secret = null;
            try {
                secret = dealerSharePackages[0].dealer.JoinByteArray(dealerSharePackages[0].shareHolders);
            }
            catch (Exception e)
            {
                mProcessDialog.dismiss();
                final String message = e.getMessage();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(JoinDataScreen.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            return secret;
        }

        protected void onPostExecute(byte[] secret) {
            if(secret != null) {
                mProcessDialog.dismiss();

                Dialog dialog = new Dialog(JoinDataScreen.this);
                dialog.setContentView(R.layout.show_secret_dialog_layout);
                dialog.setTitle("Secret");

                EditText secretBytesEditText = (EditText) dialog.findViewById(R.id.show_secret_edit_text);
                EditText secretStringEditText = (EditText) dialog.findViewById(R.id.show_secret_string_edit_text);

                secretBytesEditText.setText(Base64.toBase64String(secret));
                secretStringEditText.setText(new String(secret));

                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                dialog.show();
            }
        }
    }

    private class JoinSecretToFileTask extends AsyncTask<DealerSharePackage, Void, byte[]> {

        ProgressDialog mProcessDialog;
        String mOutputPath;

        public JoinSecretToFileTask(String outputPath)
        {
            mOutputPath = outputPath;
        }

        public void start(DealerSharePackage dealerSecretPackage) {
            mProcessDialog = new ProgressDialog(JoinDataScreen.this);
            mProcessDialog.setIndeterminate(true);
            mProcessDialog.show();
            this.execute(dealerSecretPackage);
        }

        protected byte[] doInBackground(DealerSharePackage... dealerSharePackages) {
            byte[] secret = null;
            try {
                secret = dealerSharePackages[0].dealer.JoinByteArray(dealerSharePackages[0].shareHolders);
            }
            catch (Exception e)
            {
                mProcessDialog.dismiss();
                final String message = e.getMessage();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(JoinDataScreen.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            return secret;
        }

        protected void onPostExecute(byte[] secret)
        {
            if(secret != null) {
                mProcessDialog.dismiss();
                try {
                    Log.d("JoinDataScreen", new String(secret));
                    FileOutputStream fileOutputStream = new FileOutputStream(mOutputPath);
                    fileOutputStream.write(secret);
                    fileOutputStream.flush();
                    fileOutputStream.close();

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(JoinDataScreen.this, "Secret successfully joined!", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Intent intent = new Intent(JoinDataScreen.this, StartScreen.class);
                    startActivity(intent);

                } catch (Exception e) {
                    final String message = e.getMessage();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(JoinDataScreen.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    }

    private void startJoining()
    {
        CheckBox passwordCheckBox = (CheckBox) findViewById(R.id.join_data_password_checkbox);
        EditText passwordEditText = (EditText) findViewById(R.id.join_data_password_text_edit);

        CheckBox outputFolderCheckBox = (CheckBox) findViewById(R.id.join_data_output_folder_check_box);
        EditText outputFolderEditText = (EditText) findViewById(R.id.join_data_output_folder_text_edit);
        EditText outputFileNameEditText = (EditText) findViewById(R.id.join_data_output_file_name_text_edit);

        Dealer dealer = null;
        if(passwordCheckBox.isChecked())
            dealer = new Dealer(2, 2, passwordEditText.getText().toString());
        else
            dealer = new Dealer(2, 2);

        DealerSharePackage dealerSharePackage = new DealerSharePackage();
        dealerSharePackage.dealer = dealer;

        ShareHolder[] shareHolders = new ShareHolder[mAdapter.getCount()];
        for(int i = 0; i < mAdapter.getCount(); i++)
            shareHolders[i] = mAdapter.getItem(i);

        dealerSharePackage.shareHolders = shareHolders;

        if(!outputFolderCheckBox.isChecked())
            new JoinSecretToScreenTask().start(dealerSharePackage);
        else
        {
            String outputPath  = outputFolderEditText.getText().toString() + "/" + outputFileNameEditText.getText().toString();
            JoinSecretToFileTask joinSecretToFileTask = new JoinSecretToFileTask(outputPath);
            joinSecretToFileTask.start(dealerSharePackage);
        }
    }
}
