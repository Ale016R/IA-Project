package com.example.myapplicationhardware

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.myapplicationhardware.databinding.ActivityMainBinding
import java.io.File

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import android.graphics.Bitmap
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fotoFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tomarfoto.setOnClickListener {
            val takeIntentPhoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            fotoFile = getFile(FILE_NAME)

            val providerFile = FileProvider.getUriForFile(this, "com.example.camudv.fileprovider", fotoFile)
            takeIntentPhoto.putExtra(MediaStore.EXTRA_OUTPUT, providerFile)
            if (takeIntentPhoto.resolveActivity(this.packageManager) != null) {
                startActivityForResult(takeIntentPhoto, REQUEST)
            } else {
                Toast.makeText(this, "La cámara no está disponible", Toast.LENGTH_LONG).show()
            }
        }

        binding.galeria.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    private fun getFile(fileName: String): File {
        val directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", directory)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST && resultCode == Activity.RESULT_OK) {
            val fotoTomada = BitmapFactory.decodeFile(fotoFile.absolutePath)
            binding.image.setImageBitmap(fotoTomada)
            processImage(fotoTomada)
        } else if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            val imageStream = contentResolver.openInputStream(selectedImageUri!!)
            val selectedImage = BitmapFactory.decodeStream(imageStream)
            binding.image.setImageURI(selectedImageUri)
            processImage(selectedImage)
        }
    }

    private fun processImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                labels.firstOrNull()?.let { label ->
                    val description = getDescription(label.text)
                    updateUIWithLabel(description, label.text)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error en reconocimiento de imagen: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private val speciesDescriptionMap = mapOf(
        "Felino" to listOf("cat", "tiger", "lion", "leopard", "cheetah"),
        "Canino" to listOf("dog", "wolf", "fox", "coyote"),
        "Oso" to listOf("bear", "panda", "grizzly", "polar bear"),
        "Ave" to listOf("bird", "eagle", "parrot", "penguin", "owl"),
        "Pez" to listOf("fish", "salmon", "tuna", "goldfish", "clownfish", "shark"),
        "Cetáceo" to listOf("whale", "dolphin", "orca"),
        "Insecto" to listOf("insect", "butterfly", "bee", "ant", "beetle"),
        "Arácnido" to listOf("spider", "scorpion"),
        "Equino" to listOf("horse", "zebra", "donkey"),
        "Bovino" to listOf("cow", "bull", "buffalo"),
        "Vehículos" to listOf("car", "truck", "motorcycle", "bicycle", "airplane"),
        "Frutas" to listOf("apple", "banana", "orange", "grape", "mango"),
        "Mobiliario" to listOf("chair", "table", "sofa", "desk", "bed"),
        "Tecnología" to listOf("computer", "smartphone", "tablet", "camera", "headphones"),
        "Herramientas" to listOf("hammer", "screwdriver", "wrench", "saw", "drill"),
        "Juguetes" to listOf("doll", "puzzle", "ball", "teddy bear", "lego"),
        "Utensilios de Cocina" to listOf("knife", "fork", "spoon", "plate", "cup"),
        "Ropa" to listOf("shirt", "pants", "jacket", "hat", "socks"),
        "Productos de Limpieza" to listOf("soap", "detergent", "mop", "broom", "sponge"),
        "Equipos Deportivos" to listOf("soccer ball", "tennis racket", "basketball", "golf club", "baseball bat"),
        "Navidad" to listOf("christmas tree", "christmas", "santa claus", "reindeer", "gift", "snowman"),
        "Halloween" to listOf("pumpkin", "ghost", "witch", "bat", "skeleton"),
        "Pascua" to listOf("easter egg", "bunny", "chocolate", "easter basket", "chick"),
        "Día de Acción de Gracias" to listOf("turkey", "cornucopia", "pilgrim", "pumpkin pie", "cranberry sauce"),
    )

    private fun getDescription(label: String): String {

        val labelLower = label.toLowerCase()

        for ((category, labels) in speciesDescriptionMap) {
            if (labelLower in labels) {
                return category
            }
        }
        return "Desconocido"
    }


    private fun updateUIWithLabel(description: String, label: String) {
        val imageResId = when(description) {
            "Felino" -> R.drawable.cat_image
            "Canino" -> R.drawable.dog_image
            "Oso" -> R.drawable.bear_image
            "Oso" -> R.drawable.bear_image
            "Ave" -> R.drawable.bird_image
            "Pez" -> R.drawable.fish_image
            "Cetáceo" -> R.drawable.whale_image
            "Insecto" -> R.drawable.insect_image
            "Arácnido" -> R.drawable.spider_image
            "Equino" -> R.drawable.horse_image
            "Bovino" -> R.drawable.cow_image
            "Vehículos" -> R.drawable.car_image
            "Frutas" -> R.drawable.apple_image
            "Mobiliario" -> R.drawable.chair_image
            "Tecnología" -> R.drawable.computer_image
            "Herramientas" -> R.drawable.hammer_image
            "Juguetes" -> R.drawable.soccer_ball
            "Utensilios de Cocina" -> R.drawable.knife_image
            "Ropa" -> R.drawable.shirt_image
            "Productos de Limpieza" -> R.drawable.soap_image
            "Equipos Deportivos" -> R.drawable.soccer_ball
            "Navidad" -> R.drawable.christmas_image
            "Halloween" -> R.drawable.halloween_image
            "Pascua" -> R.drawable.easter_egg_image
            "Día de Acción de Gracias" -> R.drawable.turkey_image

            else -> R.drawable.default_image
        }

        // Mostrar imagen
        binding.image.setImageResource(imageResId)

        binding.identifiedCategory.text = "Identificado: $description"

        Toast.makeText(this, "Identificado: $description", Toast.LENGTH_LONG).show()
    }

}

private const val REQUEST = 13
private const val GALLERY_REQUEST_CODE = 14
private const val STORAGE_PERMISSION_CODE = 1002
private const val FILE_NAME = "photo.jpg"
