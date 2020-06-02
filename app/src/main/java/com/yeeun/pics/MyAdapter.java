package com.yeeun.pics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MyAdapter extends BaseAdapter {
    Context mContext = null;
    ArrayList<EventData> eventList;

    public MyAdapter(Context context, ArrayList<EventData> eventList){
        this.mContext = context;
        this.eventList = eventList;
    }

    @Override
    public int getCount() {
        return eventList.size();
    }

    @Override
    public Object getItem(int i) {
        return eventList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        final Context context = viewGroup.getContext();
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_custom, viewGroup, false);
        }

        TextView title = (TextView)convertView.findViewById(R.id.event_title);
        TextView startHour = (TextView)convertView.findViewById(R.id.event_start_hour);
        TextView startMin = (TextView)convertView.findViewById(R.id.event_start_min);
        TextView endHour = (TextView)convertView.findViewById(R.id.event_end_hour);
        TextView endMin = (TextView)convertView.findViewById(R.id.event_end_min);

        String sh = Integer.toString(eventList.get(i).getStartHour());
        String sm = Integer.toString(eventList.get(i).getStartMin());
        String eh = Integer.toString(eventList.get(i).getEndHour());
        String em = Integer.toString(eventList.get(i).getEndMin());
        if(sh.length() == 1) sh = "0".concat(sh);
        if(sm.length() == 1) sm = "0".concat(sm);
        if(eh.length() == 1) eh = "0".concat(eh);
        if(em.length() == 1) em = "0".concat(em);

        title.setText(eventList.get(i).getTitle());
        startHour.setText(sh);
        startMin.setText(sm);
        endHour.setText(eh);
        endMin.setText(em);

        return convertView;
    }
}
