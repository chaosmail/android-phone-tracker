# Android PhoneTracker (Kotlin)

This is an application that tracks the GPS location, signal strength of all surrounding cells (if available, see https://stackoverflow.com/a/18688268/5200303) and device identifiers (imsi, imei, androidId, etc.) of an Android device and ingests the data to a Microsoft Azure EventHub instance.

## Connect your Azure Account

Create a file `res/values/secrets.xml` with the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="azure_namespace">xxx</string>
    <string name="azure_event_hub">xxx</string>
    <string name="azure_sas_key_name">xxx</string>
    <string name="azure_sas_key">xxx</string>
</resources>
```

## LICENSE

This software is provided under MIT license
