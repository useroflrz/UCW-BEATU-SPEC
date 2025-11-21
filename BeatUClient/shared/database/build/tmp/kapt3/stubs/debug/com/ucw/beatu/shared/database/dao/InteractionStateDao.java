package com.ucw.beatu.shared.database.dao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\bg\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00040\u00032\u0006\u0010\u0005\u001a\u00020\u0006H\'J\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\n\u00a8\u0006\u000b"}, d2 = {"Lcom/ucw/beatu/shared/database/dao/InteractionStateDao;", "", "observe", "Lkotlinx/coroutines/flow/Flow;", "Lcom/ucw/beatu/shared/database/entity/InteractionStateEntity;", "videoId", "", "upsert", "", "state", "(Lcom/ucw/beatu/shared/database/entity/InteractionStateEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "database_debug"})
@androidx.room.Dao()
public abstract interface InteractionStateDao {
    
    @androidx.room.Query(value = "SELECT * FROM interaction_state WHERE videoId = :videoId")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.ucw.beatu.shared.database.entity.InteractionStateEntity> observe(@org.jetbrains.annotations.NotNull()
    java.lang.String videoId);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object upsert(@org.jetbrains.annotations.NotNull()
    com.ucw.beatu.shared.database.entity.InteractionStateEntity state, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}