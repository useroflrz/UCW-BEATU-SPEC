package com.ucw.beatu.shared.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.ucw.beatu.shared.database.dao.CommentDao;
import com.ucw.beatu.shared.database.dao.CommentDao_Impl;
import com.ucw.beatu.shared.database.dao.InteractionStateDao;
import com.ucw.beatu.shared.database.dao.InteractionStateDao_Impl;
import com.ucw.beatu.shared.database.dao.VideoDao;
import com.ucw.beatu.shared.database.dao.VideoDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BeatUDatabase_Impl extends BeatUDatabase {
  private volatile VideoDao _videoDao;

  private volatile CommentDao _commentDao;

  private volatile InteractionStateDao _interactionStateDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `videos` (`id` TEXT NOT NULL, `playUrl` TEXT NOT NULL, `coverUrl` TEXT NOT NULL, `title` TEXT NOT NULL, `tags` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, `orientation` TEXT NOT NULL, `authorId` TEXT NOT NULL, `authorName` TEXT NOT NULL, `likeCount` INTEGER NOT NULL, `commentCount` INTEGER NOT NULL, `favoriteCount` INTEGER NOT NULL, `shareCount` INTEGER NOT NULL, `viewCount` INTEGER NOT NULL, `isLiked` INTEGER NOT NULL, `isFavorited` INTEGER NOT NULL, `isFollowedAuthor` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `comments` (`id` TEXT NOT NULL, `videoId` TEXT NOT NULL, `authorId` TEXT NOT NULL, `authorName` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isAiReply` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `interaction_state` (`videoId` TEXT NOT NULL, `liked` INTEGER NOT NULL, `favorited` INTEGER NOT NULL, `followed` INTEGER NOT NULL, `lastSeekMs` INTEGER NOT NULL, `defaultSpeed` REAL NOT NULL, `defaultQuality` TEXT NOT NULL, PRIMARY KEY(`videoId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b4bfcc242e574bfd43371537a3c01c13')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `videos`");
        db.execSQL("DROP TABLE IF EXISTS `comments`");
        db.execSQL("DROP TABLE IF EXISTS `interaction_state`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsVideos = new HashMap<String, TableInfo.Column>(17);
        _columnsVideos.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("playUrl", new TableInfo.Column("playUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("coverUrl", new TableInfo.Column("coverUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("tags", new TableInfo.Column("tags", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("durationMs", new TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("orientation", new TableInfo.Column("orientation", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("authorId", new TableInfo.Column("authorId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("authorName", new TableInfo.Column("authorName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("likeCount", new TableInfo.Column("likeCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("commentCount", new TableInfo.Column("commentCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("favoriteCount", new TableInfo.Column("favoriteCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("shareCount", new TableInfo.Column("shareCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("viewCount", new TableInfo.Column("viewCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("isLiked", new TableInfo.Column("isLiked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("isFavorited", new TableInfo.Column("isFavorited", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("isFollowedAuthor", new TableInfo.Column("isFollowedAuthor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVideos = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVideos = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVideos = new TableInfo("videos", _columnsVideos, _foreignKeysVideos, _indicesVideos);
        final TableInfo _existingVideos = TableInfo.read(db, "videos");
        if (!_infoVideos.equals(_existingVideos)) {
          return new RoomOpenHelper.ValidationResult(false, "videos(com.ucw.beatu.shared.database.entity.VideoEntity).\n"
                  + " Expected:\n" + _infoVideos + "\n"
                  + " Found:\n" + _existingVideos);
        }
        final HashMap<String, TableInfo.Column> _columnsComments = new HashMap<String, TableInfo.Column>(7);
        _columnsComments.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsComments.put("videoId", new TableInfo.Column("videoId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsComments.put("authorId", new TableInfo.Column("authorId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsComments.put("authorName", new TableInfo.Column("authorName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsComments.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsComments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsComments.put("isAiReply", new TableInfo.Column("isAiReply", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysComments = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesComments = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoComments = new TableInfo("comments", _columnsComments, _foreignKeysComments, _indicesComments);
        final TableInfo _existingComments = TableInfo.read(db, "comments");
        if (!_infoComments.equals(_existingComments)) {
          return new RoomOpenHelper.ValidationResult(false, "comments(com.ucw.beatu.shared.database.entity.CommentEntity).\n"
                  + " Expected:\n" + _infoComments + "\n"
                  + " Found:\n" + _existingComments);
        }
        final HashMap<String, TableInfo.Column> _columnsInteractionState = new HashMap<String, TableInfo.Column>(7);
        _columnsInteractionState.put("videoId", new TableInfo.Column("videoId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInteractionState.put("liked", new TableInfo.Column("liked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInteractionState.put("favorited", new TableInfo.Column("favorited", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInteractionState.put("followed", new TableInfo.Column("followed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInteractionState.put("lastSeekMs", new TableInfo.Column("lastSeekMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInteractionState.put("defaultSpeed", new TableInfo.Column("defaultSpeed", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInteractionState.put("defaultQuality", new TableInfo.Column("defaultQuality", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysInteractionState = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesInteractionState = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoInteractionState = new TableInfo("interaction_state", _columnsInteractionState, _foreignKeysInteractionState, _indicesInteractionState);
        final TableInfo _existingInteractionState = TableInfo.read(db, "interaction_state");
        if (!_infoInteractionState.equals(_existingInteractionState)) {
          return new RoomOpenHelper.ValidationResult(false, "interaction_state(com.ucw.beatu.shared.database.entity.InteractionStateEntity).\n"
                  + " Expected:\n" + _infoInteractionState + "\n"
                  + " Found:\n" + _existingInteractionState);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "b4bfcc242e574bfd43371537a3c01c13", "8777ad20e537ba5c3173e0e4b43e8bf2");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "videos","comments","interaction_state");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `videos`");
      _db.execSQL("DELETE FROM `comments`");
      _db.execSQL("DELETE FROM `interaction_state`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(VideoDao.class, VideoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CommentDao.class, CommentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(InteractionStateDao.class, InteractionStateDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public VideoDao videoDao() {
    if (_videoDao != null) {
      return _videoDao;
    } else {
      synchronized(this) {
        if(_videoDao == null) {
          _videoDao = new VideoDao_Impl(this);
        }
        return _videoDao;
      }
    }
  }

  @Override
  public CommentDao commentDao() {
    if (_commentDao != null) {
      return _commentDao;
    } else {
      synchronized(this) {
        if(_commentDao == null) {
          _commentDao = new CommentDao_Impl(this);
        }
        return _commentDao;
      }
    }
  }

  @Override
  public InteractionStateDao interactionStateDao() {
    if (_interactionStateDao != null) {
      return _interactionStateDao;
    } else {
      synchronized(this) {
        if(_interactionStateDao == null) {
          _interactionStateDao = new InteractionStateDao_Impl(this);
        }
        return _interactionStateDao;
      }
    }
  }
}
