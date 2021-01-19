package me.modernpage.videoplayerexoplayerrecylerview;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;

import java.util.ArrayList;

import me.modernpage.videoplayerexoplayerrecylerview.model.MediaObject;

/**
 * this is just an adapter for our custom recycler view, all the media objects and glide request manager
 * is injected here, and onBind method invoke onBind of viewholder object
 *
 * all other logic to set mediaObject's content to view will be implemented on {@link CustomRecyclerView}
 */

public class VideoPlayerRecyclerViewAdapter extends RecyclerView.Adapter<VideoPlayerRecyclerViewHolder> {

    private ArrayList<MediaObject> mMediaObjects;
    private RequestManager mRequestManager;

    public VideoPlayerRecyclerViewAdapter(ArrayList<MediaObject> mediaObjects, RequestManager requestManager) {
        mMediaObjects = mediaObjects;
        mRequestManager = requestManager;
    }

    @NonNull
    @Override
    public VideoPlayerRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VideoPlayerRecyclerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_video_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VideoPlayerRecyclerViewHolder holder, int position) {
        holder.onBind(mMediaObjects.get(position), mRequestManager);
    }

    @Override
    public int getItemCount() {
        return mMediaObjects.size();
    }
}
