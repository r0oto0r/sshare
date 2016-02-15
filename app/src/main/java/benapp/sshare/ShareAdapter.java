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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import secret.sharing.ShareHolder;

public class ShareAdapter extends ArrayAdapter<ShareHolder>
{
    private HashMap<ShareHolder, Integer> idMap = new HashMap<ShareHolder, Integer>();

    private class ViewHolder {
        private TextView share;
    }

    public ShareAdapter(Context context, int resource, ArrayList<ShareHolder> objects)
    {
        super(context, resource, objects);
        for (int i = 0; i < objects.size(); ++i)
            idMap.put(objects.get(i), i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.share_adapter_layout, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.share = (TextView) convertView.findViewById(R.id.share_edit_text);

            convertView.setTag(viewHolder);

        } else
        {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        ShareHolder item = getItem(position);
        if (item!= null) {

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try{
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(item);
                objectOutputStream.close();
            }
            catch (IOException e)
            {
                viewHolder.share.setText(e.getMessage());
            }
            viewHolder.share.setText(Base64.toBase64String(byteArrayOutputStream.toByteArray()));
        }

        return convertView;
    }

    @Override
    public void add(ShareHolder shareHolder)
    {
        super.add(shareHolder);
        idMap.put(shareHolder, idMap.size());
    }

    @Override
    public long getItemId(int position) {
        ShareHolder item = getItem(position);
        return idMap.get(item);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}