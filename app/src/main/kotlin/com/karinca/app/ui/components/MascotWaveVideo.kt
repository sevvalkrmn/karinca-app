package com.karinca.app.ui.components

import android.net.Uri
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout

// Plays a short, silent, looping clip (the mascot waving/talking) as a decorative
// background — no controls, no audio, no interaction. Uses a plain TextureView wrapped in
// AspectRatioFrameLayout (for the same crop-to-fill behavior media3-ui's PlayerView gives
// you) rather than PlayerView itself, which defaults to a SurfaceView: SurfaceView renders
// on its own compositor layer and reliably shows an opaque black frame for a beat before
// the first decoded frame attaches. TextureView composites like a normal view, so there's
// nothing to flash — whatever's behind it (the caller's circle Surface) just shows through
// until the first frame is ready.
@Composable
fun MascotWaveVideo(rawResId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/$rawResId")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }
    // Holds the TextureView the factory below creates, so it can be explicitly detached from
    // the player before release() — releasing while a TextureView is still attached can make
    // ExoPlayer block for a noticeable moment waiting on the surface teardown, which read as
    // the app hanging for a second or two right when navigating back off this screen.
    val textureViewHolder = remember { arrayOfNulls<TextureView>(1) }

    DisposableEffect(Unit) {
        onDispose {
            textureViewHolder[0]?.let { exoPlayer.clearVideoTextureView(it) }
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val frame = AspectRatioFrameLayout(ctx).apply {
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
            }
            val textureView = TextureView(ctx)
            textureViewHolder[0] = textureView
            frame.addView(
                textureView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            exoPlayer.setVideoTextureView(textureView)
            exoPlayer.addListener(
                object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        val ratio = if (videoSize.height == 0) {
                            0f
                        } else {
                            videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                        }
                        frame.setAspectRatio(ratio)
                    }
                },
            )
            frame
        },
    )
}
