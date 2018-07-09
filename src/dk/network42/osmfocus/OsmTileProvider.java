package dk.network42.osmfocus;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.util.Log;

public class OsmTileProvider {
	static private final String TAG = "OsmTileProvider";

	static final int TASK_DOWNLOAD_START    = 1;
	static final int TASK_DOWNLOAD_FAILED   = 2;
	static final int TASK_DOWNLOAD_COMPLETE = 3;

	static final int MAX_THREADS = 4;

	final int KEEP_ALIVE_TIME = 1;
	final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

	private BlockingQueue<Runnable>  mWorkQueue;
	private ThreadPoolExecutor       mTrdPool;
	private Queue<OsmTileDownloader> mTaskQueue;
	private String mUserAgent;
	
	OsmTileProvider(String agent) {
		this(agent, Math.min(Runtime.getRuntime().availableProcessors(), MAX_THREADS));
	}

	OsmTileProvider(String agent, int maxTrds) {
		mUserAgent = new String(agent);
		final int trds = Math.min(maxTrds, Runtime.getRuntime().availableProcessors());
		//Log.d(TAG, "Threads="+trds);
		mWorkQueue = new LinkedBlockingQueue<Runnable>();
		mTaskQueue = new LinkedBlockingQueue<OsmTileDownloader>();
		mTrdPool = new ThreadPoolExecutor(trds, trds, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mWorkQueue);
	}

	public int getActiveDownloads() {
		return mTrdPool.getActiveCount();
	}

	public void clearPending() {
		synchronized (this) {
			//Log.d(TAG, "clearPending start size="+mWorkQueue.size());
			OsmTileDownloader[] pendingArray = new OsmTileDownloader[mWorkQueue.size()];
			mWorkQueue.toArray(pendingArray);
			int len = pendingArray.length;
			//Log.d(TAG, "ArrayLen="+len);
			for (int ii = 0; ii < len; ii++) {
				//Log.d(TAG, "dlTask="+pendingArray[ii]);
				Thread trd = pendingArray[ii].mThread;
				if (trd != null) {
					trd.interrupt();
				}
			}
		}
		//Log.d(TAG, "clearPending done");
	}

	public void cancelDownload(OsmTile tile) {
		synchronized (this) {
			OsmTileDownloader[] pendingArray = new OsmTileDownloader[mWorkQueue.size()];
			mWorkQueue.toArray(pendingArray);
			int len = pendingArray.length;
			for (int ii = 0; ii < len; ii++) {
				if (pendingArray[ii].mTile == tile) {
					Thread trd = pendingArray[ii].mThread;
					if (trd != null) {
						trd.interrupt();
					}
				}
			}
		}
	}

	public OsmTileDownloader downloadTile(final String providerArg, OsmTile tile, Handler h) {

		tile.setQueuedForDownload();
		OsmTileDownloader task = mTaskQueue.poll();
		if (task == null) {
			task = this.new OsmTileDownloader();
		}
		task.mProv = this;
		task.mProviderArg = providerArg;
		task.mTile = tile;
		task.mHandler = h;
		mTrdPool.execute(task);
		return task;
	}

	protected class OsmTileDownloader implements Runnable {
		OsmTileProvider mProv;
		String mProviderArg;
		OsmTile mTile;
		Thread mThread;
		Handler mHandler;
		public void run() {
			mThread = Thread.currentThread();
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
			//Log.d(TAG, "Task started, tile="+mTile);
			mHandler.obtainMessage(TASK_DOWNLOAD_START, mTile).sendToTarget();
			if (!mTile.download(mUserAgent, mProviderArg)) {
				mTile.mDownloadErrs++;
			}
			// Mark done before new task starts - see OsmTileProvider.cancelDownload()
			synchronized (mProv) {
				mThread = null;
			}
			mTile.clearQueuedForDownload();
			//Log.d(TAG, "Task ended, tile="+mTile);
			//Log.d(TAG, "Sending TASK_DOWNLOAD_COMPLETE");
			mHandler.obtainMessage(TASK_DOWNLOAD_COMPLETE, mTile).sendToTarget();
		}
	}
}
