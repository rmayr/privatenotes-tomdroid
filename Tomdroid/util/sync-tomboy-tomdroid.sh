#!/bin/bash
#
# Loads all the notes from ~/.tomboy/ into android's emulator sdcard in /sdcard//tomdroid/
# @Author: Olivier Bilodeau <olivier@bottomlesspit.org>

#original path:
#~/.tomboy/
#fixed (for windows with cygwin) path:
#~/AppData/Roaming/Tomboy/notes/

#fix for the push command:
#adb push $(cygpath -aw $note) /sdcard/tomdroid
#original
#adb push $note /sdcard/tomdroid/

for note in `find ~/AppData/Roaming/Tomboy/notes/ -maxdepth 1 -name *.note`;
do
	echo "Pushing $note into Android"
	adb push $(cygpath -aw $note) /sdcard/tomdroid
done

echo "I hope for you that the emulator was running or else you got errors!"
