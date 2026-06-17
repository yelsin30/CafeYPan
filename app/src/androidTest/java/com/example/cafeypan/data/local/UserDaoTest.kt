package com.example.cafeypan.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cafeypan.data.local.dao.UserDao
import com.example.cafeypan.data.local.entity.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var userDao: UserDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetUser() = runBlocking {
        val user = UserEntity(
            id = 1,
            nombre = "Carlos",
            rol = "Panadero",
            pin = "4321",
            activo = true
        )
        userDao.insertUser(user)
        val fetchedUser = userDao.getUserByPin("4321")
        assertNotNull(fetchedUser)
        assertEquals("Carlos", fetchedUser?.nombre)
        assertEquals("Panadero", fetchedUser?.rol)
    }

    @Test
    fun deleteUser() = runBlocking {
        val user = UserEntity(
            id = 2,
            nombre = "Ana",
            rol = "Barista",
            pin = "1111",
            activo = true
        )
        userDao.insertUser(user)
        userDao.deleteUser(user)
        val fetchedUser = userDao.getUserByPin("1111")
        assertNull(fetchedUser)
    }
}
