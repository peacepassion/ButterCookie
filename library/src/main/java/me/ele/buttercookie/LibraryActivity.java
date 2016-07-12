package me.ele.buttercookie;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ele.library.R;

public class LibraryActivity extends AppCompatActivity {

  @BindView(R.id.btn1)
  public Button button;

  @BindString(R.string.app_name)
  public String str;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_library);
    ButterKnife.bind(this);

    Log.e("test", "button--->" + button);
    Log.e("test", "str--->" + str);
  }

  @OnClick(value = { R.id.textview1, R.id.btn1,
      R.id.btn2, R.id.btn3 }) public void click() {
    Toast.makeText(LibraryActivity.this, "haha", Toast.LENGTH_SHORT).show();
  }

  @OnClick(R.id.btn4) public void onClickTestChinese() {
    Toast.makeText(LibraryActivity.this, "请选择颜色", Toast.LENGTH_SHORT).show();
  }

  public class InnerClass2 extends View {

    @BindString(R.string.app_name)
    public String str;

    public InnerClass2(Context context) {
      super(context);
    }

    public InnerClass2(Context context, AttributeSet attrs) {
      super(context, attrs);
    }

    public InnerClass2(Context context, AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public InnerClass2(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
      super(context, attrs, defStyleAttr, defStyleRes);
    }
  }
}
