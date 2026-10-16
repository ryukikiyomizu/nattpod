# Kattpad

An independent, open-source **native Android Wattpad client**, built with Kotlin,
Jetpack Compose, and Ktor. This project is not affiliated with Wattpad. It uses
Wattpad's existing services; access, login verification, and content availability
are controlled by Wattpad.

## This rebuild: reading-first foundation

- **Discover:** story and writer search, debounced queries, genre-based searches,
  explicit pagination, and retryable errors. Genre shelves are search results,
  not fabricated trending charts or personalized recommendations.
- **Library:** account library, title filtering, All / Reading / Completed filters,
  recent-reading order, safe chapter progress, and refresh. “Completed” means the
  author has marked the story complete, not that the reader finished it.
- **Reader:** chapter navigation, serif typography, persistent text sizing,
  Paper / Sepia / Night appearances, votes and comments. Chapter synchronization
  remains best-effort using the existing endpoint; precise scroll-position sync
  and offline downloads are not implemented.
- **Story and writer pages:** working Start reading and table of contents,
  published stories, paginated follower/following lists, and follow/unfollow.
- **Account:** encrypted token storage, validated session restoration, local sign
  out, retry on temporary connection failure, and a recovery screen for unreadable
  encrypted credentials. Saved tokens are excluded from backup/device transfer.

Network and decoding errors are handled at screen/action boundaries, and coroutine
cancellation is propagated. Sparse library metadata no longer requires non-null
reading positions, chapter lists, or chapter totals.

## Build and checks

Requires **JDK 17**, the Android SDK (platform 34), and network access to Gradle,
Google Maven, and Maven Central. Open the repository in Android Studio or run:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The `Android checks` GitHub Actions workflow runs those commands on pushes and
pull requests and uploads test/lint reports and the debug APK as separate artifacts (retained for
14 days). It does not sign or
publish a production release. Unit tests cover sparse reading metadata, missing
or removed chapters, bounded progress, API decoding, UTC dates, and cancellation.

### Verification status

GitHub Actions verified commit `b8d97a9` on October 16, 2026:

- `assembleDebug`: **passed**, debug APK uploaded.
- `testDebugUnitTest`: **16 tests, 0 failures, 0 errors, 0 skipped**.
- `lintDebug`: **passed**.

[Verified workflow run](https://github.com/ryukikiyomizu/nattpod/actions/runs/38906231191)
· [Download debug APK artifact](https://github.com/ryukikiyomizu/nattpod/actions/runs/38906231191/artifacts/12047539553)

Download and unzip **kattpad-debug-apk**, then install `app-debug.apk` on an Android
10+ device. This is a debug-signed development build, not a production release;
it may not install over a release-signed app with the same application ID.
Do not uninstall an existing app without considering its local data.

The editing sandbox itself has no Java or Android SDK; compilation and checks
were performed on GitHub's hosted runner. Artifact downloads in this sandbox are
blocked by its network allowlist, so use the GitHub download link above. A live
Wattpad account and on-device behavior have **not** yet been tested. Passing CI
is not confirmation that the original signed-in crash is resolved on a device.

### Device smoke-test checklist

1. Launch without a session: discovery and search work; Library offers sign-in.
2. Search rapidly and change genres: cancelled requests do not show stale results.
3. Sign in with valid and invalid credentials; disconnect Wi-Fi while signing in.
   Controls recover without crashing or leaving the spinner running.
4. Relaunch with a valid, expired, and offline session. Offline restoration keeps
   the saved token and offers retry in Account; an HTTP 401 removes it.
5. Open libraries with unstarted stories and missing reading-position/chapter
   fields. Refresh, filter, open a story, and navigate to its chapters.
6. Read a chapter, change text size and appearance, relaunch, and confirm settings
   persist. Test first/last chapter boundaries and unavailable chapter text.
7. Vote, comment, load more comments, delete your comment, follow/unfollow, and
   test each operation offline. Check guest comment/vote controls are disabled.
8. Sign out and relaunch: account identity and library are no longer displayed.
9. Restore a device backup: old encrypted tokens must not be transferred.

## Next phases (not implemented)

1. Device crash-report/logcat verification and API contract tests.
2. Local reading-history/bookmark persistence and robust library pagination.
3. API-verified reading-list management and notifications.
4. Rights-respecting offline reading, where the service permits it.
5. Writing/publishing only after verifying supported authenticated endpoints.
6. Move new UI copy into Android string resources and complete French localization.

This is a first rebuild slice, **not full Wattpad feature parity**. No independent
account system, publishing backend, AI features, or pretend social activity is
included.
