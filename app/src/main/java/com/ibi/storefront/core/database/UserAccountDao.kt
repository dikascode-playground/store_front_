package com.ibi.storefront.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: UserAccountEntity): Long
}
