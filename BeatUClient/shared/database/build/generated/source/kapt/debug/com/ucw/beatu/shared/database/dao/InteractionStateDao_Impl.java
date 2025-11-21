package com.ucw.beatu.shared.database.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.ucw.beatu.shared.database.entity.InteractionStateEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class InteractionStateDao_Impl implements InteractionStateDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<InteractionStateEntity> __insertionAdapterOfInteractionStateEntity;

  public InteractionStateDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfInteractionStateEntity = new EntityInsertionAdapter<InteractionStateEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `interaction_state` (`videoId`,`liked`,`favorited`,`followed`,`lastSeekMs`,`defaultSpeed`,`defaultQuality`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final InteractionStateEntity entity) {
        if (entity.getVideoId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getVideoId());
        }
        final int _tmp = entity.getLiked() ? 1 : 0;
        statement.bindLong(2, _tmp);
        final int _tmp_1 = entity.getFavorited() ? 1 : 0;
        statement.bindLong(3, _tmp_1);
        final int _tmp_2 = entity.getFollowed() ? 1 : 0;
        statement.bindLong(4, _tmp_2);
        statement.bindLong(5, entity.getLastSeekMs());
        statement.bindDouble(6, entity.getDefaultSpeed());
        if (entity.getDefaultQuality() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getDefaultQuality());
        }
      }
    };
  }

  @Override
  public Object upsert(final InteractionStateEntity state,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfInteractionStateEntity.insert(state);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<InteractionStateEntity> observe(final String videoId) {
    final String _sql = "SELECT * FROM interaction_state WHERE videoId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (videoId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, videoId);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"interaction_state"}, new Callable<InteractionStateEntity>() {
      @Override
      @Nullable
      public InteractionStateEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "videoId");
          final int _cursorIndexOfLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "liked");
          final int _cursorIndexOfFavorited = CursorUtil.getColumnIndexOrThrow(_cursor, "favorited");
          final int _cursorIndexOfFollowed = CursorUtil.getColumnIndexOrThrow(_cursor, "followed");
          final int _cursorIndexOfLastSeekMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeekMs");
          final int _cursorIndexOfDefaultSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultSpeed");
          final int _cursorIndexOfDefaultQuality = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultQuality");
          final InteractionStateEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpVideoId;
            if (_cursor.isNull(_cursorIndexOfVideoId)) {
              _tmpVideoId = null;
            } else {
              _tmpVideoId = _cursor.getString(_cursorIndexOfVideoId);
            }
            final boolean _tmpLiked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfLiked);
            _tmpLiked = _tmp != 0;
            final boolean _tmpFavorited;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfFavorited);
            _tmpFavorited = _tmp_1 != 0;
            final boolean _tmpFollowed;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfFollowed);
            _tmpFollowed = _tmp_2 != 0;
            final long _tmpLastSeekMs;
            _tmpLastSeekMs = _cursor.getLong(_cursorIndexOfLastSeekMs);
            final float _tmpDefaultSpeed;
            _tmpDefaultSpeed = _cursor.getFloat(_cursorIndexOfDefaultSpeed);
            final String _tmpDefaultQuality;
            if (_cursor.isNull(_cursorIndexOfDefaultQuality)) {
              _tmpDefaultQuality = null;
            } else {
              _tmpDefaultQuality = _cursor.getString(_cursorIndexOfDefaultQuality);
            }
            _result = new InteractionStateEntity(_tmpVideoId,_tmpLiked,_tmpFavorited,_tmpFollowed,_tmpLastSeekMs,_tmpDefaultSpeed,_tmpDefaultQuality);
          } else {
            _result = null;
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
