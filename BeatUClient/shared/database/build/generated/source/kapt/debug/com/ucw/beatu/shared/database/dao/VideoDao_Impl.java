package com.ucw.beatu.shared.database.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.ucw.beatu.shared.database.converter.Converters;
import com.ucw.beatu.shared.database.entity.VideoEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class VideoDao_Impl implements VideoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<VideoEntity> __insertionAdapterOfVideoEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfClear;

  public VideoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfVideoEntity = new EntityInsertionAdapter<VideoEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `videos` (`id`,`playUrl`,`coverUrl`,`title`,`tags`,`durationMs`,`orientation`,`authorId`,`authorName`,`likeCount`,`commentCount`,`favoriteCount`,`shareCount`,`viewCount`,`isLiked`,`isFavorited`,`isFollowedAuthor`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VideoEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getPlayUrl() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPlayUrl());
        }
        if (entity.getCoverUrl() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getCoverUrl());
        }
        if (entity.getTitle() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTitle());
        }
        final String _tmp = __converters.toJson(entity.getTags());
        if (_tmp == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp);
        }
        statement.bindLong(6, entity.getDurationMs());
        if (entity.getOrientation() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getOrientation());
        }
        if (entity.getAuthorId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getAuthorId());
        }
        if (entity.getAuthorName() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getAuthorName());
        }
        statement.bindLong(10, entity.getLikeCount());
        statement.bindLong(11, entity.getCommentCount());
        statement.bindLong(12, entity.getFavoriteCount());
        statement.bindLong(13, entity.getShareCount());
        statement.bindLong(14, entity.getViewCount());
        final int _tmp_1 = entity.isLiked() ? 1 : 0;
        statement.bindLong(15, _tmp_1);
        final int _tmp_2 = entity.isFavorited() ? 1 : 0;
        statement.bindLong(16, _tmp_2);
        final int _tmp_3 = entity.isFollowedAuthor() ? 1 : 0;
        statement.bindLong(17, _tmp_3);
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM videos";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<VideoEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfVideoEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clear(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClear.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClear.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<VideoEntity>> observeTopVideos(final int limit) {
    final String _sql = "SELECT * FROM videos ORDER BY viewCount DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"videos"}, new Callable<List<VideoEntity>>() {
      @Override
      @NonNull
      public List<VideoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlayUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "playUrl");
          final int _cursorIndexOfCoverUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "coverUrl");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfOrientation = CursorUtil.getColumnIndexOrThrow(_cursor, "orientation");
          final int _cursorIndexOfAuthorId = CursorUtil.getColumnIndexOrThrow(_cursor, "authorId");
          final int _cursorIndexOfAuthorName = CursorUtil.getColumnIndexOrThrow(_cursor, "authorName");
          final int _cursorIndexOfLikeCount = CursorUtil.getColumnIndexOrThrow(_cursor, "likeCount");
          final int _cursorIndexOfCommentCount = CursorUtil.getColumnIndexOrThrow(_cursor, "commentCount");
          final int _cursorIndexOfFavoriteCount = CursorUtil.getColumnIndexOrThrow(_cursor, "favoriteCount");
          final int _cursorIndexOfShareCount = CursorUtil.getColumnIndexOrThrow(_cursor, "shareCount");
          final int _cursorIndexOfViewCount = CursorUtil.getColumnIndexOrThrow(_cursor, "viewCount");
          final int _cursorIndexOfIsLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLiked");
          final int _cursorIndexOfIsFavorited = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorited");
          final int _cursorIndexOfIsFollowedAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "isFollowedAuthor");
          final List<VideoEntity> _result = new ArrayList<VideoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VideoEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpPlayUrl;
            if (_cursor.isNull(_cursorIndexOfPlayUrl)) {
              _tmpPlayUrl = null;
            } else {
              _tmpPlayUrl = _cursor.getString(_cursorIndexOfPlayUrl);
            }
            final String _tmpCoverUrl;
            if (_cursor.isNull(_cursorIndexOfCoverUrl)) {
              _tmpCoverUrl = null;
            } else {
              _tmpCoverUrl = _cursor.getString(_cursorIndexOfCoverUrl);
            }
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final List<String> _tmpTags;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfTags);
            }
            _tmpTags = __converters.fromJson(_tmp);
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpOrientation;
            if (_cursor.isNull(_cursorIndexOfOrientation)) {
              _tmpOrientation = null;
            } else {
              _tmpOrientation = _cursor.getString(_cursorIndexOfOrientation);
            }
            final String _tmpAuthorId;
            if (_cursor.isNull(_cursorIndexOfAuthorId)) {
              _tmpAuthorId = null;
            } else {
              _tmpAuthorId = _cursor.getString(_cursorIndexOfAuthorId);
            }
            final String _tmpAuthorName;
            if (_cursor.isNull(_cursorIndexOfAuthorName)) {
              _tmpAuthorName = null;
            } else {
              _tmpAuthorName = _cursor.getString(_cursorIndexOfAuthorName);
            }
            final long _tmpLikeCount;
            _tmpLikeCount = _cursor.getLong(_cursorIndexOfLikeCount);
            final long _tmpCommentCount;
            _tmpCommentCount = _cursor.getLong(_cursorIndexOfCommentCount);
            final long _tmpFavoriteCount;
            _tmpFavoriteCount = _cursor.getLong(_cursorIndexOfFavoriteCount);
            final long _tmpShareCount;
            _tmpShareCount = _cursor.getLong(_cursorIndexOfShareCount);
            final long _tmpViewCount;
            _tmpViewCount = _cursor.getLong(_cursorIndexOfViewCount);
            final boolean _tmpIsLiked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsLiked);
            _tmpIsLiked = _tmp_1 != 0;
            final boolean _tmpIsFavorited;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsFavorited);
            _tmpIsFavorited = _tmp_2 != 0;
            final boolean _tmpIsFollowedAuthor;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFollowedAuthor);
            _tmpIsFollowedAuthor = _tmp_3 != 0;
            _item = new VideoEntity(_tmpId,_tmpPlayUrl,_tmpCoverUrl,_tmpTitle,_tmpTags,_tmpDurationMs,_tmpOrientation,_tmpAuthorId,_tmpAuthorName,_tmpLikeCount,_tmpCommentCount,_tmpFavoriteCount,_tmpShareCount,_tmpViewCount,_tmpIsLiked,_tmpIsFavorited,_tmpIsFollowedAuthor);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
