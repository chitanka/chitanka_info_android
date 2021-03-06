package info.chitanka.app.ui.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import fr.castorflex.android.circularprogressbar.CircularProgressBar;
import info.chitanka.app.Constants;
import info.chitanka.app.R;
import info.chitanka.app.TrackingConstants;
import info.chitanka.app.components.AnalyticsService;
import info.chitanka.app.di.presenters.PresenterComponent;
import info.chitanka.app.events.SearchBookEvent;
import info.chitanka.app.mvp.models.TextWork;
import info.chitanka.app.mvp.presenters.textworks.TextWorksPresenter;
import info.chitanka.app.mvp.views.TextWorksView;
import info.chitanka.app.ui.adapters.TextWorksAdapter;
import info.chitanka.app.utils.IntentUtils;
import info.chitanka.app.utils.RxBus;

/**
 * Created by nmp on 21.01.17.
 */

public class TextWorksFragment extends BaseFragment implements TextWorksView {
    public static final String TAG = TextWorksFragment.class.getSimpleName();

    @Inject
    TextWorksPresenter presenter;

    @Inject
    RxBus rxBus;

    @Inject
    AnalyticsService analyticsService;

    @BindView(R.id.rv_textworks)
    RecyclerView rvTextWorks;

    @BindView(R.id.container_empty)
    RelativeLayout containerEmpty;

    @BindView(R.id.loading)
    CircularProgressBar loading;

    private String searchTerm;
    private String authorSlug;
    private Unbinder unbinder;

    public static TextWorksFragment newInstance(String searchTerm, String authorSlug) {

        Bundle args = new Bundle();
        args.putString(Constants.EXTRA_SEARCH_TERM, searchTerm);
        args.putString(Constants.EXTRA_SLUG, authorSlug);
        TextWorksFragment fragment = new TextWorksFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        searchTerm = getArgument(Constants.EXTRA_SEARCH_TERM, savedInstanceState);
        authorSlug = getArgument(Constants.EXTRA_SLUG, savedInstanceState);

        getComponent(PresenterComponent.class).inject(this);
        presenter.setView(this);
        presenter.startPresenting();
        if (TextUtils.isEmpty(searchTerm)) {
            presenter.getAuthorTextWorks(authorSlug);
        } else {
            presenter.searchTextWorks(searchTerm);
        }

        rxBus.toObserverable()
                .compose(bindToLifecycle())
                .subscribe((event) -> {
                    if (event instanceof SearchBookEvent) {
                        containerEmpty.setVisibility(View.GONE);
                        rvTextWorks.setVisibility(View.GONE);
                        searchTerm = ((SearchBookEvent) event).getName();
                        presenter.searchTextWorks(searchTerm);
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_text_works, container, false);
        unbinder = ButterKnife.bind(this, view);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            rvTextWorks.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        } else {
            rvTextWorks.setLayoutManager(new GridLayoutManager(getActivity(), 2, LinearLayoutManager.VERTICAL, false));
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Constants.EXTRA_SEARCH_TERM, searchTerm);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unbinder.unbind();
        presenter.stopPresenting();
    }

    @Override
    public String getTitle() {
        return TAG;
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void hideLoading() {
        loading.progressiveStop();
        loading.setVisibility(View.GONE);
    }

    @Override
    public void showLoading() {
        loading.setVisibility(View.VISIBLE);
    }

    @Override
    public void presentTextWorks(List<TextWork> texts) {
        if (texts == null || texts.size() == 0) {
            rvTextWorks.setVisibility(View.GONE);
            containerEmpty.setVisibility(View.VISIBLE);
            return;
        }

        rvTextWorks.setVisibility(View.VISIBLE);
        containerEmpty.setVisibility(View.GONE);
        TextWorksAdapter adapter = new TextWorksAdapter(texts, getActivity());
        adapter.getOnWebClick()
                .compose(bindToLifecycle())
                .subscribe(textwork -> {
                    IntentUtils.openWebUrl(textwork.getChitankaUrl(), getActivity());
                    analyticsService.logEvent(TrackingConstants.CLICK_WEB_TEXTWORKS);
                });
        rvTextWorks.setAdapter(adapter);
    }
}
