> 这是在研究rxJava2源代码时做的总结,以及在做测试的一些例子。
# 一、设计思路
## 传统方式：
> 定义一个方法，调用方法，得到返回值，对返回值做操作。

## reactive：
> - 定义一个处理返回值的方法，但这个方法由框架来调用，即：observer
> - 定义一个观察流程，并返回此观察流程，即：Observable
> - 调用subscribe方法，完成观察流程Observable和observer的关联。此处订阅只处理OnNext一个方法。

```
def myOnNext = { it -> do something useful with it };
// defines, but does not invoke, the Observable
def myObservable = someObservable(itsParameters);
// subscribes the Subscriber to the Observable, and invokes the Observable
myObservable.subscribe(myOnNext);
// go on about my business
```

更进一步：
> observer可以实现以下接口，onNext,onError，onComplete，完成复杂操作。
> - onNext：也叫emit，核心处理逻辑。
> - onError：错误处理。
> - onComplete：所有onNext执行完后执行。
> 其中onNext方法叫emissions item，onError和onComplete叫notifications。onError和onComplete两个方法只调用其中一个。

```
def myOnNext     = { item -> /* do something useful with item */ };
def myError      = { throwable -> /* react sensibly to a failed call */ };
def myComplete   = { /* clean up after the final response */ };
def myObservable = someMethod(itsParameters);
myObservable.subscribe(myOnNext, myError, myComplete);
```

# 二、Hot 和 Cold
## Hot Observable：
创建之后，就开始发射元素，不管是否有订阅者。订阅者订阅以后，可以从当前订阅位置开始消费元素。
## Cold Observable：
创建之后，并不发射元素，只有订阅者之后，才发射元素。每个订阅者可以消费到所有元素。

# 三、reactiveX（reactive extensions）
允许把多个Observable的结果进行各种形式的操作。
## 针对Observable
- create
- Transforming
- Filtering
- Combining

## 针对observer
- Error Handling Operators
- Utility Operators
- Conditional and Boolean Operators
- Mathematical and Aggregate Operators
- Converting Observables
- Connectable Observable Operators
- Backpressure Operators

## Single
处理单个元素，而不是处理一系列元素。因为只处理一个元素，所以只需要实现OnSucess和OnError方法即可。

## Subject
同时实现了Observer接口和Observable接口。
## Scheduler
在不同的线程环境中执行operator和observer。默认情况下，所有的operator和observer都在执行ubscribe方法的线程里面运行。

# 四、RxJava2 基础类
- [`io.reactivex.Flowable`](http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Flowable.html): 0..N flows, supporting Reactive-Streams and backpressure 有流速控制
 - [`io.reactivex.Observable`](http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html): 0..N flows, no backpressure, 无流速控制
  - [`io.reactivex.Single`](http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Single.html): a flow of exactly 1 item or an error,只处理一个实例，上面两个可以处理集合类。
  - [`io.reactivex.Completable`](http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Completable.html): a flow without items but only a completion or error signal,不处理数据，只做一个延迟的操作。
  - [`io.reactivex.Maybe`](http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Maybe.html): a flow with no items, exactly one item or an error.  onSuccess,onError,onComplete三选一。

## 设计思路：
- rxJava2都采用链式调用，实现的思路就是在进行某个方法调用时，把当前类当作参数传进去，返回一个新的类（此类与前面的类属于同一个基类或者实现同一个接口），形成链式调用。
- Observable实现ObservableSource接口，最主要的方法为 void subscribe(@NonNull Observer<? super T> observer)。在此方法中，实现元素的发射与对应的消费。
- Flowable实现Publisher接口，最主要的方法为void subscribe(Subscriber<? super T> s);
- subscribe方法中，有两类操作，一类是通过emitter发射元素，另外一类是调用Observer或者Subscriber消费元素。
- 不同的变换，都是返回一个新的类，在新类里面，如果是对Observer或者Subscriber做变换，就封装一个新的Observer或者Subscriber进行调用，最后都是链式调用subscribe方法完成对应的变换。

## Flowable 背压模式：
- BUFFER：实现采用rxjava自定义的队列实现。队列没有长度限制，根据内存来看。默认的长度为128，如果队列满了，则自动扩容，不会阻塞，直到把内存撑满。
- MISSING：生产多少，消费多少，不做任何限制，跟Observable一样。
- ERROR：如果超过观察者指定的消费个数，则报MissingBackpressureException，发射照旧。
- DROP：超过观察者指定的消费个数，剩余直接丢掉不处理，发射照旧。
- LATEST：超过后，只把最新的一个值保存在队列里面，剩下的丢弃。

在flowable中，除了在创建的时候设置背压模式外，还可以通过调用 onBackpressureXXXX的系列方法，设置对应的背压模式，适用于利用from方法等创建的flowable。

通过onBackpressureXXXX的系列方法设置的，采用ArrayDeque双向队列实现，必须指定对应的队列长度。

## 线程切换：
- 其中有三种类型的线程：创建Flowable或者Observable的线程（或者叫建立订阅关系的线程）；发射emit元素的线程；消费subcribe元素的线程。
- observeOn：指定消费元素的线程，可以给不同观察者指定线程消费元素，在subscribe方法调用之前最近指定的为本次消费的线程。
- subscribeOn：指定发射元素的线程，但由于发射只进行一次，所以，可以指定多次，但只有最后一次指定的起作用。
- doOnSubscribe：如果想在订阅前在不同线程里面执行一些控制流程，可以采用此方法，并指定SubscribeOn的线程，则此方法会在指定的线程运行。

## 中途取消
- Observable：在调用订阅方式时，返回一个Disposable对象，可以随时调用此对象的dispose方法结束消费，也可以通过在onSubscribe方法中，调用Disposable的dispose方法取消。
- Flowable：可以同上面一样取消。同时，由于在订阅时，也可以通过Subscription指定流速或者直接取消订阅。


# 五、其他知识点
- 队列合并使用的是System.arraycopy(a, 0, b, 0, n);方法
- Action：调用方法无返回值。
- Func：调用方法并有返回值。
- 发布者有三类事件，next,error,complete，订阅者也需要处理三类事件。
- rx.ring-buffer.size 默认128，安卓默认16，这是线程执行时的队列长度。