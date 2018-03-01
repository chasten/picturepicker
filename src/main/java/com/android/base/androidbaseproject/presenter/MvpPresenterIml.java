package com.android.base.androidbaseproject.presenter;


import com.android.base.androidbaseproject.data.BaseData;
import com.android.base.androidbaseproject.exception.RetrofitHttpException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * mvp base Presenter
 * Created by secretqi on 2018/2/28.
 */

public abstract class MvpPresenterIml<V> implements IPresenter<V> {

    public V mvpView;
    private CompositeDisposable mCompositeDisposable;

    @Override
    public void attachView(V mvpView) {
        this.mvpView = mvpView;
    }

    @Override
    public void detachView() {
        this.mvpView = null;
        this.onUnsubscribe();
    }

    /**
     * RXjava取消注册，以避免内存泄露
     */
    private void onUnsubscribe() {
        if (null != this.mCompositeDisposable) {
            this.mCompositeDisposable.clear();
        }
    }

    public <T> void addSubscription(final Observable<BaseData<T>> observable, final RetrofitResponse<T> response) {
        if (null == this.mCompositeDisposable) {
            this.mCompositeDisposable = new CompositeDisposable();
        }
        Consumer<T> consumer = response::accept;
        Consumer<Throwable> throwableConsumer = throwable -> {
            RetrofitHttpException.ResponseThrowable responseThrowable = RetrofitHttpException.retrofitException(throwable);
            response.responseMessage(responseThrowable.code, responseThrowable.message);
        };
        this.mCompositeDisposable.add(observable
                .map(new HttpResultFunc<>())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer, throwableConsumer));

    }

    /**
     * 用来统一处理Http的resultCode,并将HttpResult的Data部分剥离出来返回给subscriber
     * @param <T> Subscriber真正需要的数据类型，也就是Data部分的数据类型
     */
    private class HttpResultFunc<T> implements Function<BaseData<T>, T> {

        @Override
        public T apply(BaseData<T> tBaseData) {
            if (tBaseData.getStatusCode() != 200) {
                throw new RuntimeException(tBaseData.getMessage());
            }
            if (null == tBaseData.getContent()) {
                return (T) tBaseData;
            }
            return tBaseData.getContent();
        }
    }

    /**
     *
     * @param <T>
     */
    public interface RetrofitResponse<T> {

        void accept(T t);

        void responseMessage(int code, String message);
    }

}