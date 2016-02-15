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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import java.io.File;

public class SplitDataScreen extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.split_data_activity);

        connectButtonEvents();
        connectCheckBoxEvents();
    }

    private void connectButtonEvents()
    {
        final EditText fileEditText = (EditText) findViewById(R.id.split_data_file_text_edit);
        final EditText stringEditText = (EditText) findViewById(R.id.split_data_string_text_edit);
        final CheckBox fileCheckBox = (CheckBox) findViewById(R.id.split_data_file_check_box);

        (findViewById(R.id.split_data_split_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SplitDataScreen.this, SShareSettingsScreen.class);
                intent.putExtra("isFile", fileCheckBox.isChecked());
                if(fileCheckBox.isChecked())
                    intent.putExtra("Secret", fileEditText.getText());
                else
                    intent.putExtra("Secret", stringEditText.getText());
                startActivity(intent);
            }
        });

        (findViewById(R.id.split_data_open_file_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileChooserDialog fileChooserDialog = new FileChooserDialog(SplitDataScreen.this);
                fileChooserDialog.setFileListener(new FileChooserDialog.FileSelectedListener() {
                    @Override
                    public void fileSelected(final File file) {
                        fileEditText.setText(file.getPath());
                    }
                });
                fileChooserDialog.showDialog();
            }
        });
    }

    private void connectCheckBoxEvents()
    {
        final CheckBox fileCheckBox = (CheckBox) findViewById(R.id.split_data_file_check_box);
        final CheckBox stringCheckBox = (CheckBox) findViewById(R.id.split_string_check_box);

        stringCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fileCheckBox.setChecked(!isChecked);
            }
        });

        fileCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                stringCheckBox.setChecked(!isChecked);
            }
        });
    }
}
