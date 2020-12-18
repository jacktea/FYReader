package xyz.fycz.myreader.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.ArrayList;

import xyz.fycz.myreader.R;
import xyz.fycz.myreader.application.MyApplication;
import xyz.fycz.myreader.common.APPCONST;
import xyz.fycz.myreader.ui.activity.ReadActivity;
import xyz.fycz.myreader.ui.dialog.DialogCreator;
import xyz.fycz.myreader.ui.dialog.MyAlertDialog;
import xyz.fycz.myreader.greendao.entity.Book;
import xyz.fycz.myreader.ui.activity.BookDetailedActivity;
import xyz.fycz.myreader.ui.presenter.BookcasePresenter;
import xyz.fycz.myreader.util.StringHelper;
import xyz.fycz.myreader.util.ToastUtils;


/**
 * @author fengyue
 * @date 2020/4/19 11:23
 */

public class BookcaseDetailedAdapter extends BookcaseAdapter {
    ViewHolder viewHolder = null;

    public BookcaseDetailedAdapter(Context context, int textViewResourceId, ArrayList<Book> objects,
                                   boolean editState, BookcasePresenter bookcasePresenter, boolean isGroup) {
        super(context, textViewResourceId, objects, editState, bookcasePresenter, isGroup);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null || convertView.getTag() instanceof BookcaseDragAdapter.ViewHolder) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(mResourceId, null);
            viewHolder.cbBookChecked = convertView.findViewById(R.id.cb_book_select);
            viewHolder.ivBookImg = convertView.findViewById(R.id.iv_book_img);
            viewHolder.tvBookName = convertView.findViewById(R.id.tv_book_name);
            viewHolder.tvNoReadNum = convertView.findViewById(R.id.tv_no_read_num);
            viewHolder.tvBookAuthor = convertView.findViewById(R.id.tv_book_author);
            viewHolder.tvHistoryChapter = convertView.findViewById(R.id.tv_book_history_chapter);
            viewHolder.tvNewestChapter = convertView.findViewById(R.id.tv_book_newest_chapter);
            viewHolder.llBookRead = convertView.findViewById(R.id.ll_book_read);
            viewHolder.pbLoading = convertView.findViewById(R.id.pb_loading);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        initView(position);
        return convertView;
    }

    private void initView(int position) {
        final Book book = getItem(position);
        if (StringHelper.isEmpty(book.getImgUrl())) {
            book.setImgUrl("");
        }
        viewHolder.ivBookImg.load(book.getImgUrl(), book.getName(),book.getAuthor());

        viewHolder.tvBookName.setText(book.getName());

        viewHolder.tvBookAuthor.setText(book.getAuthor());
        viewHolder.tvHistoryChapter.setText(book.getHistoryChapterId());
        assert book.getNewestChapterTitle() != null;
        viewHolder.tvNewestChapter.setText(book.getNewestChapterTitle().replace("最近更新 ", ""));

        if (mEditState) {
            viewHolder.tvNoReadNum.setVisibility(View.GONE);
            viewHolder.ivBookImg.setOnClickListener(null);
            viewHolder.llBookRead.setOnClickListener(null);
            viewHolder.pbLoading.setVisibility(View.GONE);
            viewHolder.cbBookChecked.setVisibility(View.VISIBLE);
            viewHolder.cbBookChecked.setChecked(getBookIsChecked(book.getId()));
            viewHolder.llBookRead.setOnClickListener(v -> {
                setCheckedBook(book.getId());
                mListener.onItemCheckedChange(getBookIsChecked(book.getId()));
            });
            viewHolder.ivBookImg.setOnClickListener(v -> {
                setCheckedBook(book.getId());
                mListener.onItemCheckedChange(getBookIsChecked(book.getId()));
            });
            viewHolder.cbBookChecked.setOnClickListener(v -> {
                setCheckedBook(book.getId());
                mListener.onItemCheckedChange(getBookIsChecked(book.getId()));
            });
        } else {
            viewHolder.cbBookChecked.setVisibility(View.GONE);
            boolean isLoading = false;
            try {
                isLoading = isBookLoading(book.getId());
            } catch (Exception ignored) {
            }
            if (isLoading) {
                viewHolder.pbLoading.setVisibility(View.VISIBLE);
                viewHolder.tvNoReadNum.setVisibility(View.GONE);
            } else {
                viewHolder.pbLoading.setVisibility(View.GONE);
                int notReadNum = book.getChapterTotalNum() - book.getHisttoryChapterNum() + book.getNoReadNum() - 1;
                if (notReadNum != 0) {
                    viewHolder.tvNoReadNum.setVisibility(View.VISIBLE);
                    if (book.getNoReadNum() != 0) {
                        viewHolder.tvNoReadNum.setHighlight(true);
                        if (notReadNum == -1) {
                            notReadNum = book.getNoReadNum() - 1;
                        }
                    } else {
                        viewHolder.tvNoReadNum.setHighlight(false);
                    }
                    viewHolder.tvNoReadNum.setBadgeCount(notReadNum);
                } else {
                    viewHolder.tvNoReadNum.setVisibility(View.GONE);
                }
            }
            viewHolder.llBookRead.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, ReadActivity.class);
                intent.putExtra(APPCONST.BOOK, book);
                mBookService.updateEntity(book);
                mContext.startActivity(intent);
            });
            viewHolder.ivBookImg.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, BookDetailedActivity.class);
                intent.putExtra(APPCONST.BOOK, book);
                mContext.startActivity(intent);
            });
            viewHolder.llBookRead.setOnLongClickListener(v -> {
                if (!ismEditState()) {
                    AlertDialog bookDialog = MyAlertDialog.build(mContext)
                            .setTitle(book.getName())
                            .setItems(menu, (dialog, which) -> {
                                switch (which) {
                                    case 0:
                                        if (!isGroup) {
                                            book.setSortCode(0);
                                        }else {
                                            book.setGroupSort(0);
                                        }
                                        mBookService.updateEntity(book);
                                        mBookcasePresenter.init();
                                        ToastUtils.showSuccess("书籍《" + book.getName() + "》移至顶部成功！");
                                        break;
                                    case 1:
                                        downloadBook(book);
                                        break;
                                    case 2:
                                        MyApplication.getApplication().newThread(() -> {
                                            try {
                                                if (unionChapterCathe(book)) {
                                                    DialogCreator.createTipDialog(mContext,
                                                            "缓存导出成功，导出目录："
                                                                    + APPCONST.TXT_BOOK_DIR);
                                                } else {
                                                    DialogCreator.createTipDialog(mContext,
                                                            "章节目录为空或未找到缓存文件，缓存导出失败！");
                                                }
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                                DialogCreator.createTipDialog(mContext,
                                                        "章节目录为空或未找到缓存文件，缓存导出失败！");
                                            }
                                        });
                                        break;
                                    case 3:
                                        showDeleteBookDialog(book);
                                        break;
                                }
                            })
                            .setNegativeButton(null, null)
                            .setPositiveButton(null, null)
                            .create();
                    bookDialog.show();
                    return true;
                }
                return false;
            });
        }
    }


    static class ViewHolder extends BookcaseAdapter.ViewHolder {
        TextView tvBookAuthor;
        TextView tvHistoryChapter;
        TextView tvNewestChapter;
        LinearLayout llBookRead;
    }

}
