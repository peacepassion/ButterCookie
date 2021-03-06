package ele.me.buttercookie;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ele.buttercookie.LibraryActivity;

public class MainActivity extends AppCompatActivity {

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
  }

  @OnClick(R.id.btn2) void toast() {
    Toast.makeText(MainActivity.this, "hah2", Toast.LENGTH_SHORT).show();
    startActivity(new Intent(this, LibraryActivity.class));
  }
}
