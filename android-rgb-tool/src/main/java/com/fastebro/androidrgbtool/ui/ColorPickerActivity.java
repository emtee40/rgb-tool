package com.fastebro.androidrgbtool.ui;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.graphics.Palette;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.fastebro.androidrgbtool.R;
import com.fastebro.androidrgbtool.model.PaletteSwatch;
import com.fastebro.androidrgbtool.utils.UImage;
import com.fastebro.androidrgbtool.widgets.RGBPanelData;
import uk.co.senab.photoview.PhotoViewAttacher;

import java.io.File;
import java.util.ArrayList;


public class ColorPickerActivity extends BaseActivity {
    private ImageView imageView;
    private PhotoViewAttacher attacher;
    private Bitmap bitmap;
    View.OnTouchListener imgSourceOnTouchListener;
    private RGBPanelData rgbPanelDataLayout;
    private RelativeLayout mainLayout;

    private String currentPath = null;
    private boolean deleteFile = false;
    private final static float PHOTO_SCALING_FACTOR = 3.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_picker);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mainLayout = (RelativeLayout) findViewById(R.id.color_picker_main_layout);

        rgbPanelDataLayout = new RGBPanelData(this);
        rgbPanelDataLayout.setVisibility(View.GONE);

        if (getIntent() != null) {
            // get the path of the image and set it.
            Bundle bundle = getIntent().getExtras();

            if (bundle != null) {
                currentPath = bundle.getString(UImage.EXTRA_JPEG_FILE_PATH);
                deleteFile = bundle.getBoolean(UImage.EXTRA_DELETE_FILE);
            }

            if (currentPath != null) {
                imageView = (ImageView) findViewById(R.id.iv_photo);

                try {
                    bitmap = BitmapFactory.decodeFile(currentPath);
                    imageView.setImageBitmap(bitmap);
                    imageView.setOnTouchListener(imgSourceOnTouchListener);
                    attacher = new PhotoViewAttacher(imageView);
                    attacher.setMaximumScale(attacher.getMaximumScale() * PHOTO_SCALING_FACTOR);
                    attacher.setOnViewTapListener(new PhotoViewTapListener());
                    attacher.setOnPhotoTapListener(new PhotoViewTapListener());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        mainLayout.addView(rgbPanelDataLayout, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onDestroy() {
        if (deleteFile) {
            //noinspection ResultOfMethodCallIgnored
            new File(currentPath).delete();
            getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.DATA + "=?", new String[]{currentPath});
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.color_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAfterTransition();
            } else {
                finish();
            }
            return true;
        } else if(item.getItemId() == R.id.action_palette) {
            generatePalette();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void generatePalette() {
        if (bitmap != null) {
            Palette.Builder paletteBuilder = Palette.from(bitmap);
            paletteBuilder.generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    Intent intent = new Intent(ColorPickerActivity.this, ImagePaletteActivity.class);

                    ArrayList<PaletteSwatch> swatches = new ArrayList<PaletteSwatch>();

                    if(palette.getVibrantSwatch() != null) {
                        swatches.add(new PaletteSwatch(palette.getVibrantSwatch().getRgb(),
                                PaletteSwatch.SwatchType.VIBRANT));
                    }

                    if(palette.getMutedSwatch() != null) {
                        swatches.add(new PaletteSwatch(palette.getMutedSwatch().getRgb(),
                                PaletteSwatch.SwatchType.MUTED));
                    }

                    if(palette.getLightVibrantSwatch() != null) {
                        swatches.add(new PaletteSwatch(palette.getLightVibrantSwatch().getRgb(),
                                PaletteSwatch.SwatchType.LIGHT_VIBRANT));
                    }

                    if(palette.getLightMutedSwatch() != null) {
                        swatches.add(new PaletteSwatch(palette.getLightMutedSwatch().getRgb(),
                                PaletteSwatch.SwatchType.LIGHT_MUTED));
                    }

                    if(palette.getDarkVibrantSwatch() != null) {
                        swatches.add(new PaletteSwatch(palette.getDarkVibrantSwatch().getRgb(),
                                PaletteSwatch.SwatchType.DARK_VIBRANT));
                    }

                    if(palette.getDarkMutedSwatch() != null) {
                        swatches.add(new PaletteSwatch(palette.getDarkMutedSwatch().getRgb(),
                                PaletteSwatch.SwatchType.DARK_MUTED));
                    }

                    intent.putParcelableArrayListExtra(ImagePaletteActivity.EXTRA_SWATCHES, swatches);
                    intent.putExtra(ImagePaletteActivity.FILENAME, Uri.parse(currentPath).getLastPathSegment());

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivity(intent,
                                ActivityOptions.makeSceneTransitionAnimation(ColorPickerActivity.this).toBundle());
                    } else {
                        startActivity(intent);
                    }
                }
            });
        }
    }

    private class PhotoViewTapListener
            implements PhotoViewAttacher.OnViewTapListener,
            PhotoViewAttacher.OnPhotoTapListener {
        @Override
        public void onViewTap(View view, float x, float y) {
            // Not being used so far.
        }

        @Override
        public void onPhotoTap(View view, float x, float y) {
            // x and y represent the percentage of the Drawable where the user clicked.
            int imageX = (int) (x * bitmap.getWidth());
            int imageY = (int) (y * bitmap.getHeight());

            int touchedRGB = bitmap.getPixel(imageX, imageY);


            if (imageY < bitmap.getHeight() / 2) {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                rgbPanelDataLayout.setLayoutParams(params);

                rgbPanelDataLayout.updateData(touchedRGB);

                if (rgbPanelDataLayout.getVisibility() == View.GONE) {
                    rgbPanelDataLayout.setVisibility(View.VISIBLE);
                }
            } else {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                rgbPanelDataLayout.setLayoutParams(params);

                rgbPanelDataLayout.updateData(touchedRGB);

                if (rgbPanelDataLayout.getVisibility() == View.GONE) {
                    rgbPanelDataLayout.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
    }
}
