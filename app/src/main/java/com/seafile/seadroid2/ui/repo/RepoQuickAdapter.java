package com.seafile.seadroid2.ui.repo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.EncryptUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.bumptech.glide.signature.ObjectKey;
import com.seafile.seadroid2.R;
import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.account.SupportAccountManager;
import com.seafile.seadroid2.config.AbsLayoutItemType;
import com.seafile.seadroid2.config.Constants;
import com.seafile.seadroid2.config.GlideLoadConfig;
import com.seafile.seadroid2.databinding.ItemAccountBinding;
import com.seafile.seadroid2.databinding.ItemBlankBinding;
import com.seafile.seadroid2.databinding.ItemDirentBinding;
import com.seafile.seadroid2.databinding.ItemDirentGalleryBinding;
import com.seafile.seadroid2.databinding.ItemDirentGridBinding;
import com.seafile.seadroid2.databinding.ItemGroupItemBinding;
import com.seafile.seadroid2.databinding.ItemRepoBinding;
import com.seafile.seadroid2.databinding.ItemUnsupportedBinding;
import com.seafile.seadroid2.enums.FileViewType;
import com.seafile.seadroid2.enums.RepoSelectType;
import com.seafile.seadroid2.framework.data.db.entities.DirentModel;
import com.seafile.seadroid2.framework.data.db.entities.RepoModel;
import com.seafile.seadroid2.framework.data.model.BaseModel;
import com.seafile.seadroid2.framework.data.model.BlankModel;
import com.seafile.seadroid2.framework.data.model.GroupItemModel;
import com.seafile.seadroid2.framework.data.model.search.SearchModel;
import com.seafile.seadroid2.framework.datastore.DataManager;
import com.seafile.seadroid2.framework.http.HttpIO;
import com.seafile.seadroid2.framework.util.GlideApp;
import com.seafile.seadroid2.framework.util.SLogs;
import com.seafile.seadroid2.framework.util.ThumbnailUtils;
import com.seafile.seadroid2.framework.util.URLs;
import com.seafile.seadroid2.framework.util.Utils;
import com.seafile.seadroid2.ui.base.adapter.BaseMultiAdapter;
import com.seafile.seadroid2.ui.repo.vh.AccountViewHolder;
import com.seafile.seadroid2.ui.repo.vh.BlankViewHolder;
import com.seafile.seadroid2.ui.repo.vh.DirentGalleryViewHolder;
import com.seafile.seadroid2.ui.repo.vh.DirentGridViewHolder;
import com.seafile.seadroid2.ui.repo.vh.DirentViewHolder;
import com.seafile.seadroid2.ui.repo.vh.RepoViewHolder;
import com.seafile.seadroid2.ui.repo.vh.UnsupportedViewHolder;
import com.seafile.seadroid2.ui.viewholder.GroupItemViewHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RepoQuickAdapter extends BaseMultiAdapter<BaseModel> {
    private final String KEY_PAY_LOAD_IS_CHECK = "is_check";
    private final String KEY_PAY_LOAD_KEY_REFRESH_LOCAL_FILE_STATUS = "refresh_local_file_status";

    private boolean onActionMode;

    private boolean repoEncrypted = false;
    private FileViewType fileViewType = FileViewType.LIST;
    private RepoSelectType selectType = RepoSelectType.NOT_SELECTABLE;

    /**
     * <pre>
     *  -1 no limited
     *   0 do not write this value
     * >=1 max count
     * </pre>
     */
    private int selectedMaxCount = 1;

    public void setSelectType(RepoSelectType selectType) {
        this.selectType = selectType;
    }

    public void setSelectType(RepoSelectType selectType, int selectedMaxCount) {
        this.selectType = selectType;
        this.selectedMaxCount = selectedMaxCount;
    }

    private Drawable starDrawable;

    public Drawable getStarDrawable() {
        if (null == starDrawable) {
            int DP_16 = SizeUtils.dp2px(12);
            starDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_star_32);
            starDrawable.setBounds(0, 0, DP_16, DP_16);
            starDrawable.setTint(ContextCompat.getColor(getContext(), R.color.light_grey));
        }
        return starDrawable;
    }

    public void setRepoEncrypted(boolean repoEncrypted) {
        this.repoEncrypted = repoEncrypted;
    }

    public void setFileViewType(FileViewType fileViewType) {
        this.fileViewType = fileViewType;
    }

    public RepoQuickAdapter() {
        addItemType(AbsLayoutItemType.ACCOUNT, new OnMultiItem<BaseModel, AccountViewHolder>() {
            @NonNull
            @Override
            public AccountViewHolder onCreate(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
                ItemAccountBinding binding = ItemAccountBinding.inflate(LayoutInflater.from(context), viewGroup, false);
                return new AccountViewHolder(binding);
            }

            @Override
            public void onBind(@NonNull AccountViewHolder viewHolder, int i, @Nullable BaseModel baseModel) {
                onBindAccount(viewHolder, baseModel);
            }
        }).addItemType(AbsLayoutItemType.GROUP_ITEM, new OnMultiItem<BaseModel, GroupItemViewHolder>() {
            @NonNull
            @Override
            public GroupItemViewHolder onCreate(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
                ItemGroupItemBinding binding = ItemGroupItemBinding.inflate(LayoutInflater.from(context), viewGroup, false);
                return new GroupItemViewHolder(binding);
            }

            @Override
            public void onBind(@NonNull GroupItemViewHolder holder, int i, @Nullable BaseModel groupTimeModel) {
                onBind(holder, i, groupTimeModel, null);
            }

            @Override
            public void onBind(@NonNull GroupItemViewHolder holder, int position, @Nullable BaseModel item, @NonNull List<?> payloads) {
                onBindGroup(position, holder, (GroupItemModel) item, payloads);
            }
        }).addItemType(AbsLayoutItemType.REPO, new OnMultiItem<BaseModel, RepoViewHolder>() {
            @NonNull
            @Override
            public RepoViewHolder onCreate(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
                ItemRepoBinding binding = ItemRepoBinding.inflate(LayoutInflater.from(context), viewGroup, false);
                return new RepoViewHolder(binding);
            }

            @Override
            public void onBind(@NonNull RepoViewHolder viewHolder, int i, @Nullable BaseModel baseModel) {
                onBind(viewHolder, i, baseModel, Collections.emptyList());
            }

            @Override
            public void onBind(@NonNull RepoViewHolder holder, int position, @Nullable BaseModel item, @NonNull List<?> payloads) {

                onBindRepos(holder, (RepoModel) item, payloads);

            }
        }).addItemType(AbsLayoutItemType.DIRENT_LIST, new OnMultiItem<BaseModel, DirentViewHolder>() {
            @NonNull
            @Override
            public DirentViewHolder onCreate(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
                ItemDirentBinding binding = ItemDirentBinding.inflate(LayoutInflater.from(context), viewGroup, false);
                return new DirentViewHolder(binding);
            }

            @Override
            public void onBind(@NonNull DirentViewHolder viewHolder, int i, @Nullable BaseModel baseModel) {
                onBind(viewHolder, i, baseModel, Collections.emptyList());
            }

            @Override
            public void onBind(@NonNull DirentViewHolder holder, int position, @Nullable BaseModel item, @NonNull List<?> payloads) {
                onBindDirents(holder, (DirentModel) item, payloads);
            }
        }).addItemType(AbsLayoutItemType.DIRENT_GRID, new OnMultiItem<BaseModel, DirentGridViewHolder>() {
            @NonNull
            @Override
            public DirentGridViewHolder onCreate(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
                ItemDirentGridBinding binding = ItemDirentGridBinding.inflate(LayoutInflater.from(context), viewGroup, false);
                return new DirentGridViewHolder(binding);
            }

            @Override
            public void onBind(@NonNull DirentGridViewHolder viewHolder, int i, @Nullable BaseModel baseModel) {
                onBind(viewHolder, i, baseModel, Collections.emptyList());
            }

            @Override
            public void onBind(@NonNull DirentGridViewHolder holder, int position, @Nullable BaseModel item, @NonNull List<?> payloads) {
                onBindDirentsGrid(holder, (DirentModel) item, payloads);
            }
        }).addItemType(AbsLayoutItemType.DIRENT_GALLERY, new OnMultiItem<BaseModel, DirentGalleryViewHolder>() {
            @NonNull
            @Override
            public DirentGalleryViewHolder onCreate(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
                ItemDirentGalleryBinding binding = ItemDirentGalleryBinding.inflate(LayoutInflater.from(context), viewGroup, false);
                return new DirentGalleryViewHolder(binding);
            }

            @Override
            public void onBind(@NonNull DirentGalleryViewHolder viewHolder, int i, @Nullable BaseModel baseModel) {
                onBind(viewHolder, i, baseModel, Collections.emptyList());
            }

            @Override
            public void onBind(@NonNull DirentGalleryViewHolder holder, int position, @Nullable BaseModel item, @NonNull List<?> payloads) {
                onBindDirentsGallery(holder, (DirentModel) item, payloads);

            }
        }).addItemType(AbsLayoutItemType.SEARCH, new OnMultiItem<BaseModel, DirentViewHolder>() {
            @NonNull
            @Override
            public DirentViewHolder onCreate(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
                ItemDirentBinding binding = ItemDirentBinding.inflate(LayoutInflater.from(context), viewGroup, false);
                return new DirentViewHolder(binding);
            }

            @Override
            public void onBind(@NonNull DirentViewHolder viewHolder, int i, @Nullable BaseModel baseModel) {
                onBindSearch(viewHolder, (SearchModel) baseModel);
            }
        }).addItemType(AbsLayoutItemType.NOT_SUPPORTED, new OnMultiItem<BaseModel, UnsupportedViewHolder>() {
            @NonNull
            @Override
            public UnsupportedViewHolder onCreate(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
                ItemUnsupportedBinding binding = ItemUnsupportedBinding.inflate(LayoutInflater.from(context), viewGroup, false);
                return new UnsupportedViewHolder(binding);
            }

            @Override
            public void onBind(@NonNull UnsupportedViewHolder unsupportedViewHolder, int i, @Nullable BaseModel baseModel) {

            }

            @Override
            public void onBind(@NonNull UnsupportedViewHolder holder, int position, @Nullable BaseModel item, @NonNull List<?> payloads) {
                super.onBind(holder, position, item, payloads);
            }
        }).addItemType(AbsLayoutItemType.BLANK, new OnMultiItem<BaseModel, BlankViewHolder>() {
            @NonNull
            @Override
            public BlankViewHolder onCreate(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
                ItemBlankBinding binding = ItemBlankBinding.inflate(LayoutInflater.from(context), viewGroup, false);
                return new BlankViewHolder(binding);
            }

            @Override
            public void onBind(@NonNull BlankViewHolder unsupportedViewHolder, int i, @Nullable BaseModel baseModel) {

            }

            @Override
            public void onBind(@NonNull BlankViewHolder holder, int position, @Nullable BaseModel item, @NonNull List<?> payloads) {
                super.onBind(holder, position, item, payloads);
            }
        }).onItemViewType(new OnItemViewTypeListener<BaseModel>() {
            @Override
            public int onItemViewType(int i, @NonNull List<? extends BaseModel> list) {
                if (list.get(i) instanceof GroupItemModel) {
                    return AbsLayoutItemType.GROUP_ITEM;
                } else if (list.get(i) instanceof RepoModel) {
                    return AbsLayoutItemType.REPO;
                } else if (list.get(i) instanceof DirentModel) {
                    if (FileViewType.LIST == fileViewType) {
                        return AbsLayoutItemType.DIRENT_LIST;
                    } else if (FileViewType.GRID == fileViewType) {
                        return AbsLayoutItemType.DIRENT_GRID;
                    } else {
                        return AbsLayoutItemType.DIRENT_GALLERY;
                    }
                } else if (list.get(i) instanceof SearchModel) {
                    return AbsLayoutItemType.SEARCH;
                } else if (list.get(i) instanceof Account) {
                    return AbsLayoutItemType.ACCOUNT;
                } else if (list.get(i) instanceof BlankModel) {
                    return AbsLayoutItemType.BLANK;
                }
                return AbsLayoutItemType.NOT_SUPPORTED;
            }
        });
    }

    private void onBindAccount(AccountViewHolder holder, BaseModel model) {
//        holder.binding.getRoot().setBackground(null);

        Account account = (Account) model;

        holder.binding.listItemAccountTitle.setText(account.getServerHost());
        holder.binding.listItemAccountSubtitle.setText(account.getName());

        if (TextUtils.isEmpty(account.avatar_url)) {
            holder.binding.listItemAccountIcon.setImageResource(R.drawable.default_avatar);
        } else {
            GlideApp.with(getContext())
                    .load(account.avatar_url)
                    .apply(GlideLoadConfig.getAvatarOptions())
                    .into(holder.binding.listItemAccountIcon);
        }

        if (selectType.ordinal() >= RepoSelectType.ONLY_ACCOUNT.ordinal()) {
            holder.binding.itemSelectView.setVisibility(account.is_checked ? View.VISIBLE : View.INVISIBLE);
        } else {
            holder.binding.itemSelectView.setVisibility(View.INVISIBLE);
        }
    }

    private void onBindGroup(int position, GroupItemViewHolder holder, GroupItemModel model, List<?> payloads) {
        if (!TextUtils.isEmpty(model.title)) {
            if ("Organization".equals(model.title)) {
                holder.binding.itemGroupTitle.setText(R.string.shared_with_all);
            } else {
                holder.binding.itemGroupTitle.setText(model.title);
            }
        }

        if (selectType.ordinal() >= RepoSelectType.ONLY_ACCOUNT.ordinal()) {
            holder.binding.itemGroupExpand.setRotation(0);
            holder.binding.itemGroupExpand.setVisibility(View.GONE);
            holder.binding.getRoot().setClickable(false);
        } else {
            holder.binding.itemGroupExpand.setVisibility(View.VISIBLE);
            holder.binding.itemGroupExpand.setRotation(model.is_expanded ? 270 : 90);
            holder.binding.getRoot().setClickable(true);
        }
    }

    private void onBindRepos(RepoViewHolder holder, RepoModel model, @NonNull List<?> payloads) {
        if (!CollectionUtils.isEmpty(payloads)) {
            Bundle bundle = (Bundle) payloads.get(0);

            if (bundle.containsKey(KEY_PAY_LOAD_IS_CHECK)) {
                boolean isChecked = bundle.getBoolean(KEY_PAY_LOAD_IS_CHECK);
                updateItemMultiSelectViewWithPayload(holder.binding.itemMultiSelect, isChecked);
            }
            return;
        }

        holder.binding.itemTitle.setText(model.repo_name);
        holder.binding.itemSubtitle.setText(model.getSubtitle());
        holder.binding.itemIcon.setImageResource(model.getIcon());
//        holder.binding.getRoot().setBackground(AnimatedStateListDrawableCompatUtils.createDrawableCompat(getContext()));

        int color;
        if (selectType.ordinal() == RepoSelectType.ONLY_REPO.ordinal() || onActionMode) {
//            holder.binding.getRoot().setChecked(model.is_checked);

            holder.binding.itemMultiSelect.setVisibility(View.VISIBLE);

            if (model.is_checked) {
                color = ContextCompat.getColor(getContext(), R.color.fancy_orange);
                holder.binding.itemMultiSelect.setImageResource(R.drawable.ic_checkbox_checked);
            } else {
                color = ContextCompat.getColor(getContext(), R.color.bottom_sheet_pop_disable_color);
                holder.binding.itemMultiSelect.setImageResource(R.drawable.ic_checkbox_unchecked);
            }
            holder.binding.itemMultiSelect.setImageTintList(ColorStateList.valueOf(color));
        } else {
            color = ContextCompat.getColor(getContext(), R.color.bottom_sheet_pop_disable_color);
            holder.binding.itemMultiSelect.setVisibility(View.GONE);
//            holder.binding.getRoot().setChecked(false);
        }
        holder.binding.itemMultiSelect.setImageTintList(ColorStateList.valueOf(color));


        holder.binding.expandableToggleButton.setVisibility(View.GONE);

        holder.binding.itemTitle.setCompoundDrawablePadding(Constants.DP.DP_4);
        if (model.starred) {
            holder.binding.itemTitle.setCompoundDrawables(null, null, getStarDrawable(), null);
        } else {
            holder.binding.itemTitle.setCompoundDrawables(null, null, null, null);
        }
    }

    private void onBindDirents(DirentViewHolder holder, DirentModel model, @NonNull List<?> payloads) {
        if (!CollectionUtils.isEmpty(payloads)) {
            Bundle bundle = (Bundle) payloads.get(0);
            if (bundle.containsKey(KEY_PAY_LOAD_IS_CHECK)) {
                //
                boolean isChecked = bundle.getBoolean(KEY_PAY_LOAD_IS_CHECK);
                updateItemMultiSelectViewWithPayload(holder.binding.itemMultiSelect, isChecked);
            }

            if (bundle.containsKey(KEY_PAY_LOAD_KEY_REFRESH_LOCAL_FILE_STATUS)) {
                boolean isRefreshLocalFileStatus = bundle.getBoolean(KEY_PAY_LOAD_KEY_REFRESH_LOCAL_FILE_STATUS, false);
                if (isRefreshLocalFileStatus) {
                    if (model.isDir()) {
                        holder.binding.itemDownloadStatus.setVisibility(View.GONE);
                    } else if (TextUtils.equals(model.id, model.local_file_id)) {
                        holder.binding.itemDownloadStatus.setVisibility(View.VISIBLE);
                    } else {
                        holder.binding.itemDownloadStatus.setVisibility(View.GONE);
                    }
                }
            }
            return;
        }

        holder.binding.itemTitle.setText(model.name);
        holder.binding.itemSubtitle.setText(model.getSubtitle());

//        holder.binding.getRoot().setBackground(AnimatedStateListDrawableCompatUtils.createDrawableCompat(getContext()));

        if (model.isDir() || repoEncrypted || (!Utils.isViewableImage(model.name) && !Utils.isVideoFile(model.name))) {
            holder.binding.itemIcon.setImageResource(model.getIcon());
        } else {
            loadImage(model, holder.binding.itemIcon, smallSize);
        }

        //action mode
        updateItemMultiSelectView(holder.binding.itemMultiSelect, model.is_checked);

        if (selectType.ordinal() < RepoSelectType.ONLY_ACCOUNT.ordinal()) {
            holder.binding.itemTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.item_title_color));
            holder.binding.itemSubtitle.setTextColor(ContextCompat.getColor(getContext(), R.color.item_subtitle_color));
//            if (onActionMode) {
//                holder.binding.expandableToggleButton.setVisibility(View.GONE);
//            } else {
//                holder.binding.expandableToggleButton.setVisibility(View.VISIBLE);
//            }
        } else {

            if (!model.isDir()) {
                holder.binding.itemTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.light_grey));
                holder.binding.itemSubtitle.setTextColor(ContextCompat.getColor(getContext(), R.color.light_grey));
            } else {
                holder.binding.itemTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.item_title_color));
                holder.binding.itemSubtitle.setTextColor(ContextCompat.getColor(getContext(), R.color.item_subtitle_color));
            }

//            holder.binding.expandableToggleButton.setVisibility(View.INVISIBLE);
        }
        holder.binding.expandableToggleButton.setVisibility(View.GONE);

//        holder.binding.itemDownloadStatusProgressbar.setVisibility(View.GONE);

        if (model.isDir()) {
            holder.binding.itemDownloadStatus.setVisibility(View.GONE);
        } else if (TextUtils.equals(model.id, model.local_file_id)) {
            holder.binding.itemDownloadStatus.setVisibility(View.VISIBLE);
        } else {
            holder.binding.itemDownloadStatus.setVisibility(View.GONE);
        }

        if (model.starred) {
            holder.binding.itemTitle.setCompoundDrawables(null, null, getStarDrawable(), null);
        } else {
            holder.binding.itemTitle.setCompoundDrawables(null, null, null, null);
        }
    }

    private void onBindDirentsGrid(DirentGridViewHolder holder, DirentModel model, @NonNull List<?> payloads) {
        if (!CollectionUtils.isEmpty(payloads)) {
            Bundle bundle = (Bundle) payloads.get(0);
            if (bundle.containsKey(KEY_PAY_LOAD_IS_CHECK)) {
                //
                boolean isChecked = bundle.getBoolean(KEY_PAY_LOAD_IS_CHECK);
                updateItemMultiSelectViewWithPayload(holder.binding.itemMultiSelect, isChecked);
            }

            if (bundle.containsKey(KEY_PAY_LOAD_KEY_REFRESH_LOCAL_FILE_STATUS)) {
                boolean isRefreshLocalFileStatus = bundle.getBoolean(KEY_PAY_LOAD_KEY_REFRESH_LOCAL_FILE_STATUS, false);
                if (isRefreshLocalFileStatus) {
                    if (model.isDir()) {
                        holder.binding.itemDownloadStatus.setVisibility(View.GONE);
                    } else if (TextUtils.equals(model.id, model.local_file_id)) {
                        holder.binding.itemDownloadStatus.setVisibility(View.VISIBLE);
                    } else {
                        holder.binding.itemDownloadStatus.setVisibility(View.GONE);
                    }
                }
            }

            return;
        }


        holder.binding.itemTitle.setText(model.name);

//        holder.binding.getRoot().setBackground(AnimatedStateListDrawableCompatUtils.createDrawableCompat(getContext()));

        if (model.isDir()) {
            holder.binding.itemOutline.setVisibility(View.GONE);
        } else {
            holder.binding.itemOutline.setVisibility(View.VISIBLE);
        }

        if (model.isDir() || repoEncrypted || (!Utils.isViewableImage(model.name) && !Utils.isVideoFile(model.name))) {
            holder.binding.itemIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.binding.itemIcon.setImageResource(model.getIcon());
        } else {
            holder.binding.itemIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            loadImage(model, holder.binding.itemIcon, largeSize);
        }

        //action mode
        updateItemMultiSelectView(holder.binding.itemMultiSelect, model.is_checked);

        if (model.isDir()) {
            holder.binding.itemDownloadStatus.setVisibility(View.GONE);
        } else if (TextUtils.equals(model.id, model.local_file_id)) {
            holder.binding.itemDownloadStatus.setVisibility(View.VISIBLE);
        } else {
            holder.binding.itemDownloadStatus.setVisibility(View.GONE);
        }

        if (model.starred) {
            holder.binding.itemTitle.setCompoundDrawables(null, null, getStarDrawable(), null);
        } else {
            holder.binding.itemTitle.setCompoundDrawables(null, null, null, null);
        }
    }

    private void onBindDirentsGallery(DirentGalleryViewHolder holder, DirentModel model, @NonNull List<?> payloads) {
        if (!CollectionUtils.isEmpty(payloads)) {
            Bundle bundle = (Bundle) payloads.get(0);
            if (bundle.containsKey(KEY_PAY_LOAD_IS_CHECK)) {
                //
                boolean isChecked = bundle.getBoolean(KEY_PAY_LOAD_IS_CHECK);
                updateItemMultiSelectViewWithPayload(holder.binding.itemMultiSelect, isChecked);
            }
            return;
        }
//        holder.binding.getRoot().setBackground(AnimatedStateListDrawableCompatUtils.createDrawableCompat(getContext()));

        if (model.isDir() || repoEncrypted || (!Utils.isViewableImage(model.name) && !Utils.isVideoFile(model.name))) {
            holder.binding.itemIcon.setImageResource(model.getIcon());
        } else {
            loadImage(model, holder.binding.itemIcon, largeSize);
        }

        updateItemMultiSelectView(holder.binding.itemMultiSelect, model.is_checked);
    }

    private void updateItemMultiSelectViewWithPayload(ImageView imageView, boolean isChecked) {
        int color;

        if (isChecked) {
            color = ContextCompat.getColor(getContext(), R.color.fancy_orange);
            imageView.setImageResource(R.drawable.ic_checkbox_checked);
        } else {
            color = ContextCompat.getColor(getContext(), R.color.bottom_sheet_pop_disable_color);
            imageView.setImageResource(R.drawable.ic_checkbox_unchecked);
        }
        imageView.setImageTintList(ColorStateList.valueOf(color));
    }

    private void updateItemMultiSelectView(ImageView imageView, boolean isChecked) {
        int color;
        //action mode
        if (onActionMode) {
            imageView.setVisibility(View.VISIBLE);
//            holder.binding.getRoot().setChecked(model.is_checked);

            if (isChecked) {
                color = ContextCompat.getColor(getContext(), R.color.fancy_orange);
                imageView.setImageResource(R.drawable.ic_checkbox_checked);
            } else {
                color = ContextCompat.getColor(getContext(), R.color.bottom_sheet_pop_disable_color);
                imageView.setImageResource(R.drawable.ic_checkbox_unchecked);
            }
        } else {
//            holder.binding.getRoot().setChecked(false);
            color = ContextCompat.getColor(getContext(), R.color.bottom_sheet_pop_disable_color);
            imageView.setVisibility(View.GONE);
            imageView.setImageResource(R.drawable.ic_checkbox_unchecked);
        }
        imageView.setImageTintList(ColorStateList.valueOf(color));
    }

    private void onBindSearch(DirentViewHolder holder, SearchModel model) {
//        holder.binding.getRoot().setBackground(AnimatedStateListDrawableCompatUtils.createDrawableCompat(getContext()));

        if (!model.isDir()) {
            String displayName = URLs.getFileNameFromFullPath(model.fullpath);
            holder.binding.itemTitle.setText(displayName);
        } else {
            holder.binding.itemTitle.setText(model.name);
        }
        holder.binding.itemSubtitle.setText(model.getSubtitle());

        if (repoEncrypted || (!Utils.isViewableImage(model.name) && !Utils.isVideoFile(model.name))) {
            holder.binding.itemIcon.setImageResource(model.getIcon());
        } else {
            DirentModel direntModel = new DirentModel();
            direntModel.full_path = model.fullpath;
            direntModel.repo_id = model.repo_id;
            loadImage(direntModel, holder.binding.itemIcon, smallSize);
        }

        holder.binding.expandableToggleButton.setVisibility(View.GONE);
        holder.binding.itemMultiSelect.setVisibility(View.GONE);
//        holder.binding.itemDownloadStatusProgressbar.setVisibility(View.GONE);
        holder.binding.itemDownloadStatus.setVisibility(View.GONE);

        holder.binding.itemTitle.setCompoundDrawables(null, null, null, null);
    }

    private final int largeSize = 512;
    private final int smallSize = 128;

    private void loadImage(DirentModel direntModel, ImageView imageView, int size) {
        String thumbnailUrl = convertThumbnailUrl(direntModel, size);
        if (TextUtils.isEmpty(thumbnailUrl)) {
            GlideApp.with(getContext())
                    .load(direntModel.getIcon())
                    .apply(GlideLoadConfig.getCacheableThumbnailOptions())
                    .into(imageView);
            return;
        }

        String thumbKey = EncryptUtils.encryptMD5ToString(thumbnailUrl);

        GlideApp.with(getContext())
                .load(thumbnailUrl)
                .signature(new ObjectKey(thumbKey))
                .apply(GlideLoadConfig.getCustomDrawableOptions(direntModel.getIcon()))
                .into(imageView);
    }

    private String server_url;
    private final boolean isLogin = SupportAccountManager.getInstance().isLogin();

    private Account account;

    private Account getCurrentAccount() {
        if (account == null) {
            account = SupportAccountManager.getInstance().getCurrentAccount();
        }
        return account;
    }

    private String getServerUrl() {
        if (!TextUtils.isEmpty(server_url)) {
            return server_url;
        }

        if (!isLogin) {
            return null;
        }

        server_url = HttpIO.getCurrentInstance().getServerUrl();
        return server_url;
    }

    private String convertThumbnailUrl(DirentModel direntModel, int size) {
        String serverUrl = getServerUrl();
        if (TextUtils.isEmpty(serverUrl)) {
            return null;
        }
        return ThumbnailUtils.convertThumbnailUrl(serverUrl, direntModel.repo_id, direntModel.full_path, size);
    }

    public void setOnActionMode(boolean on) {
        this.onActionMode = on;

        if (!on) {
            setAllItemSelected(false);
        }

        notifyItemRangeChanged(0, getItemCount());
    }

    public boolean isOnActionMode() {
        return onActionMode;
    }

    public void setAllItemSelected(boolean itemSelected) {
        for (BaseModel item : getItems()) {

            if (item instanceof GroupItemModel) {
                continue;
            } else if (item instanceof Account) {
                continue;
            } else if (item instanceof SearchModel) {
                continue;
            }

            item.is_checked = itemSelected;
        }

        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_PAY_LOAD_IS_CHECK, itemSelected);
        notifyItemRangeChanged(0, getItemCount(), bundle);
    }

    public List<BaseModel> getSelectedList() {
        return getItems().stream().filter(f -> f.is_checked).collect(Collectors.toList());
    }

    /**
     * @return is selected?
     */
    public boolean selectItemByMode(int position) {
        BaseModel item = getItems().get(position);

        //single
        if (selectedMaxCount == 1) {
            int selectedPosition = getSelectedPositionByMode();
            if (selectedPosition == position) {
                item.is_checked = !item.is_checked;
                notifyItemChanged(selectedPosition);
                return item.is_checked;

            } else if (selectedPosition > -1) {
                //Deselect an item that has already been selected
                getItems().get(selectedPosition).is_checked = false;
                notifyItemChanged(selectedPosition);

                item.is_checked = true;
                notifyItemChanged(position);
            } else {
                item.is_checked = true;
                notifyItemChanged(position);
            }
        } else {
            long selectedCount = getSelectedCountBySelectType();
            if (selectedCount >= selectedMaxCount) {
                return false;
            }

            item.is_checked = !item.is_checked;
            notifyItemChanged(position);

            return item.is_checked;
        }

        return true;
    }

    private long getSelectedCountBySelectType() {
        if (RepoSelectType.ONLY_ACCOUNT == selectType) {
            return getItems().stream()
                    .filter(Account.class::isInstance)
                    .filter(f -> f.is_checked)
                    .count();
        } else if (RepoSelectType.ONLY_REPO == selectType) {
            return getItems().stream()
                    .filter(RepoModel.class::isInstance)
                    .filter(f -> f.is_checked)
                    .count();
        }
        return 0;
    }

    private int getSelectedPositionByMode() {
        for (int i = 0; i < getItems().size(); i++) {
            if (getItems().get(i).is_checked) {
                return i;
            }
        }
        return -1;
    }

    public void filterListBySearchKeyword(String searchContent) {
        this.searchContent = searchContent;

        if (CollectionUtils.isEmpty(finalList)) {
            return;
        }

        List<BaseModel> filterList;
        if (!TextUtils.isEmpty(searchContent)) {
            filterList = finalList.stream().filter(_searchFilter).collect(Collectors.toList());
        } else {
            filterList = finalList;
        }
        notify(filterList);
    }

    private List<BaseModel> finalList;

    public void notifyDataChanged(List<BaseModel> list) {
        if (CollectionUtils.isEmpty(list)) {
            finalList = null;
            submitList(null);
            return;
        }

        finalList = new ArrayList<>(list);
        if (CollectionUtils.isEmpty(getItems())) {
            submitList(finalList);
            return;
        }

        if (finalList.size() == 1) {
            submitList(finalList);
        } else {
            notify(finalList);
        }
    }

    private void notify(List<BaseModel> list) {

        final List<BaseModel> oldList = getItems();
        final List<BaseModel> newList = list;

        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return getItems().size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldClassName = getItems().get(oldItemPosition).getClass().getName();
                String newClassName = newList.get(newItemPosition).getClass().getName();
                if (!oldClassName.equals(newClassName)) {
                    return false;
                }

                if (getItems().get(oldItemPosition) instanceof Account newT) {
                    Account oldT = (Account) newList.get(newItemPosition);
                    return TextUtils.equals(newT.email, oldT.email);
                }

                if (getItems().get(oldItemPosition) instanceof GroupItemModel newT) {
                    GroupItemModel oldT = (GroupItemModel) newList.get(newItemPosition);
                    return TextUtils.equals(newT.title, oldT.title);
                }

                if (getItems().get(oldItemPosition) instanceof RepoModel newT) {
                    RepoModel oldT = (RepoModel) newList.get(newItemPosition);
                    return TextUtils.equals(newT.repo_id, oldT.repo_id) && newT.group_id == oldT.group_id;
                }

                if (getItems().get(oldItemPosition) instanceof DirentModel newT) {
                    DirentModel oldT = (DirentModel) newList.get(newItemPosition);
                    return TextUtils.equals(newT.full_path, oldT.full_path);
                }

                if (getItems().get(oldItemPosition) instanceof SearchModel newT) {
                    SearchModel oldT = (SearchModel) newList.get(newItemPosition);
                    return TextUtils.equals(newT.fullpath, oldT.fullpath);
                }

                return true;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                String oldClassName = getItems().get(oldItemPosition).getClass().getName();
                String newClassName = newList.get(newItemPosition).getClass().getName();
                if (!oldClassName.equals(newClassName)) {
                    return false;
                }

                if (getItems().get(oldItemPosition) instanceof Account newT) {
                    Account oldT = (Account) newList.get(newItemPosition);
                    return newT.equals(oldT);
                }

                if (getItems().get(oldItemPosition) instanceof GroupItemModel newT) {
                    GroupItemModel oldT = (GroupItemModel) newList.get(newItemPosition);
                    return newT.equals(oldT);
                }


                if (getItems().get(oldItemPosition) instanceof RepoModel newT) {
                    RepoModel oldT = (RepoModel) newList.get(newItemPosition);
                    return newT.equals(oldT);
                }


                if (getItems().get(oldItemPosition) instanceof SearchModel newT) {
                    SearchModel oldT = (SearchModel) newList.get(newItemPosition);
                    return newT.equals(oldT);
                }

                if (getItems().get(oldItemPosition) instanceof DirentModel newT) {
                    DirentModel oldT = (DirentModel) newList.get(newItemPosition);
                    return newT.equals(oldT);
                }

                return true;
            }
        });

        setItems(newList);
        diffResult.dispatchUpdatesTo(this);

        if (oldList.equals(newList)) {
            Bundle payload = new Bundle();
            payload.putBoolean(KEY_PAY_LOAD_KEY_REFRESH_LOCAL_FILE_STATUS, true);
            notifyItemRangeChanged(0, newList.size(), payload);
        }

    }

    private String searchContent;
    private final Predicate<? super BaseModel> _searchFilter = new Predicate<>() {
        @Override
        public boolean test(BaseModel baseModel) {
            if (TextUtils.isEmpty(searchContent)) {
                return true;
            }

            if (baseModel instanceof Account) {
                return false;
            } else if (baseModel instanceof GroupItemModel) {
                return false;
            } else if (baseModel instanceof RepoModel m) {
                return m.repo_name.toLowerCase().contains(searchContent.toLowerCase());
            } else if (baseModel instanceof DirentModel m) {
                return m.name.toLowerCase().contains(searchContent.toLowerCase());
            } else {
                return false;
            }
        }
    };
}
