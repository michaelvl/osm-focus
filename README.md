# osm-focus
OSMFocus Android application - OpenStreetMap Data for Android

**This version of OSMfocus is no longer being maintained - see [https://github.com/ubipo/osmfocus](https://github.com/ubipo/osmfocus).**

![Image](images/featuregfx.png?raw=true)

OSMfocus can show details of nearby objects from the OpenStreetMap database such
that they can be compared with real world observations. Correcting errors or
adding missing information to OpenStreetMap and thus getting it 'into focus' is
the main purpose of OSMfocus. OSMfocus is not a map, navigation tool or
OpenStreetMap editor.

Discrepancies between the real-world and OpenStreetMap are best observed on-site
while memorizing details for later comparison with OpenStreetMap is usually
difficult and with a good likelihood of missing the actual differences.

![Image](images/screen01.png?raw=true)
![Image](images/screen02.png?raw=true)
![Image](images/screen03.png?raw=true)
![Image](images/screen04.png?raw=true)

OSMfocus shows key-value pairs as = and abbreviate two pairs that occur quite
often, namely highway= and name= which are shown as :. Way objects which has
just a single key-value pair of type 'building=*' are ignored when searching for
nearby objects. To make better utilization of screen space, the following keys
are not shown: 'kms:*', 'osak:*', 'created_by', 'addr:country', 'addr:postcode'
and 'source'.

OSMfocus use location services and network access permissions for downloading
vector map data and background map tiles for your current location.

OSMfocus use data from OpenStreetMap (www.openstreetmap.org). This data and
screenshots containing maps are (C) Copyright OpenSteetMap contributors.

[OSMfocus on Google play](https://play.google.com/store/apps/details?id=dk.network42.osmfocus)

# About the Name OSMfocus

The thinking behind the application name is that the application will help
"sharpening" the OpenStreetMap data such that it represents a more clear view of
the observable world.