package com.magicsurvivor.screens;

import android.app.Activity;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.magicsurvivor.R;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class LevelSelect extends AppCompatActivity implements MapAdapter.OnMapSelectedListener {
    private RecyclerView mapRecyclerView;
    private MapAdapter mapAdapter;
    private Button playButton;
    private String money;
    private int selectedMapIndex = -1;
    private List<MapItem> maps;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_level_select);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mapRecyclerView = findViewById(R.id.mapRecyclerView);
        playButton = findViewById(R.id.playButton);

        // Get money from intent
        Intent intent = getIntent();
        money = intent.getStringExtra("money");

        // Setup RecyclerView with horizontal layout
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mapRecyclerView.setLayoutManager(layoutManager);

        // Create list of maps
        maps = createMapList();

        // Setup adapter
        mapAdapter = new MapAdapter(this, maps, this);
        mapRecyclerView.setAdapter(mapAdapter);

        // Initialize activity result launchers
        initializeActivityResultLaunchers();

        // Play button click listener
        playButton.setOnClickListener(v -> onPlayClicked(v));
    }

    private void initializeActivityResultLaunchers() {
        // Gallery picker launcher
        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        Log.d("PhotoPicker", "Selected URI: " + uri);
                        Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                        // Replace the upload map item with the selected image
                        updateMapWithImage(uri);
                    } else {
                        Log.d("PhotoPicker", "No media selected");
                    }
                });

        // Camera launcher - uses TakePicture contract for FULL resolution
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && photoUri != null) {
                        Log.d("CameraCapture", "Photo captured at: " + photoUri);
                        Toast.makeText(this, "Photo captured successfully", Toast.LENGTH_SHORT).show();
                        // Update map with full resolution image
                        updateMapWithImage(photoUri);
                    } else {
                        Log.d("CameraCapture", "Photo capture cancelled");
                    }
                });
    }

    private void updateMapWithImage(Uri imageUri) {
        if (selectedMapIndex >= 0 && selectedMapIndex < maps.size()) {
            try {
                // Compress the image to prevent memory issues
                Uri compressedUri = compressImage(imageUri);
                if (compressedUri != null) {
                    MapItem uploadMap = maps.get(selectedMapIndex);
                    uploadMap.setImageUri(compressedUri);
                    mapAdapter.notifyItemChanged(selectedMapIndex);
                }
            } catch (Exception e) {
                Log.e("ImageCompress", "Error compressing image", e);
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private Uri compressImage(Uri imageUri) {
        try {
            // Decode the image with reduced size sampling
            java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inSampleSize = 2; // Initial downscaling to reduce memory
            android.graphics.Bitmap originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            if (originalBitmap == null) return imageUri;

            // Fix orientation from EXIF data
            originalBitmap = fixImageOrientation(imageUri, originalBitmap);

            // Further compress if image is very large
            if (originalBitmap.getWidth() > 2000 || originalBitmap.getHeight() > 2000) {
                originalBitmap = compressBitmap(originalBitmap, 0.7f);
            }

            // Save compressed bitmap with lower quality (60%) to prevent crashes
            return saveBitmapToUri(originalBitmap, 60);
        } catch (Exception e) {
            Log.e("ImageCompress", "Error in compressImage", e);
            // Fallback to original URI if compression fails
            return imageUri;
        }
    }

    private android.graphics.Bitmap fixImageOrientation(Uri imageUri, android.graphics.Bitmap bitmap) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return bitmap;
            
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            inputStream.close();

            android.graphics.Matrix matrix = new android.graphics.Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.preScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.preScale(1, -1);
                    break;
                default:
                    return bitmap;
            }
            return android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            Log.e("FixOrientation", "Error fixing orientation", e);
            return bitmap;
        }
    }

    private android.graphics.Bitmap compressBitmap(android.graphics.Bitmap bitmap, float scale) {
        int newWidth = (int) (bitmap.getWidth() * scale);
        int newHeight = (int) (bitmap.getHeight() * scale);
        return android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private Uri saveBitmapToUri(android.graphics.Bitmap bitmap, int quality) {
        try {
            java.io.File cacheDir = getCacheDir();
            java.io.File imageFile = new java.io.File(cacheDir, "temp_photo_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, fos);
            fos.close();
            return android.net.Uri.fromFile(imageFile);
        } catch (Exception e) {
            Log.e("BitmapSave", "Error saving bitmap", e);
            return null;
        }
    }

    private Uri saveBitmapToUri(android.graphics.Bitmap bitmap) {
        return saveBitmapToUri(bitmap, 60);
    }

    private List<MapItem> createMapList() {
        List<MapItem> maps = new ArrayList<>();
        maps.add(new MapItem("Shuffle", R.drawable.shuffle, "choose a random map"));
        maps.add(new MapItem("Forest", R.drawable.game_background, "Map 1"));
        maps.add(new MapItem("Volcano", R.drawable.map2, "Map 2"));
        maps.add(new MapItem("Ice Cavern", R.drawable.map5, "Map 3"));
        maps.add(new MapItem("Dark Temple", R.drawable.map6, "Map 4"));
        maps.add(new MapItem("Upload", R.drawable.upload, "choose an image"));
        return maps;
    }

    @Override
    public void onMapSelected(int position) {
        selectedMapIndex = position;
        int selectedMapDrawable = maps.get(position).getImageResId();

        if(selectedMapIndex != maps.size() - 1) return;
        
        // Show upload dialog
        View dialog = getLayoutInflater().inflate(R.layout.upload_dialoge_custom, null);

        Button closeBtn = dialog.findViewById(R.id.closeBtn);
        LinearLayout gallery = dialog.findViewById(R.id.layoutGallery);
        LinearLayout camera = dialog.findViewById(R.id.layoutCamera);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialog);
        AlertDialog dialogAlert = builder.create();

        closeBtn.setOnClickListener(view1 -> dialogAlert.dismiss());
        gallery.setOnClickListener(view1 -> {
            openGallery();
            dialogAlert.dismiss();
        });
        camera.setOnClickListener(view1 -> {
            takePhoto();
            dialogAlert.dismiss();
        });

        dialogAlert.show();
    }

    private void openGallery() {
        pickMediaLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create a file for the camera to save the full resolution photo
            photoUri = createImageFile();
            if (photoUri != null) {
                takePictureLauncher.launch(photoUri);
            } else {
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String imageFileName = "PHOTO_" + timeStamp;
            File storageDir = getCacheDir();
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);
            // Return a file:// URI wrapped in FileProvider
            return FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", image);
        } catch (Exception e) {
            Log.e("CreateImageFile", "Error creating image file", e);
            return null;
        }
    }

    public void upload() {
        // This method is kept for backward compatibility but not used
        // Use openGallery() instead
    }

    public void onPlayClicked(View view) {
        if (selectedMapIndex == -1) {
            // No map selected, show message or disable button
            Toast.makeText(this, "select a map", Toast.LENGTH_SHORT).show();
            return;
        }

        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        pulse.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = new Intent(LevelSelect.this, MainActivity.class);
                intent.putExtra("money", money);
                // Pass userId to MainActivity so it can be used for Firebase operations
                String userId = getIntent().getStringExtra("userId");
                if (userId != null) {
                    intent.putExtra("userId", userId);
                }
                
                if(selectedMapIndex == 0) {//shuffle
                    Random rnd = new Random();
                    selectedMapIndex = rnd.nextInt(maps.size() - 1); // Exclude upload option
                }
                
                // Pass the map data to MainActivity
                MapItem selectedMap = maps.get(selectedMapIndex);
                if (selectedMap.isCustomImage() && selectedMap.getImageUri() != null) {
                    intent.putExtra("selectedMapUri", selectedMap.getImageUri().toString());
                } else {
                    intent.putExtra("selectedMap", selectedMap.getImageResId());
                }
                
                startActivity(intent);
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        view.startAnimation(pulse);
    }

    public void returnn(View view) {
        Intent intent = new Intent(LevelSelect.this, MainMenu.class);
        intent.putExtra("money", money);
        // Pass userId to MainMenu so it can refresh money from Firebase
        String userId = getIntent().getStringExtra("userId");
        if (userId != null) {
            intent.putExtra("userId", userId);
        }
        startActivity(intent);
        finish();
    }

}
