# TimeR Machine

[![Android CI](https://github.com/timer-machine/timer-machine-android/actions/workflows/android.yml/badge.svg?branch=main)](https://github.com/timer-machine/timer-machine-android/actions/workflows/android.yml)

A highly customizable interval timer app for Android

![Showcase](images/showcase.jpg)

<a href='https://play.google.com/store/apps/details?id=io.github.deweyreed.timer.google'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' height='100'/></a>

## Structure

The app uses the [Navigation component](https://developer.android.com/guide/navigation).

- Modules whose names start with `app-` are different destinations of the navigation graph.
- Each destination uses `ViewModel` in the `presentation` module.
- Each `ViewModel` is injected with `UseCase` in the `domain` module.
- Each `UseCase` is injected with different repositories that are implemented in the `data` module.
- Modules whose names start with `component-` are shared views and utility codes.
- The `flavor-google` module includes some advanced features and IAP.

## Build

Use the `dog` product flavor to develop. The other two flavors require Firebase. Their detailed
build instructions will arrive later.

## Contribute

Since I've been cleaning up and improving the codebase recently, please open an issue before
contributing. It would avoid some repetitive work.

## License

TimeR Machine is under the [GNU General Public License v3.0](LICENSE).

Some code and functions
from [AOSP's desklock](https://android.googlesource.com/platform/packages/apps/DeskClock/+/refs/heads/master/src/com/android/deskclock)
are under the Apache License 2.0.
