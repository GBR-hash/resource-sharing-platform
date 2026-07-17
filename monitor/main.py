import time
from contextlib import asynccontextmanager

import psutil
from fastapi import FastAPI
from fastapi.responses import FileResponse, Response

from camera import camera


@asynccontextmanager
async def lifespan(app: FastAPI):
    camera.start()
    yield
    camera.stop()


app = FastAPI(title='Monitor Dashboard', lifespan=lifespan)


def read_cpu_temp():
    try:
        with open('/sys/class/thermal/thermal_zone0/temp') as f:
            return int(f.read().strip()) / 1000.0
    except Exception:
        return -1.0


def collect_stats():
    cpu = psutil.cpu_percent(interval=None)
    mem = psutil.virtual_memory()
    disk_root = psutil.disk_usage('/')
    disk_nvme = psutil.disk_usage('/mnt/nvme')
    return {
        'cpu_percent': round(cpu, 1),
        'cpu_temp': round(read_cpu_temp(), 1),
        'memory_percent': round(mem.percent, 1),
        'memory_used_gb': round(mem.used / (1024 ** 3), 2),
        'memory_total_gb': round(mem.total / (1024 ** 3), 2),
        'disk_root_percent': round(disk_root.percent, 1),
        'disk_nvme_percent': round(disk_nvme.percent, 1),
        'uptime_seconds': int(time.time() - psutil.boot_time()),
    }


@app.get('/')
async def index():
    return FileResponse('static/index.html')


@app.get('/api/stats')
async def api_stats():
    return collect_stats()


@app.get('/frame.jpg')
async def frame_jpg():
    frame = camera.get_latest_frame()
    if frame is None:
        return Response(status_code=204)
    return Response(
        content=frame,
        media_type='image/jpeg',
        headers={
            'Cache-Control': 'no-cache, no-store, must-revalidate',
        },
    )
