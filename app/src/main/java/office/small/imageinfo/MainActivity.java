package office.small.imageinfo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * 1. Написать код, используя rxjava, который читает файл-картинку формата jpg из файловой системы,
 *    затем конвертирует ее png и записывает обратно в файловую систему. Чтение и запись должна
 *    производиться не в UI потоке. Использовать lambda expressions везде, где возможно.
 *
 *  TODO: 2. *Добавить в предыдущем примере возможность отказаться от проведения операции, выведя
 *  TODO:  после начала в UI поток диалоговое окно с надписью “Выполняется конвертация” и кнопкой “Отменить”.
 *  TODO:  Если понадобится, для того, чтобы замедлить процессы в фоновом потоке, использовать метод sleep().
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    /** ButterKnife Code **/
    @BindView(R.id.txtSDK)
    TextView txtSDK;
    @BindView(R.id.btnSelectImage)
    Button btnSelectImage;
    @BindView(R.id.txtUriPath)
    TextView txtUriPath;
    @BindView(R.id.txtRealPath)
    TextView txtRealPath;
    @BindView(R.id.imgView)
    ImageView imgView;
    /** ButterKnife Code **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // add click listener to button
        btnSelectImage.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View view) {
        // ask to permissions
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant that should be quite unique
        }

        // 1. on Upload click call ACTION_GET_CONTENT intent
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // 2. pick image only
        intent.setType("image/*");
        // 3. start activity
        startActivityForResult(intent, 0);

        // define onActivityResult to do something with picked image
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        if(resCode == Activity.RESULT_OK && data != null){
            String realPath;
            // SDK < API11
            if (Build.VERSION.SDK_INT < 11)
                realPath = RealPathUtil.getRealPathFromURI_BelowAPI11(this, data.getData());
            // SDK >= 11 && SDK < 19
            else if (Build.VERSION.SDK_INT < 19)
                realPath = RealPathUtil.getRealPathFromURI_API11to18(this, data.getData());
            // SDK > 19 (Android 4.4)
            else
                realPath = RealPathUtil.getRealPathFromURI_API19(this, data.getData());
            setTextViews(Build.VERSION.SDK_INT, data.getData().getPath(),realPath);
        }
    }

    private void setTextViews(int sdk, String uriPath, String realPath){

        this.txtSDK.setText("Build.VERSION.SDK_INT: "+sdk);
        this.txtUriPath.setText("URI Path: "+uriPath);
        this.txtRealPath.setText("Real Path: "+realPath);


        Uri uriFromPath = Uri.fromFile(new File(realPath));
        // you have two ways to display selected image
        // ( 1 ) imageView.setImageURI(uriFromPath);
        // ( 2 ) imageView.setImageBitmap(bitmap);

        Observable<Bitmap> oImgPng = Observable.just(uriFromPath)
                .flatMap((Function<Uri, ObservableSource<Bitmap>>) uri -> {
                    Bitmap bmpjpg = null;
                    Observable<Bitmap> obBitmap = null;
                    try {
                        bmpjpg = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                        obBitmap = Observable.just(codec(bmpjpg, Bitmap.CompressFormat.PNG, 0));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    return  obBitmap;
                })
                .subscribeOn(Schedulers.io());

        Disposable dImg = oImgPng
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Bitmap>() {
                @Override
                public void onNext(Bitmap bmp) {
                    imgView.setImageBitmap(bmp);
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onComplete() {

                }
        });
        dImg.dispose();

        Log.d("INFMODE", "Build.VERSION.SDK_INT:"+sdk);
        Log.d("INFMODE", "URI Path:"+uriPath);
        Log.d("INFMODE", "Real Path: "+realPath);


    }

    private static Bitmap codec(Bitmap src, Bitmap.CompressFormat format,
                                int quality) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        src.compress(format, quality, os);

        byte[] array = os.toByteArray();
        return BitmapFactory.decodeByteArray(array, 0, array.length);
    }

}

