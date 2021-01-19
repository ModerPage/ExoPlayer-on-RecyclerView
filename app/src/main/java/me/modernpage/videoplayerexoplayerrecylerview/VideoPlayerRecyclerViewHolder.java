package me.modernpage.videoplayerexoplayerrecylerview;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;

import me.modernpage.videoplayerexoplayerrecylerview.model.MediaObject;

/**
 * this is just recycler viewholder, that is used between recycler view and recycler view adapter
 * to place resources on list-item efficiently
 */
public class VideoPlayerRecyclerViewHolder extends RecyclerView.ViewHolder {
    FrameLayout media_container;
    TextView title;
    ImageView thumbnail, volumeControl;
    ProgressBar progressBar;
    View parent;

    // this is how setting up glide to load image(thumbnail) to list item
    // injecting once instead of instantiating every time
    RequestManager requestManager;

    public VideoPlayerRecyclerViewHolder(@NonNull View itemView) {
        super(itemView);
        parent = itemView;
        media_container = itemView.findViewById(R.id.media_container);
        thumbnail = itemView.findViewById(R.id.thumbnail);
        title = itemView.findViewById(R.id.title);
        progressBar = itemView.findViewById(R.id.progressBar);
        volumeControl = itemView.findViewById(R.id.volume_control);
    }

    public void onBind(MediaObject mediaObject, RequestManager requestManager) {
//      setting request manager with glide default configurations
        this.requestManager = requestManager;
        parent.setTag(this);
        title.setText(mediaObject.getTitle());
        this.requestManager
                .load(mediaObject.getThumbnail())
                .into(thumbnail);
    }
}
