# Portfolzio

My photography portfolio website. Written in Scala with ZIO. See it in action:
[raitis.krikis.id.lv](https://raitis.krikis.id.lv)

## How it works

For each photo you want to display, you create a directory. The directory can be nested within other directories
however deep you like. The directory path (using `/` as the separator character) is that image's ID.
(Image IDs are used for alt text in the html.)
Inside that directory you should have:

- One or more image files, with the name matching the regex `(?i).*\.(jpg|jpeg)$`
- Zero or more raw image files, with the name matching the regex `(?i).*\.(arw|raw)$`
- One UTF-8 encoded text file named `info.json` - a json data structure with image metadata.

Inside any directory you can create text files ending with `.album` which behave as albums. The name of the file
(before .`album`) is the album's ID, and in that text file you can reference images or other albums by their ID
(or relative ID) as a newline separated list. You can use the `*` character to match all images or albums in a
directory. You can also match tags with `tag:example`.

The server observes the directory for changes and refreshes periodically to reflect those changes.
Lower-sized preview images are generated for the images using imagemagick.

## Dependencies

- Use OpenJDK 11
- inotifywatch, available from the `inotify-tools` package, for watching file system changes.
  If you have a photo library with more than 8192 photos, please increase the number of inotify watches
  the system allows to be made by writing to `/proc/sys/fs/inotify/max_user_watches`.
- imagemagick, used for generating preview images
- sha1sum, used for checking if preview images need to be regenerated

## Running in development mode

1. Launch sbt shell
2. `~ reStart`

## Running in production

1. Launch sbt shell
2. `assembly`
3. Copy the generated fat-jar file to your server
4. run it with Java: `java -jar portfolzio.jar`

There is a `portfolzio.service` systemd service file available for copy-pasting into `/etc/systemd/system` to start
the app that way. Use a dedicated webserver like Caddy for HTTPS support.

## info.json example

All fields are optional, and can be omitted.

```json
{
  "description": "",
  "tags": [
    "",
    ""
  ],
  "primaryImageFile": "",
  "time": "2024-01-27T18:20:54",
  "cameraModel": "Sony a7 IV",
  "aperture": "F2.8",
  "focalLength": "300mm",
  "shutterSpeed": "1/60s",
  "iso": "ISO 100"
}
```