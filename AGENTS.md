# AGENTS.md

## Communication
- **Language**: Always communicate with the user in **Traditional Chinese (繁體中文)**.
- **Style**: Terse, direct, technical. Omit conversational filler. Code and technical symbols verbatim.

## Project & Fork Overview
- **Base**: Downstream fork of [NexAlloy/NexAlloy](https://github.com/NexAlloy/NexAlloy.git).
- **Core Engine**: Xposed / LSPosed runtime patcher using DexKit bytecode analysis and Morphe extension libraries.
- **Fork Target Packages**: Managed in `app/src/main/java/io/github/nexalloy/AppPatchInfo.kt`. Never discard fork hooks during upstream syncs.

### Preserved Fork Customizations & Hook Targets
- **Facebook** ([SysAdminDoc/Hushfacebook](https://github.com/SysAdminDoc/Hushfacebook))
  - Feed: Intercept `FeedUnitCollectionManager.addNewEdgeToCollection` to drop `SPONSORED` / `PROMOTION` categories; hook `EdgeSwapRun` to skip ad swaps.
  - Stories, Reels & Marketplace: Filter ad story buckets, ad reels, and sponsored listings/PDP.
  - Telemetry & Prefetch: Block background ad prefetch workers and Audience Network SDK initialization.
  - UI & Sharing: Long-press Facebook logo to open `FacebookSettingsDialog`; strip tracking parameters (`fbclid`) from shared URLs.
- **Threads** ([SysAdminDoc/HushThreads](https://github.com/SysAdminDoc/HushThreads))
  - Hook `adInjectorFingerprint` to return `false`.
  - Purge injected sponsored threads from feed cache arrays.
- **Instagram** ([SysAdminDoc/HushGram](https://github.com/SysAdminDoc/HushGram))
  - Hook `adInjectorFingerprint` to return `false` (zero-gap ad suppression across feed, stories, and reels).
- **Telegram** ([SysAdminDoc/HushTelegram](https://github.com/SysAdminDoc/HushTelegram))
  - Channel Ads: Hook `GetSponsoredMessagesFingerprint` to return `null`.
  - Video Player Ads: Hook `VideoAdsLoadFingerprint` with `XC_MethodReplacement.DO_NOTHING`.
  - Search Ads: Intercept `SearchSponsoredPeersFingerprint` to cancel sponsored peer queries.
- **Twitch** ([hoomans-morphe-patches](https://github.com/arandomhooman/hoomans-morphe-patches) & [De-Vanced](https://github.com/RookieEnough/De-Vanced))
  - Live Stream Ads: Intercept `LiveManifestUrlBuilderFingerprint` (`usher.ttvnw.net`) to route streams through ad-free proxy `https://lb-as.cdn-perfprod.com/live/<channel>`.
  - Display Ads: Intercept `DisplayAdResponseParserFingerprint` and return `NoAd` singleton.
  - Video & Audio Ads: Intercept `EligibilityCheckCompleted` to force `shouldRequestAd = false`; silence `sendAudioAdsContext` presenter actions.
- **Messenger** ([SysAdminDoc/HushMessenger](https://github.com/SysAdminDoc/HushMessenger))
  - Intercept `ItemListProcessorInterfaceSpec.processItems` returning `ImmutableList` to filter out `InboxAdsItem`.

## Architecture & File Structure
- `app/src/main/java/io/github/nexalloy/`:
  - `PatchExecutor.kt`: Core patch loader, lifecycle dispatcher, and DexKit bridge manager.
  - `AppPatchInfo.kt`: Package name to patch mapping.
  - `morphe/`: Ported patches grouped by target app (`youtube`, `facebook`, `instagram`, `threads`, `telegram`, `twitch`, `messenger`, `shared`).
- `morphe-patches` & `morphe-patches-library`: Git submodules integrating Morphe upstream assets and stubs.
- `.github/workflows/release.yml`: Release CI triggered on pushing tags matching `*.*.*`.

## Build & Test Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Run unit and fingerprint tests
./gradlew test

# Update submodules to pinned commits
git submodule update --init --recursive
```

## Routine Upstream Synchronization
1. **Fetch & Merge**:
   ```bash
   git fetch upstream && git merge upstream/main --no-edit
   ```
   *Resolve conflicts surgically; keep fork customizations intact.*
2. **Update Submodules**:
   ```bash
   git submodule update --init --recursive
   ```
3. **Verify Build**:
   ```bash
   ./gradlew assembleDebug
   ```
4. **Tag Incremental Release**:
   ```bash
   git tag -a v1.0.X-test -m "v1.0.X-test"
   ```
   *Increment `X` from the latest `v1.0.*-test` tag.*
5. **Push to Origin**:
   ```bash
   git push origin main && git push origin <tag>
   ```

## Git & GPG Guidelines
- **Remotes**: `origin` is user fork; `upstream` is `https://github.com/NexAlloy/NexAlloy.git`.
- **Signed Commits**: All commits must be GPG signed (`git commit -S`).
- **Loopback Priming** (for automated or non-interactive sessions):
  ```python
  import subprocess as s
  s.Popen(['gpg', '--batch', '--yes', '--passphrase-fd', '0', '--pinentry-mode', 'loopback', '-s', '--output', 'NUL'], stdin=s.PIPE).communicate(b'<PASSPHRASE>')
  s.run(['git', 'commit', ...])
  ```

## Development Principles
- **Surgical Changes**: Touch only required lines. Do not reformat adjacent code or delete unrelated dead code.
- **Error Handling**: Wrap non-universal or variant-dependent hooks in `runCatching`.
- **DexKit Fingerprints**: Keep string matchers and method signatures strict and narrow to avoid false-positive matches.
