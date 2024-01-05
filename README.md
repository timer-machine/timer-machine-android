# TimeR Machine

[![Android CI](https://github.com/timer-machine/timer-machine-android/actions/workflows/android.yml/badge.svg?branch=main)](https://github.com/timer-machine/timer-machine-android/actions/workflows/android.yml)

A highly customizable interval timer app for Android

![Showcase](images/showcase.jpg)

## Download

|Link|Package Name|
|:-:|:-:|
|<a href='https://play.google.com/store/apps/details?id=io.github.deweyreed.timer.google'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' height='75'/></a>|io.github.deweyreed.timer.google|
|[Get it on Google Drive](https://drive.google.com/open?id=1YHIdW77fuxmyQ7sFza1LEIqmhzBygEZx)<br>(AAB universial APK)|io.github.deweyreed.timer.google|
|<a href='https://f-droid.org/en/packages/io.github.deweyreed.timer.other/'><img alt='Get it on F-Droid' src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png' height='75'/></a>|io.github.deweyreed.timer.other|
|[Get it on GitHub](https://github.com/timer-machine/timer-machine-android/releases)|io.github.deweyreed.timer.other|

## Structure

The app uses the [Navigation component](https://developer.android.com/guide/navigation).

- Modules whose names start with `app-` are different destinations of the navigation graph.
- Each destination uses `ViewModel` in the `presentation` module.
- Each `ViewModel` is injected with `UseCase` in the `domain` module.
- Each `UseCase` is injected with different repositories that are implemented in the `data` module.
- Modules whose names start with `component-` are shared views and utility codes.
- The `flavor-google` module includes some advanced features and IAP.

## Build

Use the `dog` product flavor to develop and test.

The `google` product flavor is the version in Google Play. It has some in-app purchases. It also
uses Firebase to store backup files and AppCenter to track crashes.

- Firebase: Create a Firebase project and add `google-services.json` to the project.
  - [This optional Firebase Cloud Function](functions/index.js) removes old backup files when
      there are too many.
- AppCenter: Create an AppCenter project and put the app secret to your `local.properties`(
  Format: `APP_CENTER_APP_SECRET=your-app-secret`).

Compared with the `google` product flavor, the `other` product flavor removes in-app purchases and
corresponding functions to release the app to other app stores.

## Contribute

If you have any questions or suggestions, feel free
to [open an issue](https://github.com/timer-machine/timer-machine-android/issues/new).

There are some legacy codes that I wrote while learning Android development. I plan to fix them
when they are broken or required by a new feature.

## Translations

If you'd like to add translations, please join the project
on [Weblate](https://hosted.weblate.org/engage/timer-machine/).

[![Translation status](https://hosted.weblate.org/widget/timer-machine/android/multi-auto.svg)](https://hosted.weblate.org/engage/timer-machine/)

## License

TimeR Machine is under the [GNU General Public License v3.0](LICENSE).

Some code and functions
from [AOSP's desklock](https://android.googlesource.com/platform/packages/apps/DeskClock/+/refs/heads/master/src/com/android/deskclock)
are under the Apache License 2.0.
