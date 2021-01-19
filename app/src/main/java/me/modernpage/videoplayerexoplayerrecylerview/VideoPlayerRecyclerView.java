package me.modernpage.videoplayerexoplayerrecylerview;

import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

import me.modernpage.videoplayerexoplayerrecylerview.model.MediaObject;

/**
 * custom recycler view to show all the media objects on it
 */
public class VideoPlayerRecyclerView extends RecyclerView {
    private static final String TAG = "VideoPlayerRecyclerView";

    private enum VolumeState {ON, OFF}

    // ui
    private ImageView thumbnail, volumeControl;
    private ProgressBar progressBar;
    private View viewHolderParent;
    private FrameLayout frameLayout;
    // this is exoplayer video view, ui component(container), it is either inserted or removed from list-item
    private PlayerView videoSurfaceView;
    // this is actual exo player to play a video on exoplayer video view
    private SimpleExoPlayer videoPlayer;

    // vars
    // this is same list that is inside of recycler view adapter
    private ArrayList<MediaObject> mediaObjects = new ArrayList<>();
    private int videoSurfaceDefaultHeight = 0;
    // total height of device display, use this to play which video by calculation
    // algorithm works like gives more preference to play video where list item's sides are not cut off by any edges by screen
    // give more preference for top list item if there is more than one possible list item
    private int screenDefaultHeight = 0;
    private Context context;
    // position of the list item which is playing video right now
    private int playPosition = -1;
    private boolean isVideoViewAdded;
    private RequestManager requestManager;

    // controlling playback state
    private VolumeState volumeState;

    public VideoPlayerRecyclerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    // this is constructor containing attributeSet is mandatory, because we use this in the layout
    // otherwise we can't add the recycler view on the layout
    public VideoPlayerRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    // initializing method for the recycler view
    private void init(Context context){

        // getting application context to get screen size
        this.context = context.getApplicationContext();
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);

        // setting video view default height as screen x point,
        // because exoplayer video view height is measured by x coordinate (horizontally)
        videoSurfaceDefaultHeight = point.x;

        // scree default height is screen equals to y coordination as usual
        screenDefaultHeight = point.y;

        videoSurfaceView = new PlayerView(this.context);

        // this is mandatory setting, because we are adding surface view dynamically
        // tell the view when inserting video view does not fit on the view we are inserting to
        // by this setting it automatically use the video aspect ratio, and fit inside view perfectly
        videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(context).build();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory();
        TrackSelector trackSelector =
                new DefaultTrackSelector(context, videoTrackSelectionFactory);

        // 2. Create the player
        videoPlayer = new SimpleExoPlayer.Builder(context).build();

        // Bind the player to the view.
        // disabling some default controllers to video view
        videoSurfaceView.setUseController(false);
        videoSurfaceView.setPlayer(videoPlayer);
        setVolumeControl(VolumeState.ON);

        // scrolling listener to recycler view
        // reason of defining this listener we can't just change anything on list-item
        // until the recycler view has stopped moving
        addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // check whether the scroll view has stopped moving
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Log.d(TAG, "onScrollStateChanged: called.");
                    if(thumbnail != null){ // show the old thumbnail
                        thumbnail.setVisibility(VISIBLE);
                    }

                    // There's a special case when the end of the list has been reached.
                    // Need to handle that with this bit of logic
                    // top one will be played
                    if(!recyclerView.canScrollVertically(1)){
                        playVideo(true);
                    }
                    else{
                        playVideo(false);
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        // this listener is to listen the moment when the added new list item to the recycler view
        addOnChildAttachStateChangeListener(new OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {

            }

            // callback triggered when the view detached from the recycler view
            // called when one of view on the recycler view is removed
            // the view is removed when it is off the screen
            @Override
            public void onChildViewDetachedFromWindow(View view) {
                if (viewHolderParent != null && viewHolderParent.equals(view)) {
                    // removes old video view from the list item where playing videos
                    // dynamically removing the video view
                    resetVideoView();
                }

            }
        });
        // adding listener to a video player,
        videoPlayer.addListener(new Player.EventListener() {

            // callback to receive different kinds of state changing on video view
            @Override
            public void onPlaybackStateChanged(int state) {
                switch (state) {
                    case Player.STATE_BUFFERING:
                        Log.e(TAG, "onPlayerStateChanged: Buffering video.");
                        if (progressBar != null) {
                            progressBar.setVisibility(VISIBLE);
                        }
                        break;
                    case Player.STATE_ENDED:
                        Log.d(TAG, "onPlayerStateChanged: Video ended.");
                        videoPlayer.seekTo(0);
                        break;
                    case Player.STATE_IDLE:

                        break;
                    case Player.STATE_READY:
                        Log.e(TAG, "onPlayerStateChanged: Ready to play.");
                        if (progressBar != null) {
                            progressBar.setVisibility(GONE);
                        }
                        if(!isVideoViewAdded){
                            // adding video view to list item
                            addVideoView();
                        }
                        break;
                    default:
                        break;
                }
            }
        });

    } // init()

    // play video, and select most suitable list item to play the view
    public void playVideo(boolean isEndOfList) {

        int targetPosition;

        // this is to determine which video to play , given more than one list item on the screen
        if(!isEndOfList){
            // position of the first visible list item on the list
            int startPosition = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();

            // position of the last visible list item on the list
            int endPosition = ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();

            // if there is more than 2 list-items on the screen, set the difference to be 1
            // we are only interested in top 2 of them
            if (endPosition - startPosition > 1) {
                endPosition = startPosition + 1;
            }

            // something is wrong. return.
            if (startPosition < 0 || endPosition < 0) {
                return;
            }

            // if there is more than 1 list-item on the screen
            if (startPosition != endPosition) {

                // this is to determine how much of the first top list item is cut off by the screen edge
                int startPositionVideoHeight = getVisibleVideoSurfaceHeight(startPosition);

                // this is to determine how much of the second top list item is cut off by the screen edge
                int endPositionVideoHeight = getVisibleVideoSurfaceHeight(endPosition);

                // setting target position on which list item video is being played between 2 top list items
                targetPosition = startPositionVideoHeight > endPositionVideoHeight ? startPosition : endPosition;
            }
            else {
                targetPosition = startPosition;
            }
        }
        else{
            targetPosition = mediaObjects.size() - 1;
        }

        Log.d(TAG, "playVideo: target position: " + targetPosition);

        // video is already playing so return
        if (targetPosition == playPosition) {
            return;
        }

        // set the position of the list-item that is to be played
        playPosition = targetPosition;
        if (videoSurfaceView == null) {
            return;
        }

        // remove any old surface views from previously playing videos
        videoSurfaceView.setVisibility(INVISIBLE);
        removeVideoView(videoSurfaceView);

        // getting screen visible list item position, and get viewholder from it
        int currentPosition = targetPosition - ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();

        View child = getChildAt(currentPosition);
        if (child == null) {
            return;
        }

        VideoPlayerRecyclerViewHolder holder = (VideoPlayerRecyclerViewHolder) child.getTag();
        if (holder == null) {
            playPosition = -1;
            return;
        }
        thumbnail = holder.thumbnail;
        progressBar = holder.progressBar;
        volumeControl = holder.volumeControl;
        viewHolderParent = holder.itemView;
        requestManager = holder.requestManager;
        frameLayout = holder.itemView.findViewById(R.id.media_container);

        videoSurfaceView.setPlayer(videoPlayer);

        viewHolderParent.setOnClickListener(videoViewClickListener);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(
                context, Util.getUserAgent(context, "RecyclerView VideoPlayer"));
        String mediaUrl = mediaObjects.get(targetPosition).getMedia_url();
        if (mediaUrl != null) {
            MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(mediaUrl));
            videoPlayer.setMediaSource(videoSource);
            videoPlayer.prepare();
            videoPlayer.setPlayWhenReady(true);
        }
    }

    private OnClickListener videoViewClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleVolume();
        }
    };

    /**
     * Returns the visible region of the video surface on the screen.
     * if some is cut off, it will return less than the @videoSurfaceDefaultHeight
     * @param playPosition
     * @return
     */
    private int getVisibleVideoSurfaceHeight(int playPosition) {
        int at = playPosition - ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
        Log.d(TAG, "getVisibleVideoSurfaceHeight: at: " + at);

        View child = getChildAt(at);
        if (child == null) {
            return 0;
        }

        int[] location = new int[2];
        child.getLocationInWindow(location);

        if (location[1] < 0) {
            return location[1] + videoSurfaceDefaultHeight;
        } else {
            return screenDefaultHeight - location[1];
        }
    }


    // Remove the old player
    private void removeVideoView(PlayerView videoView) {
        ViewGroup parent = (ViewGroup) videoView.getParent();
        if (parent == null) {
            return;
        }

        int index = parent.indexOfChild(videoView);
        if (index >= 0) {
            parent.removeViewAt(index);
            isVideoViewAdded = false;
            viewHolderParent.setOnClickListener(null);
        }

    }

    // add video view to the list item
    // just opposite of resetVideoView()
    private void addVideoView(){
        frameLayout.addView(videoSurfaceView);
        isVideoViewAdded = true;
        videoSurfaceView.requestFocus();
        videoSurfaceView.setVisibility(VISIBLE);
        videoSurfaceView.setAlpha(1);
        thumbnail.setVisibility(GONE);
    }

    // removing video view from the list item
    private void resetVideoView(){
        if(isVideoViewAdded){
            removeVideoView(videoSurfaceView);
            playPosition = -1;
            videoSurfaceView.setVisibility(INVISIBLE);
            thumbnail.setVisibility(VISIBLE);
        }
    }

    public void releasePlayer() {

        if (videoPlayer != null) {
            videoPlayer.release();
            videoPlayer = null;
        }

        viewHolderParent = null;
    }

    private void toggleVolume() {
        if (videoPlayer != null) {
            if (volumeState == VolumeState.OFF) {
                Log.d(TAG, "togglePlaybackState: enabling volume.");
                setVolumeControl(VolumeState.ON);

            } else if(volumeState == VolumeState.ON) {
                Log.d(TAG, "togglePlaybackState: disabling volume.");
                setVolumeControl(VolumeState.OFF);

            }
        }
    }

    private void setVolumeControl(VolumeState state){
        volumeState = state;
        if(state == VolumeState.OFF){
            videoPlayer.setVolume(0f);
            animateVolumeControl();
        }
        else if(state == VolumeState.ON){
            videoPlayer.setVolume(1f);
            animateVolumeControl();
        }
    }

    private void animateVolumeControl(){
        if(volumeControl != null){
            volumeControl.bringToFront();
            if(volumeState == VolumeState.OFF){
                requestManager.load(R.drawable.ic_volume_off_black_24dp)
                        .into(volumeControl);
            }
            else if(volumeState == VolumeState.ON){
                requestManager.load(R.drawable.ic_volume_up_black_24dp)
                        .into(volumeControl);
            }
            volumeControl.animate().cancel();

            volumeControl.setAlpha(1f);

            volumeControl.animate()
                    .alpha(0f)
                    .setDuration(600).setStartDelay(1000);
        }
    }

    public void setMediaObjects(ArrayList<MediaObject> mediaObjects){
        this.mediaObjects = mediaObjects;
    }
}
