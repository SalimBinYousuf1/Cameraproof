# Salim - Photo Proof Camera & Field Inspection Reports

Salim is a photo-proof camera Android application built for inspectors, delivery staff, contractors, and field agents. It burns date, time, verified GPS coordinates, street address, project name, and notes directly into captured photos, groups photos into projects, and exports professional multi-page PDF reports.

## Key Features

1. **Camera with Burned-In Proof Stamp**
   - Live camera with back/front toggle, 3-state flash (Off/Auto/On), tap-to-focus, pinch-to-zoom, and rule-of-thirds grid.
   - Burns date/time (12h/24h), GPS coordinates, street address, project name, and inspector notes directly onto high-resolution JPEGs.
   - Orientation-aware matrix transformation prevents stamps from appearing sideways or truncated.
   - EXIF metadata tagging (GPS latitude, longitude, altitude, timestamp, user comment).

2. **Offline-First Data Storage**
   - 100% offline, zero accounts, zero analytics, zero external network dependency.
   - Room Database for projects and photos metadata.
   - DataStore Preferences for user stamp and report customization.
   - Memory-safe thumbnail generation for instant gallery scrolling.

3. **Multi-Page PDF Inspection Reports**
   - Generates multi-page PDF reports via `android.graphics.pdf.PdfDocument`.
   - Memory-efficient downscaling and recycling prevents Out-Of-Memory (OOM) errors even with 100+ photos.
   - Exports via Android Storage Access Framework (SAF) and system Share sheet.
   - Free tier includes a discreet "Made with Salim" footer; Pro tier removes watermarks and enables custom company/inspector headers.

4. **Google Play Billing Integration (Salim Pro)**
   - Complete Google Play Billing Library v7 integration (`BillingClient`).
   - Query product details, purchase flow, acknowledgement, restore purchases, and launch entitlement verification.
   - Free tier allows up to 3 active projects; Pro unlocks unlimited projects, clean PDFs, and company branding.

5. **Design & Accessibility**
   - Material Design 3 with calm Slate and Precision Sky palette.
   - Edge-to-edge system insets handling, supporting both Light and Dark themes.
   - Full accessibility support with 48dp+ touch targets, content descriptions, and localized strings.
