# ButterCookie


 [ButterKnife](https://github.com/JakeWharton/butterknife) is a great DI library,  it injects resources, widgets, and event response listeners so simple & convenient. But sadly it cann't be used in library project, this really sucks, because library's R fields are all non-final. java annotation's value must be some constant value, that is to say, must be with **final**.

**ButterCookie** is just for this!!! It makes ButterKnife can be used in library as fine as in android application project. This is really cool, aha? Its usage is far too easy, just apply it as a gradle plugin, then you are good to go as usual.


## Usage

```groovy
dependencies {
   ...
   classpath 'me.ele:buttercookie-plugin:1.0.0'
 }
  
apply plugin: 'com.android.application'
apply plugin: 'me.ele.buttercookie'

```

## feel free to use, welcome issue and comment


