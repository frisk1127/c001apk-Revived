# Third-party notices

GitHub release notes are rendered with [Markwon 4.6.2](https://github.com/noties/Markwon/tree/v4.6.2)
(Dimitry Ivanov, Apache-2.0), including its GFM tables extension.
The license is distributed in `app/src/main/assets/licenses/Markwon-Apache-2.0.txt`.

The Niuniu emoji images (`niuniu_emotion_*.png`) and their Chinese names come
from the Coolapk 16.5.0 Android client (2607151). The images are included unchanged
for displaying and composing Coolapk messages. Emoji artwork belongs to Coolapk
and its respective rights holders; it is not covered by this project's code license.

The SZLM registration wire format and the sample report in
`app/src/main/assets/szlm_report.json` are based on
[kongwufang/szlm_res v1.0.0](https://github.com/kongwufang/szlm_res/releases/tag/v1.0.0)
(`getduid.py`, MIT, Copyright 2026 kongwufang).
The complete license is distributed in `app/src/main/assets/licenses/szlm_res-MIT.txt`.

The Kotlin client and application integration are implemented in this project.
Registration runs once after a successful login, or when requested in settings. It does not collect account
credentials or real device identifiers and does not rotate identifiers in response
to verification challenges. Existing identifiers are preserved on failure.

The animated about-page background adapts the OS3 shader and phone presets from
the [Miuix example](https://github.com/compose-miuix-ui/miuix/tree/39c40f99844227b853f0049a0933b1f3ae6c00ba/example/shared/src/commonMain/kotlin/component/effect)
(Copyright 2026, compose-miuix-ui contributors, Apache-2.0).
It uses Miuix's runtime shader API with app-owned lifecycle and rendering code.
The glass card uses the example's `Overlay_Thin_Light` / `Pured_Regular_Light`
blend presets from `component/blend/ColorBlendToken.kt` under the same license.
The motion and color-cycle behavior was checked against
[InstallerX-Revived](https://github.com/wxxsfxyzm/InstallerX-Revived/tree/35d7c7a06e86b280789a171d8b213becac1a0fd7/app/src/main/java/com/rosan/installer/ui/library/effect),
which uses the same Miuix example effect.
The shader clamps empty color fields to avoid division by zero. The full license
is distributed in `app/src/main/assets/licenses/miuix-background-Apache-2.0.txt`.
