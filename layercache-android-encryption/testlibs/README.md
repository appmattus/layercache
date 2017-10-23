# JUnitParams-1.1.0-custom.jar

v1.1.0 has issues running on Android, so this custom version contains two changes to ensure it works correctly from API
17 and up.

## ParameterisedTestMethodRunner:currentTestDescription

This method has been changed to the following due to it crashing with ArrayIndexOutOfBounds on all Android versions:

```java
Description currentTestDescription() {
    try {
        return method.description().getChildren().get(count - 1);
    } catch (IndexOutOfBoundsException ex) {
        ex.printStackTrace();
        return null;
    }
}

```

## Exception before running instrumentation tests on Android 19 and below

As per [issue 134](https://github.com/Pragmatists/JUnitParams/issues/134) JUnitParams fails on Android API 19 and below
due to the look-back in the RegEx.

A [PR](https://github.com/Pragmatists/JUnitParams/pull/139) is submitted to fix this issue.
