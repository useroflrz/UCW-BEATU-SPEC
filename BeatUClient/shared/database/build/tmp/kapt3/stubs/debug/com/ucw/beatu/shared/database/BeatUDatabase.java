package com.ucw.beatu.shared.database;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \t2\u00020\u0001:\u0001\tB\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\bH&\u00a8\u0006\n"}, d2 = {"Lcom/ucw/beatu/shared/database/BeatUDatabase;", "Landroidx/room/RoomDatabase;", "()V", "commentDao", "Lcom/ucw/beatu/shared/database/dao/CommentDao;", "interactionStateDao", "Lcom/ucw/beatu/shared/database/dao/InteractionStateDao;", "videoDao", "Lcom/ucw/beatu/shared/database/dao/VideoDao;", "Companion", "database_debug"})
@androidx.room.Database(entities = {com.ucw.beatu.shared.database.entity.VideoEntity.class, com.ucw.beatu.shared.database.entity.CommentEntity.class, com.ucw.beatu.shared.database.entity.InteractionStateEntity.class}, version = 1, exportSchema = true)
@androidx.room.TypeConverters(value = {com.ucw.beatu.shared.database.converter.Converters.class})
public abstract class BeatUDatabase extends androidx.room.RoomDatabase {
    @org.jetbrains.annotations.NotNull()
    public static final com.ucw.beatu.shared.database.BeatUDatabase.Companion Companion = null;
    
    public BeatUDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.ucw.beatu.shared.database.dao.VideoDao videoDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.ucw.beatu.shared.database.dao.CommentDao commentDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.ucw.beatu.shared.database.dao.InteractionStateDao interactionStateDao();
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b\u00a8\u0006\t"}, d2 = {"Lcom/ucw/beatu/shared/database/BeatUDatabase$Companion;", "", "()V", "build", "Lcom/ucw/beatu/shared/database/BeatUDatabase;", "context", "Landroid/content/Context;", "dbName", "", "database_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ucw.beatu.shared.database.BeatUDatabase build(@org.jetbrains.annotations.NotNull()
        android.content.Context context, @org.jetbrains.annotations.NotNull()
        java.lang.String dbName) {
            return null;
        }
    }
}