package top.saymzx.easycontrol.app.client;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Pair;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecode {

  private MediaCodec videoDecodec;
  private final ByteBuffer csd0;
  private final ByteBuffer csd1;
  private final Pair<Integer, Integer> videoSize;

  public VideoDecode(Pair<Integer, Integer> videoSize, ByteBuffer csd0, ByteBuffer csd1) throws IOException {
    this.videoSize = videoSize;
    this.csd0 = csd0;
    this.csd1 = csd1;
  }

  public void release() {
    try {
      videoDecodec.stop();
      videoDecodec.release();
    } catch (Exception ignored) {
    }
  }

  public synchronized void decodeIn(ByteBuffer data) {
    try {
      int inIndex = videoDecodec.dequeueInputBuffer(0);
      // 缓冲区已满丢帧
      if (inIndex < 0) return;
      int size = data.remaining();
      videoDecodec.getInputBuffer(inIndex).put(data);
      // 提交解码器解码
      videoDecodec.queueInputBuffer(inIndex, 0, size, 0, 0);
    } catch (IllegalStateException ignored) {
    }
  }

  private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  public void decodeOut(boolean isNormalPlay) {
    try {
      int outIndex = videoDecodec.dequeueOutputBuffer(bufferInfo, -1);
      if (outIndex >= 0) videoDecodec.releaseOutputBuffer(outIndex, isNormalPlay);
    } catch (IllegalStateException ignored) {
    }
  }

  // 创建Codec
  public void setVideoDecodec(Surface surface) throws IOException {
    // 创建解码器
    String codecMime = MediaFormat.MIMETYPE_VIDEO_AVC;
    videoDecodec = MediaCodec.createDecoderByType(codecMime);
    MediaFormat mediaFormat = MediaFormat.createVideoFormat(codecMime, videoSize.first, videoSize.second);
    // 获取视频标识头
    mediaFormat.setByteBuffer("csd-0", csd0);
    mediaFormat.setByteBuffer("csd-1", csd1);
    // 配置解码器
    videoDecodec.configure(mediaFormat, surface, null, 0);
    // 启动解码器
    videoDecodec.start();
    // 解析首帧，解决开始黑屏问题
    decodeIn(csd0);
    decodeIn(csd1);
  }

}