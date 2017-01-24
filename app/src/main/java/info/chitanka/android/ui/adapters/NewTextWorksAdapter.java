package info.chitanka.android.ui.adapters;

import android.content.res.Resources;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.internal.LinkedTreeMap;

import java.util.List;

import butterknife.Bind;
import info.chitanka.android.R;
import info.chitanka.android.mvp.models.NewTextWorksResult;
import info.chitanka.android.mvp.models.TextWork;
import info.chitanka.android.ui.dialogs.DownloadDialog;
import info.chitanka.android.utils.DateUtils;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by nmp on 23.01.17.
 */

public class NewTextWorksAdapter extends AdvancedSectionedRecyclerViewAdapter<NewTextWorksAdapter.SectionViewHolder, TextWorksAdapter.ViewHolder> {

    private final Resources res;
    private final FragmentManager fragmentManager;
    LinkedTreeMap<String, List<NewTextWorksResult>> map;
    private PublishSubject<TextWork> onWebClick = PublishSubject.create();

    public NewTextWorksAdapter(LinkedTreeMap<String, List<NewTextWorksResult>> map, Resources res, FragmentManager fragmentManager) {
        this.map = map;
        this.res = res;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public int getGroupCount() {
        return map.keySet().size();
    }

    @Override
    public int getChildCount(int group) {
        List<NewTextWorksResult> textWorks = getTextWorks(group);
        return textWorks != null ? textWorks.size() : 0;
    }

    private List<NewTextWorksResult> getTextWorks(int keyPosition) {
        int i = 0;
        for (String key : map.keySet()) {
            if (i == keyPosition) {
                return map.get(key);
            }
            i++;
        }

        return null;
    }

    private String getDate(int keyPosition) {
        int i = 0;
        for (String key : map.keySet()) {
            if (i == keyPosition) {
                return DateUtils.getReadableDateWithTime(key, "yyyy-MM-dd");
            }
            i++;
        }

        return "";
    }

    @Override
    public SectionViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_date_header, parent, false);
        return new SectionViewHolder(view);
    }

    @Override
    public TextWorksAdapter.ViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_textwork, parent, false);

        return new TextWorksAdapter.ViewHolder(view);
    }

    @Override
    public void onBindSectionViewHolder(SectionViewHolder holder, int position, List<Object> payloads) {
        holder.tvHeader.setText(getDate(position));
    }

    public Observable<TextWork> getOnWebClick() {
        return onWebClick.asObservable();
    }

    @Override
    public void onBindChildViewHolder(TextWorksAdapter.ViewHolder holder, int belongingGroup, int position, List<Object> payloads) {
        NewTextWorksResult newTextWorksResults = getTextWorks(belongingGroup).get(position);
        TextWork textWork = newTextWorksResults.getText();
        holder.bind(textWork, res);

        holder.tvDownload.setOnClickListener(view1 -> {
            DownloadDialog
                    .newInstance(textWork.getTitle(), textWork.getDownloadUrl(), textWork.getFormats())
                    .show(fragmentManager, DownloadDialog.TAG);
        });

        holder.tvWeb.setOnClickListener(view1 -> {
            onWebClick.onNext(textWork);
        });
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.tv_header)
        TextView tvHeader;

        public SectionViewHolder(View itemView) {
            super(itemView);
            tvHeader = (TextView) itemView.findViewById(R.id.tv_header);
        }
    }
}