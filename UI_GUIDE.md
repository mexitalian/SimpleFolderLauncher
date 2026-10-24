# UI Development Guide

This guide explains how to work on the launcher's UI with an AI assistant (or any contributor), and how to get testable builds from CI.

## UI Surface Map

The UI is cleanly separated from the data layer, so you can point at the exact surface you want to change:

| You want to change... | Relevant files |
|---|---|
| **Home screen layout** (folder list, widget container, path bar) | `res/layout/activity_main.xml`, `MainActivity.java` |
| **App/folder row appearance** (icons, names, spacing) | `files/FileAdapter.java`, `files/FileViewHolder.java`, the row layout in `res/layout` |
| **Search screen & Dots bar** | `search/SearchActivity.java`, `search/SearchResultAdapter.java`, `search/SearchEngine.java`, `search/dots/*` |
| **Colors, themes, fonts** | `res/values/` (colors, styles, themes) |
| **Menus** (add/edit/delete/move) | `res/menu/addaction_menu.xml`, `res/menu/fileaction_menu.xml` |
| **Settings screens** | `settings/*`, preference XML in `res/xml` |
| **Gesture behavior** (swipe up/down, double tap) | `HomeGestureListener.java` and the gesture callbacks in `MainActivity.java` |
| **Widget layout system** | `widgets/*` (`WidgetSystem`, `WidgetList`, `WidgetLayout`, `WidgetElement`) |

## How to Instruct Changes

When working with an AI assistant on the UI, effective prompts look like:

- **Reference a screen by name**: "the search screen", "the widget picker" — the files can be found from there
- **Describe the intent for the look**: minimal? colorful? dense? — this matters more than file names
- **Mockups or sketches help enormously**: even a rough ASCII layout or a screenshot of another launcher you like
- **One screen at a time**: small focused commits are easy to revert if the result doesn't feel right

Typical iteration loop:

1. Change is made and committed (ideally one screen per commit)
2. CI builds the APK (see below)
3. Download the APK from the workflow artifacts, install, and try it
4. Report what to tweak, and iterate

## CI

### Tests

[`.github/workflows/test.yml`](.github/workflows/test.yml) runs `./gradlew test` on every push and pull request.

### Builds

[`.github/workflows/build.yml`](.github/workflows/build.yml) builds the app on every push to `main` and on manual dispatch:

- **Debug APK** — `app-debug.apk`, installable right away
- **Release APK** — unsigned `app-release-unsigned.apk`; sign it with your own keystore before distributing

Find them under the workflow run's **Artifacts** section on the Actions tab.

### Store screenshots

[`.github/workflows/screengrab.yml`](.github/workflows/screengrab.yml) runs fastlane screengrab on an emulator (manual trigger only) and commits the results to the `fastlane` branch.
