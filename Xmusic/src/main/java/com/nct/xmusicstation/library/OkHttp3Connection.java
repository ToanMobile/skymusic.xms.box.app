package com.nct.xmusicstation.library;

import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.util.FileDownloadHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttp3Connection implements FileDownloadConnection {

    private final OkHttpClient mClient;
    private final Request.Builder mRequestBuilder;

    private Request mRequest;
    private Response mResponse;
    private ResponseBody body;

    private OkHttp3Connection(Request.Builder builder, OkHttpClient client) {
        mRequestBuilder = builder;
        mClient = client;
    }

    private OkHttp3Connection(String url, OkHttpClient client) {
        this(new Request.Builder().url(url), client);
    }

    @Override
    public void addHeader(String name, String value) {
        mRequestBuilder.addHeader(name, value);
    }

    @Override
    public boolean dispatchAddResumeOffset(String etag, long offset) {
        return false;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (mResponse == null) throw new IOException("Please invoke #execute first!");
        body = mResponse.body();
        if (body == null) throw new IOException("No body found on response!");
        return body.byteStream();
    }

    @Override
    public Map<String, List<String>> getRequestHeaderFields() {
        if (mRequest == null) {
            mRequest = mRequestBuilder.build();
        }

        return mRequest.headers().toMultimap();
    }

    @Override
    public Map<String, List<String>> getResponseHeaderFields() {
        return mResponse == null ? null : mResponse.headers().toMultimap();
    }

    @Override
    public String getResponseHeaderField(String name) {
        return mResponse == null ? null : mResponse.header(name);
    }

    @Override
    public boolean setRequestMethod(String method) throws ProtocolException {
        mRequestBuilder.method(method, null);
        return true;
    }

    @Override
    public void execute() throws IOException {
        if (mRequest == null) {
            mRequest = mRequestBuilder.build();
        }

        mResponse = mClient.newCall(mRequest).execute();
    }

    @Override
    public int getResponseCode() throws IOException {
        if (mResponse == null) throw new IllegalStateException("Please invoke #execute first!");

        return mResponse.code();
    }

    @Override
    public void ending() {
        if (body != null) {
            body.close();
        }
        mRequest = null;
        mResponse = null;
    }

    /**
     * The creator for the connection implemented with the okhttp3.
     */
    public static class Creator implements FileDownloadHelper.ConnectionCreator {

        private OkHttpClient mClient;

        @Override
        public FileDownloadConnection create(String url) throws IOException {
            if (mClient == null) {
                synchronized (Creator.class) {
                    mClient = UnsafeOkHttpClient.getUnsafeOkHttpClient().newBuilder()
                            .connectTimeout(2, TimeUnit.MINUTES)
                            .readTimeout(2, TimeUnit.MINUTES)
                            .writeTimeout(2, TimeUnit.MINUTES)
                            .addNetworkInterceptor(chain -> {
                                Request request = chain.request().newBuilder().addHeader("Connection", "close").build();
                                return chain.proceed(request);
                            }).build();
                }
            }
            return new OkHttp3Connection(url, mClient);
        }
    }
}