import logging
import subprocess
import threading

logger = logging.getLogger(__name__)

SOI = b'\xff\xd8'
EOI = b'\xff\xd9'
DEVICE = '/dev/video1'

FFMPEG_CMD = [
    'ffmpeg',
    '-f', 'v4l2',
    '-input_format', 'mjpeg',
    '-video_size', '320x180',
    '-framerate', '30',
    '-i', DEVICE,
    '-c', 'copy',
    '-f', 'mjpeg',
    '-',
]


class CameraStream:
    def __init__(self):
        self._process = None
        self._thread = None
        self._latest_frame = None
        self._lock = threading.Lock()
        self._running = False

    def start(self):
        logger.info('Starting ffmpeg: %s', ' '.join(FFMPEG_CMD))
        self._process = subprocess.Popen(
            FFMPEG_CMD,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            bufsize=0,
        )
        self._running = True
        self._thread = threading.Thread(target=self._reader, daemon=True)
        self._thread.start()

    def stop(self):
        self._running = False
        if self._thread:
            self._thread.join(timeout=3)
        if self._process:
            logger.info('Stopping ffmpeg')
            self._process.terminate()
            try:
                self._process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                self._process.kill()
            self._process = None

    def get_latest_frame(self):
        with self._lock:
            frame = self._latest_frame
            self._latest_frame = None
            return frame

    def _reader(self):
        buf = bytearray()
        try:
            assert self._process and self._process.stdout
            while self._running:
                chunk = self._process.stdout.read(4096)
                if not chunk:
                    break
                buf.extend(chunk)
                while True:
                    soi = buf.find(SOI)
                    if soi == -1:
                        break
                    eoi = buf.find(EOI, soi + 2)
                    if eoi == -1:
                        break
                    frame = bytes(buf[soi : eoi + len(EOI)])
                    del buf[: eoi + len(EOI)]
                    with self._lock:
                        self._latest_frame = frame
        except Exception:
            logger.exception('Reader thread error')


camera = CameraStream()
