# Changelog

<!--
One section per release, newest first. The heading must be "## " followed by the exact
launcher_version_name from ZalithLauncher/gradle.properties. The section becomes the GitHub
release description and the in-app update notification. The release workflow fails if it is empty.
-->

## 2.8.2 Ray RC1

**HIGHLIGHTS:**
- Drastically reduced the Launcher's file size
- Cleaned up the codebase (removed 4,000+ lines)
- Added: A visualizer separate from the audio widget
- Added: A custom boot launcher
- Added: A "Ray" page containing Ray-specific features
- The Launcher menu now supports zooming out, allowing cards to be placed anywhere

**OTHER CHANGES:**
- Fixed: An issue where the Minecraft version for a card would default to the Launcher's version when downloading mods
- Fixed: Update notifications appearing even when the Launcher is already up to date
- Fixed: Audio widget appearing squashed when no audio is loaded
- Fixed: Audio widget pausing playback when navigating to another page while music is imported
- Added: A "don't auto-close log" option to prevent the log window from closing automatically when the Mojang logo appears
- Removed: The delete button for Launcher cards
- Removed: Unnecessary duplicate UI settings
- Updated: Page-transition animations so they can be toggled independently (preventing all animations from being disabled at once)
- Renamed: The "Controls" menu to "Mouse"
- Tweaked: The three navigation buttons at the top
- Various other minor adjustments
