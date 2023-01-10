package com.example.meetlisbon;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.RequestFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * https://gist.github.com/stephenhand/0fdb6425353465875bbe3cd7343c3695
 * Created by stephenh on 25/10/2017.
 * This class permits the use of synchronous blocking HTTP requests in volley. It also makes no use of the main thread, configuring its own looper thread to process responses on
 */

public class SynchronousVolley {


    private Handler responseHandler;
    private final Thread responseThread = new Thread(new Runnable() {
        @Override
        public void run() {
            synchronized (SynchronousVolley.this) {
                Looper.prepare();
                responseHandler = new Handler(Looper.myLooper());
                SynchronousVolley.this.notify();

            }
            Looper.loop();
        }
    });
    private static final String DEFAULT_CACHE_DIR = "volley";

    private final RequestQueue queue;

    /** Number of network request dispatcher threads to start. */
    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 4;
    //Simplified version of Volley.newRequestQueue that permits specifying a custom handler for responses
    //This allows you to run response handler code on threads other than the UI thread
    private static RequestQueue newVolleyRequestQueue(Context context, Handler handler){
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        Network network = new BasicNetwork(new HurlStack());
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network, DEFAULT_NETWORK_THREAD_POOL_SIZE, new ExecutorDelivery(handler));
        queue.start();
        return queue;
    }

    public SynchronousVolley(Context ctx) {
        synchronized (this){
            try {
                responseThread.start();
                //10 seconds should be more than long enough to set up the looper
                SynchronousVolley.this.wait(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (responseHandler == null) {
                throw new IllegalStateException("Failed to set up looper to handle HTTP requests");
            }
        }
        this.queue = newVolleyRequestQueue(ctx, responseHandler);

    }

    public <T> T request(final Request<T> request){
        RequestFuture<T> future = RequestFuture.newFuture();
        future.setRequest(request);
        queue.add(request);
        try {
            return (T)future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}