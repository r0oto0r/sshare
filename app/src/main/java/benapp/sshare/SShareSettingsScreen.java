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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.UUID;

import secret.sharing.Dealer;
import secret.sharing.ShareHolder;

public class SShareSettingsScreen extends Activity {


    private ClipboardManager mClipboard;

    private boolean mIsFile;
    private String mPostedSecretString;

    private byte[] mSecret;

    private int mThreshold = 2;
    private int mShareCount = 2;

    private TextView mThresHoldTextView;
    private TextView mSharecountTextView;

    private EditText mEstimatedSizeEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sshare_settings_activity);

        mClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        Bundle bundle = getIntent().getExtras();
        mIsFile = bundle.getBoolean("isFile");
        mPostedSecretString = bundle.getCharSequence("Secret").toString();

        mThresHoldTextView = (TextView) findViewById(R.id.sshare_settings_threshold_textview);
        mSharecountTextView = (TextView) findViewById(R.id.sshare_settings_sharecount_textview);

        mEstimatedSizeEditText = (EditText) findViewById(R.id.sshare_settings_estimated_filesize_text_edit);

        String randomName = UUID.randomUUID().toString().replaceAll("-", "");
        ((EditText) findViewById(R.id.sshare_settings_output_file_name_text_edit)).setText(randomName);

        connectButtonEvents();

        defineSecret();
        calculateEstimatedShareSize();
    }

    private void connectButtonEvents() {
        (findViewById(R.id.sshare_settings_split_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSplitting();
            }
        });

        final EditText folderEditText = (EditText) findViewById(R.id.sshare_settings_output_folder_text_edit);
        folderEditText.setText(FolderChooserDialog.getDefaultPath().getAbsolutePath());
        (findViewById(R.id.sshare_settings_open_folder_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FolderChooserDialog folderChooserDialog = new FolderChooserDialog(SShareSettingsScreen.this);
                folderChooserDialog.setFolderListener(new FolderChooserDialog.FolderSelectedListener() {
                    @Override
                    public void fileSelected(final File file) {
                        folderEditText.setText(file.getPath());
                    }
                });
                folderChooserDialog.showDialog();
            }
        });

        setupNumberSelectors();
    }

    private void setupNumberSelectors() {
        Button thresholdUpButton = (Button) findViewById(R.id.sshare_settings_threshold_button_up);
        Button thresholdDownButton = (Button) findViewById(R.id.sshare_settings_threshold_button_down);
        Button shareCountUpButton = (Button) findViewById(R.id.sshare_settings_sharecount_button_up);
        Button shareCountDownButton = (Button) findViewById(R.id.sshare_settings_sharecount_button_down);

        thresholdUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setThreshold(mThreshold + 1);
            }
        });

        thresholdDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setThreshold(mThreshold - 1);
            }
        });

        shareCountUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setShareCount(mShareCount + 1);
            }
        });

        shareCountDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setShareCount(mShareCount - 1);
            }
        });
    }

    private void setThreshold(int value) {
        if(value >= 2 && value <= 9)
        {
            mThreshold = value;
            mThresHoldTextView.setText(Integer.toString(mThreshold));
        }

        if(mThreshold > mShareCount)
            setShareCount(mThreshold);

    }

    private void setShareCount(int value) {
        if(value >= 2 && value <= 9)
        {
            mShareCount = value;
            mSharecountTextView.setText(Integer.toString(mShareCount));
        }

        if(mShareCount < mThreshold)
            setThreshold(mShareCount);
    }

    private void defineSecret()
    {
        if(!mIsFile)
        {
            mSecret = mPostedSecretString.getBytes();
        }
        else
        {
            try
            {
                FileInputStream fileInputStream = new FileInputStream(new File(mPostedSecretString));
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while(fileInputStream.available() > 0)
                    byteArrayOutputStream.write(fileInputStream.read());
                mSecret = byteArrayOutputStream.toByteArray();
            }
            catch(Exception e)
            {
                Toast.makeText(SShareSettingsScreen.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void calculateEstimatedShareSize()
    {
        long blockSize = 16;
        long secretHeader = 20;
        long encryptionHeader = 80;
        long containerHeader = 8;
        long secretLength = mSecret.length;

        long estimatedShareSize = (blockSize - ((secretLength + secretHeader) % blockSize)) + secretLength + encryptionHeader + containerHeader;

        mEstimatedSizeEditText.setText(sizeToString(estimatedShareSize));
    }

    private String sizeToString(long size)
    {
        String retValue = "";
        if(size > 1024)
        {
            double newSize = Math.floor(size / 1024.0f);
            if(newSize > 1024)
            {
                newSize = Math.floor(newSize / 1024.0f);
                if(newSize > 1024)
                {
                    newSize = Math.floor(newSize / 1024.0f);
                    retValue = Double.toString(newSize) + " GByte";
                }
                else
                    retValue = Double.toString(newSize) + " MByte";
            }
            else
                retValue = Double.toString(newSize) + " KByte";
        }
        else
            retValue = Long.toString(size) + " Byte";

        return retValue;
    }

    private class DealerSecretPackage
    {
        public Dealer dealer;
        public byte[] secret;
    }

    private class SplitSecretToScreenTask extends AsyncTask<DealerSecretPackage, Void, ShareHolder[]> {

        ProgressDialog mProcessDialog;

        public void start(DealerSecretPackage dealerSecretPackage)
        {
            mProcessDialog = new ProgressDialog(SShareSettingsScreen.this);
            mProcessDialog.setIndeterminate(true);
            mProcessDialog.show();
            this.execute(dealerSecretPackage);
        }

        protected ShareHolder[] doInBackground(DealerSecretPackage... dealerSecretPackages) {
            ShareHolder[] shareHolders = null;
            try {
                shareHolders = dealerSecretPackages[0].dealer.SplitByteArray(dealerSecretPackages[0].secret);
            }
            catch (Exception e)
            {
                mProcessDialog.dismiss();
                final String message = e.getMessage();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(SShareSettingsScreen.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            return shareHolders;
        }

        protected void onPostExecute(ShareHolder[] shareHolders) {
            if(shareHolders != null) {
                mProcessDialog.dismiss();

                Dialog dialog = new Dialog(SShareSettingsScreen.this);
                dialog.setContentView(R.layout.share_list_view_layout);
                dialog.setTitle("Shares");
                ListView list = (ListView) dialog.findViewById(R.id.share_list_view);

                ArrayList<ShareHolder> shareHolderArrayList = new ArrayList<>();
                for (int i = 0; i < shareHolders.length; i++)
                    shareHolderArrayList.add(shareHolders[i]);

                ShareAdapter adapter = new ShareAdapter(SShareSettingsScreen.this, R.layout.share_adapter_layout, shareHolderArrayList);

                list.setAdapter(adapter);

                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        try {
                            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                            objectOutputStream.writeObject(parent.getItemAtPosition(position));
                            objectOutputStream.close();
                        } catch (IOException e) {
                            Toast.makeText(SShareSettingsScreen.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        ClipData clip = ClipData.newPlainText("Share", Base64.toBase64String(byteArrayOutputStream.toByteArray()));
                        mClipboard.setPrimaryClip(new ClipData(clip));
                        Toast.makeText(SShareSettingsScreen.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                        view.setBackgroundColor(Color.rgb(149, 196, 144));
                    }
                });

                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                dialog.show();
            }
        }
    }

    private class SplitSecretToFilesTask extends AsyncTask<DealerSecretPackage, Void, ShareHolder[]> {

        ProgressDialog mProcessDialog;
        String mOutputPath;

        public SplitSecretToFilesTask(String outputPath)
        {
            mOutputPath = outputPath;
        }

        public void start(DealerSecretPackage dealerSecretPackage)
        {
            mProcessDialog = new ProgressDialog(SShareSettingsScreen.this);
            mProcessDialog.setIndeterminate(true);
            mProcessDialog.show();
            this.execute(dealerSecretPackage);
        }

        protected ShareHolder[] doInBackground(DealerSecretPackage... dealerSecretPackages) {
            ShareHolder[] shareHolders = null;
            try {
                shareHolders = dealerSecretPackages[0].dealer.SplitByteArray(dealerSecretPackages[0].secret);
            }
            catch (Exception e)
            {
                mProcessDialog.dismiss();
                final String message = e.getMessage();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(SShareSettingsScreen.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            return shareHolders;
        }

        protected void onPostExecute(ShareHolder[] shareHolders)
        {
            if(shareHolders != null) {
                try {
                    mProcessDialog.dismiss();

                    for (int i = 0; i < shareHolders.length; i++) {
                        FileOutputStream fileOutputStream = new FileOutputStream(mOutputPath + "." + Integer.toString(i) + ".share");

                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                        objectOutputStream.writeObject(shareHolders[i]);
                        objectOutputStream.flush();
                        objectOutputStream.close();

                        fileOutputStream.write(Base64.encode(byteArrayOutputStream.toByteArray()));
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(SShareSettingsScreen.this, "Secret successfully split!", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Intent intent = new Intent(SShareSettingsScreen.this, StartScreen.class);
                    startActivity(intent);
                } catch (Exception e) {
                    final String message = e.getMessage();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(SShareSettingsScreen.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    }

    private void startSplitting()
    {
        CheckBox passwordCheckBox = (CheckBox) findViewById(R.id.sshare_settings_password_checkbox);
        EditText passwordEditText = (EditText) findViewById(R.id.sshare_settings_password_text_edit);

        CheckBox outputFolderCheckBox = (CheckBox) findViewById(R.id.sshare_settings_output_folder_check_box);
        EditText outputFolderEditText = (EditText) findViewById(R.id.sshare_settings_output_folder_text_edit);
        EditText outputFileNameEditText = (EditText) findViewById(R.id.sshare_settings_output_file_name_text_edit);

        Dealer dealer = null;
        if(passwordCheckBox.isChecked())
            dealer = new Dealer(mThreshold, mShareCount, passwordEditText.getText().toString());
        else
            dealer = new Dealer(mThreshold, mShareCount);

        DealerSecretPackage dealerSecretPackage = new DealerSecretPackage();
        dealerSecretPackage.dealer = dealer;
        dealerSecretPackage.secret = mSecret;

        if(!outputFolderCheckBox.isChecked())
            new SplitSecretToScreenTask().start(dealerSecretPackage);
        else
        {
            String outputPath  = outputFolderEditText.getText().toString() + "/" + outputFileNameEditText.getText().toString();
            SplitSecretToFilesTask splitSecretToFilesTask = new SplitSecretToFilesTask(outputPath);
            splitSecretToFilesTask.start(dealerSecretPackage);
        }
    }
}
