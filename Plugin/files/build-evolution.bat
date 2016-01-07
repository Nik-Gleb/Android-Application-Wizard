cd /d %~dp0
gource --load-config gource.cfg ----auto-skip-seconds 1
ffmpeg -y -r 60 -f image2pipe -vcodec ppm -i evolution.ppm -vcodec libx264 -preset ultrafast -pix_fmt yuv420p -crf 8 -threads 0 -bf 0 build\evolution.mp4
del evolution.ppm
PAUSE