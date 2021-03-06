package info.chitanka.app.mvp.presenters.book;

import java.lang.ref.WeakReference;

import info.chitanka.app.api.ChitankaApi;
import info.chitanka.app.mvp.presenters.BasePresenter;
import info.chitanka.app.mvp.views.BookView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by nmp on 16-3-24.
 */
public class BookPresenterImpl extends BasePresenter<BookView> implements BookPresenter {

    ChitankaApi chitankaApi;
    private Subscription subscription;

    public BookPresenterImpl(ChitankaApi chitankaApi) {
        this.chitankaApi = chitankaApi;
    }

    @Override
    public void loadBooksDetails(int id) {
        subscription = chitankaApi.getBookDetails(id)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(book -> {
                    if(viewExists())
                        getView().presentBookDetails(book);
                }, err -> {
                    Timber.e(err, "Error with loading book!");
                    if (!viewExists())
                        return;

                    getView().showError();
                });
    }

    @Override
    public void startPresenting() {

    }

    @Override
    public void stopPresenting() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void setView(BookView view) {
        this.view = new WeakReference<BookView>(view);
    }
}
