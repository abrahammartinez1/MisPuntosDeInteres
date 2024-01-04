package com.example.mymaps


import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymaps.models.Place
import com.example.mymaps.models.UserMap
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.*

const val EXTRA_USER_MAP = "EXTRA_USER_MAP"
const val EXTRA_MAP_TITLE  = "EXTRA_MAP_TITLE"
private const val TAG = "MainActivity"
private const val FILE_NAME = "UserMaps.data"
class MainActivity : AppCompatActivity() {
    private lateinit var rvMaps : RecyclerView
    private lateinit var fabCreateMap : FloatingActionButton
    private lateinit var userMaps: MutableList<UserMap>
    private lateinit var mapAdapter: MapsAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rvMaps = findViewById<RecyclerView>(R.id.rvMaps)
        fabCreateMap = findViewById(R.id.fabCreateMap)
        userMaps = deSerializeUserMaps(this).toMutableList()

        // set Layout Manager
        rvMaps.layoutManager = LinearLayoutManager(this)

        //Set Adapter
        mapAdapter = MapsAdapter(this, userMaps, object: MapsAdapter.OnClickListener{
            override fun onItemClick(position: Int) {
                Log.i(TAG, "on item clicked $position")
                //Navigate to maps activity
                val intent = Intent(this@MainActivity, MapDisplayActivity::class.java)
                intent.putExtra(EXTRA_USER_MAP, userMaps[position])
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        })
        rvMaps.adapter = mapAdapter

        val resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val userMap = result.data?.getSerializableExtra(EXTRA_USER_MAP) as UserMap
                Log.i(TAG, "onActivityResult with new map title ${userMap.title}")
                userMaps.add(userMap)
                mapAdapter.notifyItemInserted(userMaps.size - 1)
                serializeUserMaps(this, userMaps )
            }
        }
        //Add Listener for floating action button
        fabCreateMap.setOnClickListener{
            Log.i(TAG, "Tap on FAB")
            showAlertDialog(resultLauncher)
        }
    }

    private fun showAlertDialog(resultLauncher: ActivityResultLauncher<Intent>) {
        val mapFormView = LayoutInflater.from(this).inflate(R.layout.dialog_create_map, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Nombre del punto")
            .setView(mapFormView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("OK", null)
            .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = mapFormView.findViewById<EditText>(R.id.etMapTitle).text.toString()
            if (title.trim().isEmpty() ) {
                Toast.makeText(
                    this,
                    "Map must have no-empty title",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val intent = Intent(this@MainActivity, CreateMapActivity::class.java)
            intent.putExtra(EXTRA_MAP_TITLE, title)
            resultLauncher.launch(intent)
            dialog.dismiss()
        }
    }

    private fun serializeUserMaps(context: Context, userMaps: List<UserMap>){
        Log.i(TAG, "serializableUserMaps")
        ObjectOutputStream(FileOutputStream(getDataFile(context))).use { it.writeObject(userMaps) }
    }

    private fun deSerializeUserMaps(context: Context) : List<UserMap>{
        Log.i(TAG, "deSerializeUserMaps")
        val dataFile = getDataFile(context)
        if(!dataFile.exists()){
            Log.i(TAG, "Data file does not exists")
            return emptyList()
        }
        ObjectInputStream(FileInputStream(dataFile)).use{ return it.readObject() as List<UserMap>}
    }
    private fun getDataFile(context: Context): File {
        Log.i(TAG, "Recuperando fichero del directorio ${context.filesDir}")
        return File(context.filesDir, FILE_NAME)
    }

}