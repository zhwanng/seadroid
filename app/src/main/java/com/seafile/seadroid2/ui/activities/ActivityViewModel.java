package com.seafile.seadroid2.ui.activities;

import android.text.TextUtils;
import android.util.Pair;

import androidx.lifecycle.MutableLiveData;

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.seafile.seadroid2.SeafException;
import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.account.SupportAccountManager;
import com.seafile.seadroid2.framework.crypto.SecurePasswordManager;
import com.seafile.seadroid2.framework.data.db.AppDatabase;
import com.seafile.seadroid2.framework.data.db.entities.EncKeyCacheEntity;
import com.seafile.seadroid2.framework.data.db.entities.FileCacheStatusEntity;
import com.seafile.seadroid2.framework.data.db.entities.RepoModel;
import com.seafile.seadroid2.framework.data.db.entities.StarredModel;
import com.seafile.seadroid2.framework.data.model.ResultModel;
import com.seafile.seadroid2.framework.data.model.TResultModel;
import com.seafile.seadroid2.framework.data.model.dirents.DirentFileModel;
import com.seafile.seadroid2.framework.datastore.sp.SettingsManager;
import com.seafile.seadroid2.ui.base.viewmodel.BaseViewModel;
import com.seafile.seadroid2.enums.OpType;
import com.seafile.seadroid2.framework.http.HttpIO;
import com.seafile.seadroid2.framework.data.model.activities.ActivityModel;
import com.seafile.seadroid2.framework.data.model.activities.ActivityWrapperModel;
import com.seafile.seadroid2.framework.util.SLogs;
import com.seafile.seadroid2.ui.dialog_fragment.DialogService;
import com.seafile.seadroid2.ui.file.FileService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import okhttp3.RequestBody;

public class ActivityViewModel extends BaseViewModel {
    private final MutableLiveData<List<ActivityModel>> listLiveData = new MutableLiveData<>();

    public MutableLiveData<List<ActivityModel>> getListLiveData() {
        return listLiveData;
    }

    public void getRepoModelFromLocal(String repoId, Consumer<RepoModel> consumer) {
        Single<List<RepoModel>> singleDb = AppDatabase.getInstance().repoDao().getRepoById(repoId);
        addSingleDisposable(singleDb, new Consumer<List<RepoModel>>() {
            @Override
            public void accept(List<RepoModel> repoModels) throws Exception {
                if (consumer != null) {
                    if (CollectionUtils.isEmpty(repoModels)) {
                        //no data in sqlite
                    } else {
                        consumer.accept(repoModels.get(0));
                    }
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                SLogs.e(throwable);
            }
        });
    }

    public void decryptRepo(String repoId, Consumer<String> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("consumer is null");
        }

        Single<List<EncKeyCacheEntity>> encSingle = AppDatabase.getInstance().encKeyCacheDAO().getListByRepoIdAsync(repoId);
        Single<String> s = encSingle.flatMap(new Function<List<EncKeyCacheEntity>, SingleSource<String>>() {
            @Override
            public SingleSource<String> apply(List<EncKeyCacheEntity> encKeyCacheEntities) throws Exception {
                if (CollectionUtils.isEmpty(encKeyCacheEntities)) {
                    return Single.just("need-to-re-enter-password");//need password and save into database
                }

                long now = TimeUtils.getNowMills();
                EncKeyCacheEntity encKeyCacheEntity = encKeyCacheEntities.get(0);
                boolean isExpired = encKeyCacheEntity.expire_time_long == 0 || now > encKeyCacheEntity.expire_time_long;
                if (isExpired) {
                    if (TextUtils.isEmpty(encKeyCacheEntity.enc_key) || TextUtils.isEmpty(encKeyCacheEntity.enc_iv)) {
                        return Single.just("need-to-re-enter-password");//expired, need password
                    } else {
                        String decryptPassword = SecurePasswordManager.decryptPassword(encKeyCacheEntity.enc_key, encKeyCacheEntity.enc_iv);
                        return Single.just(decryptPassword);//expired, but no password
                    }
                }

                return Single.just("done");
            }
        });


        addSingleDisposable(s, new Consumer<String>() {
            @Override
            public void accept(String i) throws Exception {
                consumer.accept(i);
            }
        });
    }


    public void remoteVerify(String repoId, String password, Consumer<ResultModel> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("consumer is null");
        }
        getRefreshLiveData().setValue(true);

        Map<String, String> requestDataMap = new HashMap<>();
        requestDataMap.put("password", password);
        Map<String, RequestBody> bodyMap = genRequestBody(requestDataMap);

        Single<ResultModel> netSingle = HttpIO.getCurrentInstance().execute(DialogService.class).setPassword(repoId, bodyMap);
        Single<ResultModel> single = netSingle.flatMap(new Function<ResultModel, SingleSource<ResultModel>>() {
            @Override
            public SingleSource<ResultModel> apply(ResultModel resultModel) throws Exception {
                //update local password and expire
                EncKeyCacheEntity encEntity = new EncKeyCacheEntity();
                encEntity.v = 2;
                encEntity.repo_id = repoId;

                Pair<String, String> p = SecurePasswordManager.encryptPassword(password);
                if (p != null) {
                    encEntity.enc_key = p.first;
                    encEntity.enc_iv = p.second;

                    long expire = TimeUtils.getNowMills();
                    expire += SettingsManager.DECRYPTION_EXPIRATION_TIME;
                    encEntity.expire_time_long = expire;
                    AppDatabase.getInstance().encKeyCacheDAO().insert(encEntity);
                }


                return Single.just(resultModel);
            }
        });

        addSingleDisposable(single, tResultModel -> {
            getRefreshLiveData().setValue(false);
            consumer.accept(tResultModel);
        }, throwable -> {
            getRefreshLiveData().setValue(false);

            TResultModel<RepoModel> tResultModel = new TResultModel<>();
            tResultModel.error_msg = getErrorMsgByThrowable(throwable);
            consumer.accept(tResultModel);
        });
    }

    public void checkRemoteAndOpen(String repo_id, String path, Consumer<String> consumer) {
        getSecondRefreshLiveData().setValue(true);
        Single<DirentFileModel> detailSingle = HttpIO.getCurrentInstance().execute(FileService.class).getFileDetail(repo_id, path);

        Single<List<FileCacheStatusEntity>> dbSingle = AppDatabase.getInstance().fileCacheStatusDAO().getByFullPath(repo_id, path);
        Single<String> fileIdSingle = dbSingle.flatMap(new Function<List<FileCacheStatusEntity>, SingleSource<String>>() {
            @Override
            public SingleSource<String> apply(List<FileCacheStatusEntity> f) {
                if (CollectionUtils.isEmpty(f)) {
                    return Single.just("");
                }

                if (TextUtils.isEmpty(f.get(0).file_id)) {
                    return Single.just("");
                }

                return Single.just(f.get(0).file_id);
            }
        }).flatMap(new Function<String, SingleSource<String>>() {
            @Override
            public SingleSource<String> apply(String local_file_id) throws Exception {
                if (TextUtils.isEmpty(local_file_id)) {
                    return Single.just("");
                }

                return detailSingle.flatMap(new Function<DirentFileModel, SingleSource<? extends String>>() {
                    @Override
                    public SingleSource<? extends String> apply(DirentFileModel direntFileModel) throws Exception {
                        if (direntFileModel == null) {
                            return Single.just("");
                        }
                        if (!direntFileModel.id.equals(local_file_id)) {
                            return Single.just("");
                        }
                        return Single.just(local_file_id);
                    }
                });
            }
        });

        addSingleDisposable(fileIdSingle, new Consumer<String>() {
            @Override
            public void accept(String local_file_id) throws Exception {
                if (consumer != null) {
                    consumer.accept(local_file_id);
                }
                getSecondRefreshLiveData().setValue(false);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                if (consumer != null) {
                    consumer.accept(null);
                }
                getSecondRefreshLiveData().setValue(false);
            }
        });


    }

    public void loadAllData(int page) {
        getRefreshLiveData().setValue(true);
        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        Single<ActivityWrapperModel> flowable = HttpIO.getCurrentInstance().execute(ActivityService.class).getActivities(page);
        addSingleDisposable(flowable, new Consumer<ActivityWrapperModel>() {
            @Override
            public void accept(ActivityWrapperModel wrapperModel) throws Exception {
                getRefreshLiveData().setValue(false);

                if (wrapperModel == null) {
                    return;
                }
                for (ActivityModel event : wrapperModel.events) {
                    event.related_account = account.getSignature();
                    switch (event.op_type) {
                        case "create":
                            event.opType = OpType.CREATE;
                            break;
                        case "edit":
                            event.opType = OpType.EDIT;
                            break;
                        case "rename":
                            event.opType = OpType.RENAME;
                            break;
                        case "delete":
                            event.opType = OpType.DELETE;
                            break;
                        case "restore":
                            event.opType = OpType.RESTORE;
                            break;
                        case "move":
                            event.opType = OpType.MOVE;
                            break;
                        case "update":
                            event.opType = OpType.UPDATE;
                            break;
                        case "public":
                            event.opType = OpType.PUBLISH;
                            break;
                    }
                }
                getListLiveData().setValue(wrapperModel.events);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                getRefreshLiveData().setValue(false);
                SeafException seafException = getExceptionByThrowable(throwable);

                if (seafException == SeafException.REMOTE_WIPED_EXCEPTION) {
                    //post a request
                    completeRemoteWipe();
                }

                getSeafExceptionLiveData().setValue(seafException);
            }
        });
    }
}
