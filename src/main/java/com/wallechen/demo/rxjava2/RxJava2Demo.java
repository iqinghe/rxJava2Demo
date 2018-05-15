package com.wallechen.demo.rxjava2;

import java.util.Arrays;
import java.util.List;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.*;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by wallechen on 18/5/3.
 */
public class RxJava2Demo {

	public static void main(String[] args) {
		//		flowable();
		//		observable();
		flat();
	}

	public static void flowable() {
		Flowable<String> flowable = Flowable.create(new FlowableOnSubscribe<String>() {
			@Override
			public void subscribe(@NonNull FlowableEmitter<String> emitter) throws Exception {
				System.out.println("emitter thread::::" + Thread.currentThread().getName());
				System.out.println("request::::" + emitter.requested());
				//Thread.currentThread().sleep(100000000);
				for (int i = 0; i < 150; i++) {
					System.out.println("发射::::" + i);
					emitter.onNext(String.valueOf(i));
				}
			}
		}, BackpressureStrategy.LATEST);
		//				.subscribeOn(Schedulers.computation()).skip(10)
		//				.subscribeOn(Schedulers.newThread());
		//		flowable.observeOn(Schedulers.newThread()).subscribe(new Subscriber<String>() {
		flowable.subscribe(new Subscriber<String>() {
			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(10);
			}

			@Override
			public void onNext(String s) {
				System.out.println("observer thread::::" + Thread.currentThread().getName());
				System.out.println("next::::" + s);
				/*try {
					Thread.currentThread().sleep(10000000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}*/
			}

			@Override
			public void onError(Throwable throwable) {
				System.out.println("异常信息为::::" + throwable);
			}

			@Override
			public void onComplete() {
				System.out.println("消费完成!!!");
			}
		});
		System.out.println("subscribe thread::::" + Thread.currentThread().getName());
	}

	public static void observable() {
		Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(@NonNull ObservableEmitter<String> observableEmitter) throws Exception {
				observableEmitter.onNext("a");
				observableEmitter.onNext("b");
				System.out.println("emitter thread::::" + Thread.currentThread().getName());
				//throw new NullPointerException("空");
				//observableEmitter.onError(new NullPointerException("测试!"));
				//				observableEmitter.onComplete();
				//throw new NullPointerException("test");
				//Function
			}
		})
		/*.subscribeOn(Schedulers.computation()).doOnSubscribe((t) -> {
			System.out.println("t");
			System.out.println("doOnSubscribe thread::::" + Thread.currentThread().getName());
		}).subscribeOn(Schedulers.newThread());*/
				//				.subscribeOn(Schedulers.computation()).subscribeOn(Schedulers.newThread()).subscribeOn(Schedulers.newThread());
				.subscribeOn(Schedulers.computation());
		//		observable.skip(1);
		observable.observeOn(Schedulers.computation()).skip(1).observeOn(Schedulers.newThread()).subscribe(onNext -> {
			System.out.println("初始消费::::" + onNext);
			System.out.println("observer thread::::" + Thread.currentThread().getName());
		}, onComplete -> {
			System.out.println("error!!!");
		}, () -> {
			System.out.println("complete!!!");
		});
		System.out.println("subscribe thread::::" + Thread.currentThread().getName());
		//		observable.observeOn(Schedulers.newThread()).subscribe(new Observer<String>() {
		//			@Override
		//			public void onSubscribe(@NonNull Disposable disposable) {
		//				//disposable.dispose();
		//			}
		//
		//			@Override
		//			public void onNext(@NonNull String s) {
		//				System.out.println("second subcribe::::" + s);
		//			}
		//
		//			@Override
		//			public void onError(@NonNull Throwable throwable) {
		//				System.out.println("second error!!!");
		//			}
		//
		//			@Override
		//			public void onComplete() {
		//				System.out.println("mission complete!!!");
		//			}
		//		});
		//		observable.onErrorResumeNext((Throwable throwable) -> {
		//			return new ObservableSource<String>() {
		//				@Override
		//				public void subscribe(@NonNull Observer<? super String> observer) {
		//					observer.onNext(throwable.toString());
		//				}
		//			};
		//		}).observeOn(Schedulers.newThread()).subscribe(onNext -> {
		//			System.out.println("报错后消费::::" + onNext);
		//		});
	}

	/**
	 * 通过指定observeOn,在不同线程中执行不同的操作,最后统一消费
	 */
	public static void flat() {
		List<String> words = Arrays.asList("the", "quick", "brown", "fox", "jumped", "over", "the", "lazy", "dog");
		Observable.fromIterable(words).observeOn(Schedulers.newThread()).flatMap(word -> {
			System.out.println("flatMap thread::::" + Thread.currentThread().getName());
			return Observable.fromArray(word.split(""));
		}).distinct().sorted().observeOn(Schedulers.newThread())
				.zipWith(Observable.range(1, Integer.MAX_VALUE), (string, count) -> {
					System.out.println("zip thread::::" + Thread.currentThread().getName());
					return String.format("%2d. %s", count, string);
				}).subscribe(System.out::println);
	}
}
