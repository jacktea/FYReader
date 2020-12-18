package xyz.fycz.myreader.ui.presenter;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import xyz.fycz.myreader.R;
import xyz.fycz.myreader.application.MyApplication;
import xyz.fycz.myreader.base.BasePresenter;
import xyz.fycz.myreader.util.llog.LLog;
import xyz.fycz.myreader.webapi.callback.ResultCallback;
import xyz.fycz.myreader.common.APPCONST;
import xyz.fycz.myreader.ui.activity.CatalogActivity;
import xyz.fycz.myreader.util.ToastUtils;
import xyz.fycz.myreader.webapi.crawler.ReadCrawlerUtil;
import xyz.fycz.myreader.greendao.entity.Book;
import xyz.fycz.myreader.greendao.entity.Chapter;
import xyz.fycz.myreader.greendao.service.ChapterService;
import xyz.fycz.myreader.ui.adapter.ChapterTitleAdapter;
import xyz.fycz.myreader.ui.fragment.CatalogFragment;
import xyz.fycz.myreader.webapi.CommonApi;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author fengyue
 * @date 2020/7/22 9:14
 */
public class CatalogPresenter implements BasePresenter {
    private static final String TAG = CatalogPresenter.class.getSimpleName();
    private CatalogFragment mCatalogFragment;
    private ChapterService mChapterService;
    private ArrayList<Chapter> mChapters = new ArrayList<>();
    private ArrayList<Chapter> mConvertChapters = new ArrayList<>();
    private int curSortflag = 0; //0正序  1倒序
    private ChapterTitleAdapter mChapterTitleAdapter;
    private Book mBook;

    public CatalogPresenter(CatalogFragment mCatalogFragment) {
        this.mCatalogFragment = mCatalogFragment;
        mChapterService = ChapterService.getInstance();
    }

    @Override
    public void start() {
        mBook = ((CatalogActivity) mCatalogFragment.getActivity()).getmBook();
        mCatalogFragment.getFcChangeSort().setOnClickListener(view -> {
            if (curSortflag == 0) {//当前正序
                curSortflag = 1;
            } else {//当前倒序
                curSortflag = 0;
            }
            if (mChapterTitleAdapter != null) {
                changeChapterSort();
            }
        });
        mChapters = (ArrayList<Chapter>) mChapterService.findBookAllChapterByBookId(mBook.getId());
        if (mChapters.size() != 0) {
            initChapterTitleList();
        }else {
            if ("本地书籍".equals(mBook.getType())){
                ToastUtils.showWarring("本地书籍请先拆分章节！");
                return;
            }
            mCatalogFragment.getPbLoading().setVisibility(View.VISIBLE);
            CommonApi.getBookChapters(mBook.getChapterUrl(), ReadCrawlerUtil.getReadCrawler(mBook.getSource()),false,
                    new ResultCallback() {
                        @Override
                        public void onFinish(Object o, int code) {
                            mChapters = (ArrayList<Chapter>) o;

                            MyApplication.runOnUiThread(() -> {
                                mCatalogFragment.getPbLoading().setVisibility(View.GONE);
                                initChapterTitleList();
                            });

                        }

                        @Override
                        public void onError(Exception e) {
                            e.printStackTrace();
                            ToastUtils.showError("章节目录加载失败！\n" + e.getLocalizedMessage());
                            MyApplication.runOnUiThread(() -> mCatalogFragment.getPbLoading().setVisibility(View.GONE));
                        }
                    });
        }
        mCatalogFragment.getLvChapterList().setOnItemClickListener((adapterView, view, i, l) -> {
            Chapter chapter = mChapterTitleAdapter.getItem(i);
            final int position;
            assert chapter != null;
            if (chapter.getNumber() == 0) {
                if (curSortflag == 0) {
                    position = i;
                } else {
                    position = mChapters.size() - 1 - i;
                }
            } else {
                position = chapter.getNumber();
            }
            /*LLog.i(TAG, "position = " + position);
            LLog.i(TAG, "mChapters.size() = " + mChapters.size());*/
            Intent intent = new Intent();
            intent.putExtra(APPCONST.CHAPTER_PAGE, new int[]{position, 0});
            mCatalogFragment.getActivity().setResult(Activity.RESULT_OK, intent);
            mCatalogFragment.getActivity().finish();
        });
    }


    /**
     * 初始化章节目录
     */
    private void initChapterTitleList() {
        //初始化倒序章节
        mConvertChapters.addAll(mChapters);
        Collections.reverse(mConvertChapters);
        //设置布局管理器
        int curChapterPosition;
        curChapterPosition = mBook.getHisttoryChapterNum();
        mChapterTitleAdapter = new ChapterTitleAdapter(mCatalogFragment.getContext(), R.layout.listview_chapter_title_item, mChapters, mBook);
        mCatalogFragment.getLvChapterList().setAdapter(mChapterTitleAdapter);
        mCatalogFragment.getLvChapterList().setSelection(curChapterPosition);
    }

    /**
     * 改变章节列表排序（正倒序）
     */
    private void changeChapterSort() {
        if (curSortflag == 0) {
            mChapterTitleAdapter.clear();
            mChapterTitleAdapter.addAll(mChapterTitleAdapter.getmList());
        } else {
            mChapterTitleAdapter.clear();
            mConvertChapters.clear();
            mConvertChapters.addAll(mChapterTitleAdapter.getmList());
            Collections.reverse(mConvertChapters);
            mChapterTitleAdapter.addAll(mConvertChapters);
        }
        mChapterTitleAdapter.notifyDataSetChanged();
        mCatalogFragment.getLvChapterList().setAdapter(mChapterTitleAdapter);
    }

    /**
     * 搜索过滤
     *
     * @param query
     */
    public void startSearch(String query) {
        if (mChapters.size() == 0)  return;
        mChapterTitleAdapter.getFilter().filter(query);
        mCatalogFragment.getLvChapterList().setSelection(0);
    }
}
