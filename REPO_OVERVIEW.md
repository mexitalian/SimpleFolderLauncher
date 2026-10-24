# SimpleFolderLauncher — Overview

A minimalist, privacy-respecting, Linux-inspired Android launcher. No tracking, no bloat.

## Key Features

- **File-system structure** – apps and widgets are organized like files and folders
- **Private** – no internet, no tracking
- **Customizable UI** – edit names, colors, icons, and widgets
- **Dots search** – a search bar shortcut system inspired by DuckDuckGo bangs:
  - `.b` – search/open in the default browser
  - `.m` – search in the default Maps app
  - `y` – search YouTube/NewPipe
  - Example: `.b example.com` opens that URL in the default browser

## Usage

- **Long-press background:** add apps, folders, widgets, or open settings
- **Tap:** launch an app / open a folder
- **Long-press item:** move, rename, or change its icon
- **Swipe up:** search bar (use Dots)
- **Swipe down:** close the search page / open notifications and quick settings
- **Per-folder widgets:**
  - Normal widgets, taking up as much space as needed
  - Row widgets: height defined as a percentage of the folder width, containing widgets whose widths are percentages of the row width
- **Long-press a widget in edit mode** to remove it

## Technical Details

- Android app, Gradle-based (standard `app/`, `build.gradle`, `gradlew` structure)
- **Fastlane** for release automation
- **GitHub Actions** workflows for CI
- Single contributor (mexitalian), ~114 commits

## Notes

The launcher editing UI is still rather unintuitive; improvements are planned. This is a hobby/personal project aimed at users who want a barebones, tinkerer-friendly launcher with a terminal-like organization model.
