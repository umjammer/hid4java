[![Release](https://jitpack.io/v/umjammer/hid4java.svg)](https://jitpack.io/#umjammer/hid4java)
[![Java CI](https://github.com/umjammer/hid4java/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/hid4java/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/hid4java/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/hid4java/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# hid4java

## Install

 * [maven](https://jitpack.io/#umjammer/hid4java)

## TODO

 * ~~sigabrt~~ ... `-Djbr.catch.SIGABRT=true` (maybe only for jetbrain's jvm)
 * ~~npe on close~~

## LESSON

 * `sigabrt` `__pthread_kill+0x8` ... suspect using java heap as native memory, like using `byte[]` for a parameter type should be `Pointer`
---

## Original

The `hid4java` project supports USB HID devices through a common API which is provided here under the MIT license. The API is very simple but provides great flexibility such as support for feature reports and blocking reads with timeouts. Attach/detach events are provided to allow applications to respond instantly to device availability.

## Telegram group

If you want to discuss `hid4java` in general please use [the Telegram chat](https://t.me/joinchat/CtU4ZBltWCAFBAjwM5KLLw). I can't guarantee
an instant response but I'm usually active on Telegram during office hours in the GMT timezone.

Remember to [check the Wiki first](https://github.com/gary-rowe/hid4java/wiki/Home) before asking questions to avoid causing frustration!

## Technologies

* ~~[hidapi](https://github.com/libusb/hidapi) - Native USB HID library for multiple platforms~~
* [JNA](https://github.com/twall/jna) - to remove the need for Java Native Interface (JNI) and greatly simplify the project
* ~~[dockcross](https://github.com/dockcross/dockcross) - Cross-compilation environments for multiple platforms to create hidapi libraries~~
* Java 17+ - to remove dependencies on JVMs that have reached end of life

## Install

* https://jitpack.io/#umjammer/hid4java

## Code example

Taken from [UsbHidEnumerationExample](https://github.com/gary-rowe/hid4java/blob/develop/src/test/java/org/hid4java/examples/UsbHidEnumerationExample.java) which
provides more details. 

```java
// Configure to use custom specification
HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();

// Use the v0.7.0 manual start feature to get immediate attach events
hidServicesSpecification.setAutoStart(false);

// Get HID services using custom specification
HidServices hidServices = HidManager.getHidServices(hidServicesSpecification);
hidServices.addHidServicesListener(this);

// Manually start the services to get attachment event
hidServices.start();

// Provide a list of attached devices
for (HidDevice hidDevice : hidServices.getAttachedHidDevices()) {
  System.out.println(hidDevice);
}
    
```

## More information

Much of the information previously in this README has been migrated to the [project Wiki](https://github.com/gary-rowe/hid4java/wiki/Home) as it was getting rather long. Here are some useful jumping off points that should help:

* [Home](https://github.com/gary-rowe/hid4java/wiki/Home) - The wiki Home page with lots of useful launch points
* [FAQ](https://github.com/gary-rowe/hid4java/wiki/FAQ) - Frequently asked questions
* [Examples](https://github.com/gary-rowe/hid4java/wiki/Examples) - Using the examples to kickstart your own project
* [Troubleshooting](https://github.com/gary-rowe/hid4java/wiki/Troubleshooting) - A comprehensive troubleshooting guide

## Closing notes

All trademarks and copyrights are acknowledged.

Many thanks to `victorix` who provided the basis for this library. Please [see the inspiration on the mbed.org site](http://developer.mbed.org/cookbook/USBHID-bindings-).

Thanks also go to everyone who has contributed their knowledge and advice during the creation and subsequent improvement of this library.
