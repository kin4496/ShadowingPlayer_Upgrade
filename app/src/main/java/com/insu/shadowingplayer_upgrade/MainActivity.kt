package com.insu.shadowingplayer_upgrade

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.insu.shadowingplayer_upgrade.ui.dashboard.VideoData
import com.insu.shadowingplayer_upgrade.ui.home.AudioData
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
private const val REQUEST_READ_EXTERNAL_STORAGE=1000
class MainActivity : AppCompatActivity() {
    companion object{
        var videos=mutableListOf<VideoData>()
        var audios=mutableListOf<AudioData>()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // 권한이 허용되지 않음 ②
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // 이전에 이미 권한이 거부되었을 때 설명 ③
                alert("동영상 정보를 얻기 위해서는 외부 저장소 권한이 필수로 필요합니다", "권한이 필요한 이유") {
                    yesButton {
                        // 권한 요청
                        ActivityCompat.requestPermissions(this@MainActivity,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            REQUEST_READ_EXTERNAL_STORAGE)
                    }
                    noButton { }
                }.show()
            } else {
                // 권한 요청 ④
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_READ_EXTERNAL_STORAGE)
            }
        }else{
            getAllVideo()
            getAllAudio()
        }
    }
    private fun getAllAudio(){
        val projection=arrayOf(MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION)
        val sortOrder="${MediaStore.Audio.Media.TITLE} DESC"
        val cursor=contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder)
        if(cursor!=null){
            while(cursor.moveToNext()){
                var title=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                var id:Long=cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                var duration:String=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                var uri = Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                var path=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                audios.add(AudioData(title,duration,uri,path))
            }
        }
        cursor?.close()
    }
    private fun getFolderName(uri:Uri):String{
        val proj = arrayOf(MediaStore.Images.Media.DATA)

        val cursor: Cursor? = uri?.let{contentResolver.query(it, proj, null, null, null)}
        cursor!!.moveToNext()
        val path: String =
            cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))

        var pos=path.lastIndexOf('/')
        var temp=path.slice(0 until pos)
        pos=temp.lastIndexOf('/')

        var folderName=if(pos==0||pos==-1){
            "."
        }else{
            temp.slice(pos until temp.length)
        }
        cursor.close()
        return folderName
    }
    private fun getAllVideo(){
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME
        )
        val cursor=contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null)
        if(cursor!=null){
            while(cursor.moveToNext()){
                var title:String=cursor.getString(1)
                var id:Long=cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media._ID))

                /*
                api 29부터 resolver로 썸네일 가져오는 것이 좋다고 개발자 문서에 있음
                This class was deprecated in API level 29.
                Callers should migrate to using ContentResolver#loadThumbnail,
                since it offers richer control over requested thumbnail sizes and cancellation behavior.
                */

                var bitmap: Bitmap = MediaStore.Video.Thumbnails.getThumbnail(contentResolver,
                    id, MediaStore.Video.Thumbnails.MINI_KIND,null)
                bitmap=Bitmap.createScaledBitmap(bitmap,300,200,true)

                var uri = Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                var temp=VideoData(title,bitmap,uri,getFolderName(uri!!))
                videos.add(temp)
            }
        }
        cursor?.close()
    }
}