# vssbox Branding — FD Manager placement

Company: **vssbox**
Logo: blue neon isometric cube (dark bg) — `vssbox_logo.jpg` (JPG, small, needs transparent PNG/SVG for production)

## Current app identity
- Name: FD Manager
- Theme: teal primary #0f766e, M3 light+dark, BankMonogram tiles
- Screens: HomeScreen, FdListScreen (FdCard), FdDetailScreen, AddEditFdScreen, CalendarScreen, SettingsScreen, RecycleBinScreen

## Where we CAN display logo + name (official components)

| Location | Component | How | Pros / Cons |
|---|---|---|---|
| **Launcher icon** | `ic_launcher` adaptive | Cube as foreground, bg #0B1220 | Strongest branding, but replaces FD identity — needs decision |
| **Splash screen** | Android 12+ splash | Cube centered dark bg + vssbox text | Good first impression, matches logo bg |
| **TopAppBar** | `HomeScreen` TopAppBar, `FdListScreen` TopAppBar | Option A: title "FD Manager" + subtitle "by vssbox" (labelSmall). Option B: small cube 24dp on right. | Visible everywhere, but can clutter. Recommend only subtitle. |
| **HomeScreen hero footer** | Below total invested card | Row: cube 16-20dp + "Built by vssbox" labelSmall onSurfaceVariant | Subtle, trust, not competing with totals — **recommended** |
| **Settings → About** | `SettingsScreen` About card | Large cube 72dp, "vssbox", version, links, © 2026, CBSL disclaimer | **Best home for full branding** — minimal intrusion |
| **Onboarding** | Future #1 onboarding | 3-pager with cube + tagline | Good for new users |
| **Play Store** | Store listing | Icon + feature graphic with cube + vssbox footer on screenshots | Required for store |

**Avoid**: FdCard, BankSummaryCard — those show institution monograms (BOC/NSB/COM). Mixing company logo there would confuse institution identity.

## Color
Current primary teal #0f766e vs cube blue ~#5B9CF6. Options:
- Keep teal primary, blue as brand accent (About + footer)
- Shift primary to blue to match logo (bigger change, needs theme update)

## Assets needed before coding
- Transparent PNG 512x512 + 1024x1024
- SVG or vector drawable
- Light-bg version + monochrome for themed icons (Android 13+)
- Adaptive icon foreground + background

## Questions for owner (from issue #27)
1. Company vs product rename? FD Manager vs vssbox FD Manager vs vssbox?
2. Icon replace or keep FD icon and use cube only in About?
3. TopAppBar branding everywhere or only About?
4. Logo variants available?
5. Keep teal or shift to blue?
6. Tagline?
7. © footer on Home?
8. Priority must-have vs nice-to-have?

## Recommendation (my take)
- Keep **FD Manager** as product name, add **"by vssbox"** as company for v1 — low risk, users search FD Manager.
- Icon: create adaptive icon from cube (dark bg #0B1220) for Play Store, but keep teal for now? Or A/B test.
- **About screen** = primary branding home, **Home footer** subtle "Built by vssbox" 16dp cube.
- Need transparent asset before any code — JPG dark bg will look bad on light theme.
