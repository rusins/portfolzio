# Portfolzio

My photography portfolio portfolzio.website. Written in Scala with ZIO.

## How it works
For each photo you want to display, you create a directory. The directory can be nested within other directories
however deep you like. The directory path (using `/` as the separator character) is that image's UID.
Inside that directory you should have:
- One or more image files, with the name matching the regex `(?i)\.(jpg|jpeg)$`
- Zero or more raw image files, with the name matching the regex `(?i)\.(arw|raw)$`
- One UTF-8 encoded text file named `info.json` - a json data structure with image metadata.
- One UTF-8 encoded text file named `tags.txt` - a newline separated list of categories the image belongs to.

Inside any directory you can create text files which behave as albums. The name of the file is the
album's UID, and in that text file you can reference images or other albums by their UID (or relative UID)
as a newline separated list. You can use the `*` character to match all images or albums in a directory.

The server observes the directory for changes and refreshes periodically to reflect those changes.
Lower-sized preview images are generated for the images using ???

## Dependencies
- inotifywatch, available from the `inotify-tools` package, for watching file system changes.
  If you have a photo library with more than 8192 photos, please increase the number of inotify watches
  the system allows to be made by writing to `/proc/sys/fs/inotify/max_user_watches`.

## Running in development mode

1. Launch sbt shell
2. `~ reStart`