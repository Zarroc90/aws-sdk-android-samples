package com.amazonaws.demo.androidpubsub;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.demo.androidpubsub.Res.IDProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    private ArrayList<String> mdeviceNames = new ArrayList<>();
    private ArrayList<String> mdeviceImage = new ArrayList<>();
    private ArrayList<String> mpowerText = new ArrayList<>();
    private ArrayList<Boolean> mbuttonState = new ArrayList<>();
    private Context context;

    public interface OnItemLongClickListener {
        public boolean onItemLongClicked(int position);
    }


    public RecyclerViewAdapter(ArrayList<String> mdeviceNames, ArrayList<String> mdeviceImage, ArrayList<String> mpowerText, ArrayList<Boolean> mbuttonState, Context context) {
        this.mdeviceNames = mdeviceNames;
        this.mdeviceImage = mdeviceImage;
        this.mpowerText = mpowerText;
        this.mbuttonState = mbuttonState;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_listitem,viewGroup, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position)  {
        Log.d(TAG,"onBindViewHolder called.");

        Resources resources = context.getResources();
        int resid = resources.getIdentifier(mdeviceImage.get(position),"mipmap", context.getPackageName());

        viewHolder.device_image.setImageResource(resid);
        viewHolder.device_name.setText(mdeviceNames.get(position));
        //viewHolder.power_image.setImageDrawable(Drawable.createFromPath(String.valueOf(R.drawable.power_icon)));
        viewHolder.power_text.setText(mpowerText.get(position));
        String buttonState = "On";
        if(mbuttonState.get(position)){
            buttonState = "On";
            viewHolder.on_off_button.setPressed(true);
        }else{
            buttonState = "Off";
            viewHolder.on_off_button.setPressed(false);
        }
        viewHolder.on_off_button.setText(buttonState);

        viewHolder.on_off_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"onClick: clicked on: "+ mdeviceNames.get(position));
                //Toast.makeText(context, mdeviceNames.get(position),Toast.LENGTH_SHORT).show();
                String state;
                PubSubActivity pubSubActivity = new PubSubActivity();

                Log.d(TAG,"Button State: "+mbuttonState.get(position));
                if(mbuttonState.get(position)){
                    //mbuttonState.set(position,Boolean.FALSE);
                    state="off";
                }else{
                    //mbuttonState.set(position,Boolean.TRUE);
                    state="on";
                }
                Log.d(TAG,"Button State: "+mbuttonState.get(position));
                String topic = "$aws/things/"+mdeviceNames.get(position)+"/shadow/update";
                String msg = "{\"state\":{\"desired\":{\"switch\":\""+state+"\"}}}";
                pubSubActivity.publishMqttMessage(msg,topic,AWSIotMqttQos.QOS0);

                //notifyItemChanged(position);

            }
        });
        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener(){

            @Override
            public boolean onLongClick(View view) {

                RecyclerViewAdapter.OnItemLongClicked(position);
                return false;
            }
        });



    }

    private static void OnItemLongClicked(int position) {
        System.out.println("Item "+position+" pressed long.");

    }

    @Override
    public int getItemCount() {
        return mdeviceNames.size();
    }

    public String getItemname(int position){
        return mdeviceNames.get(position);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView device_image;
        TextView device_name;
        CircleImageView power_image;
        TextView power_text;
        Button on_off_button;
        RelativeLayout parent_layout;
        RelativeLayout view_background;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            device_image=itemView.findViewById(R.id.image);
            device_name=itemView.findViewById(R.id.device_name);
            power_image=itemView.findViewById(R.id.power_icon);
            power_text=itemView.findViewById(R.id.power_text);
            on_off_button=itemView.findViewById(R.id.button);
            parent_layout= itemView.findViewById(R.id.parent_layout);
            view_background = itemView.findViewById(R.id.view_background);
        }


    }

    public void removeItem(int position) {
        mdeviceNames.remove(position);
        mpowerText.remove(position);
        mbuttonState.remove(position);
        mdeviceImage.remove(position);
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()

        //notifyItemRemoved(position);
    }
}
