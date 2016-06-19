package ele.me.buttercookie;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.examples.appmodules.ModulesExampleActivity;
import me.ele.buttercookie.LibraryActivity;

public class MainActivity extends AppCompatActivity {

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
  }

  @OnClick(R.id.test_library) void launchLibraryActivity() {
    Toast.makeText(MainActivity.this, "launch library activity", Toast.LENGTH_SHORT).show();
    startActivity(new Intent(this, LibraryActivity.class));
  }

  @OnClick(R.id.test_realm) void gotoRealmTestActivity() {
    startActivity(new Intent(this, ModulesExampleActivity.class));
  }
}
