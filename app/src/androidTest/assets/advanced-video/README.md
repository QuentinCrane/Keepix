# Synthetic local video fixture

`synthetic.mp4` is generated locally from FFmpeg test patterns and a sine wave. It
contains no user media or third-party footage. It is packaged only in the test APK,
not in the application APK, and is used for MediaStore and playback lifecycle tests.

Generation command (FFmpeg available on the development machine):

```sh
ffmpeg -f lavfi -i testsrc2=size=320x180:rate=12 -f lavfi -i sine=frequency=440:sample_rate=22050 -t 3 -c:v libx264 -preset ultrafast -pix_fmt yuv420p -c:a aac -b:a 32k synthetic.mp4
```

Tests insert copies only in `Movies/kanleme-advanced/`, track their content URIs,
and remove only those test-owned copies after the test. The encoding tool itself
is not distributed with the app or test APK.
