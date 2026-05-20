# AcademiQ Design Tokens

> Auto-extracted from Figma on 2026-05-20. This is the single source of truth for
> all UI styling. Reference this file when building any JavaFX view.
>
> **Source file:** [Figma — Main / DashBoard](https://www.figma.com/design/RhfdZ1KOHIloQS2JZbERZ5/Main?node-id=34-190&m=dev)
>
> **Frames inspected:**
> - `34:190` — DashBoard (1440 × 1024, root)
> - `34:191` — Frame 4 (left sidebar, 342 × 1024)
> - `51:933` — Sidebar buttons stack (Dashboard / Account / Announcement / Faculty Evaluation)
> - `34:192` — Settings / Log out group (bottom of sidebar)
> - `34:197` — Frame 5 (profile header card, 940 × 192)
> - `34:217` — Dashboard navbar (tab bar + content area, 940 × 622)
> - `34:213` — Frame 10 (light/dark theme toggle, 132 × 48)
>
> Values were sourced from Figma variables (`get_variable_defs`) and frame
> properties (`get_design_context`). Anything Figma did not specify is marked
> **[inferred]** from the screenshot and should be verified.

---

## Color Palette

The Figma file does **not** publish named color variables — colors below are
the raw hex values used in the design. The token names are the project's
naming convention, not Figma's.

| Token Name      | Hex        | Source                                                    | Usage                                                          |
|-----------------|------------|-----------------------------------------------------------|----------------------------------------------------------------|
| `primary`       | `#89343B`  | sidebar active button, active tab, theme toggle dark side | CIT maroon. Active nav state, primary buttons, active tabs.    |
| `surface`       | `#FFFFFF`  | sidebar bg, profile card, navbar, tab bar, content area   | Cards, dialogs, sidebar, top-level surfaces.                   |
| `background`    | `#F5F5F5`  | page bg (visible between cards) **[inferred]**            | Main app background behind cards.                              |
| `text-primary`  | `#1D1B20`  | "Full name", inactive tab labels, settings/log out labels | Main body text, headings, inactive nav labels.                 |
| `text-secondary`| `#828282`  | "ID", "Contact number", "Email", "Address" labels         | Field labels, captions, helper text.                           |
| `text-on-primary`| `#FFFFFF` | "Dashboard" (active sidebar item), "Schedule" (active tab)| Text/icons sitting on `primary` backgrounds.                   |
| `text-inactive-nav` | `#000000` | inactive sidebar labels (Account, Announcement, etc.) | Sidebar items in their default (non-active) state.            |
| `muted`         | `#E0E0E0`  | avatar placeholder fill, **[inferred]** dividers/borders  | Avatar fallback, subtle borders.                               |
| `accent`        | `#89343B`  | alias of `primary`                                        | The design uses a single brand color; no separate accent.      |
| `success`       | `#2E7D32`  | **[inferred — not in Figma]**                             | Good GPA, achievable projection. Pick before shipping.         |
| `warning`       | `#ED6C02`  | **[inferred — not in Figma]**                             | Challenging projection, near-cap warnings.                     |
| `danger`        | `#C62828`  | **[inferred — not in Figma]**                             | Errors, schedule conflicts, impossible projections.            |

> **Note:** the dashboard frame in Figma does not yet show grading, status, or
> conflict UI, so `success` / `warning` / `danger` are placeholders. They are
> harmonized with `#89343B` (red-leaning, muted). Confirm with the designer
> before locking them in.

---

## Typography

Figma publishes three text styles as variables. The font family is **Inter**
across the board.

| Element              | Font Family | Size | Weight        | Line Height | Letter Spacing | Color       |
|----------------------|-------------|------|---------------|-------------|----------------|-------------|
| Page title           | Inter       | 24px | SemiBold (600)| 30px        | -0.15px        | `#1D1B20`   |
| Section header       | Inter       | 24px | SemiBold (600)| 30px        | -0.15px        | `#1D1B20`   |
| Nav button (active)  | Inter       | 24px | SemiBold (600)| 30px        | -0.15px        | `#FFFFFF`   |
| Nav button (inactive)| Inter       | 24px | SemiBold (600)| 30px        | -0.15px        | `#000000`   |
| Tab (active)         | Inter       | 24px | SemiBold (600)| 30px        | -0.15px        | `#FFFFFF`   |
| Tab (inactive)       | Inter       | 24px | SemiBold (600)| 30px        | -0.15px        | `#1D1B20`   |
| Body text            | Inter       | 16px | Regular (400) | 22px        | -0.18px        | `#1D1B20`   |
| Field label          | Inter       | 16px | Regular (400) | 22px        | -0.18px        | `#828282`   |
| Field value          | Inter       | 14px | Medium (500)  | 20px        | -0.16px        | `#1D1B20`   |
| Caption / muted      | Inter       | 14px | Medium (500)  | 20px        | -0.16px        | `#828282`   |
| Table header         | Inter       | 16px | Regular (400) | 22px        | -0.18px        | `#828282`   *[inferred]* |
| Table cell           | Inter       | 14px | Medium (500)  | 20px        | -0.16px        | `#1D1B20`   *[inferred]* |
| Button text          | Inter       | 24px | SemiBold (600)| 30px        | -0.15px        | matches state |
| Grade display (large)| Inter       | 24px | SemiBold (600)| 30px        | -0.15px        | `#1D1B20`   *[inferred]* |

### Figma named styles (raw)

| Variable name           | Family | Size | Weight | LineHeight | Letter spacing |
|-------------------------|--------|------|--------|------------|----------------|
| `Heading/H5/Semi Bold`  | Inter  | 24   | 600    | 30         | -0.15          |
| `Label/Label-1/Regular` | Inter  | 16   | 400    | 22         | -0.18          |
| `Label/Label-2/Medium`  | Inter  | 14   | 500    | 20         | -0.16          |

### JavaFX font note

**Inter is not bundled with JavaFX.** Either:
- Ship Inter (`Inter-Regular.ttf`, `Inter-Medium.ttf`, `Inter-SemiBold.ttf`) under
  `src/main/resources/fonts/` and register via `Font.loadFont(...)` at startup, or
- Fall back to the closest JavaFX-available system fonts:
  - **Segoe UI** (Windows) — closest neutral grotesque match.
  - **System** / `-fx-font-family: "System"` cross-platform fallback.

Recommended `.css` declaration once loaded:
```css
.root { -fx-font-family: "Inter", "Segoe UI", "System"; }
```

---

## Spacing

Figma does not expose named spacing tokens. These values are derived from the
auto-layout `gap` / `padding` properties on inspected frames.

| Token         | Value | Source / Usage                                                        |
|---------------|-------|-----------------------------------------------------------------------|
| `spacing-xs`  | 4px   | Navbar inner vertical padding (`py-[4px]`).                            |
| `spacing-sm`  | 11px  | Gap between field label and field value in profile card.              |
| `spacing-md`  | 16px  | Sidebar button horizontal padding; gap between Settings/Log out rows. |
| `spacing-lg`  | 18px  | Vertical gap between sidebar buttons; gap between navbar and content. |
| `spacing-xl`  | 24px  | Gap between sidebar icon and label.                                    |
| `spacing-2xl` | 32px  | Navbar tab-bar horizontal padding.                                     |
| `spacing-3xl` | 60px  | Gap between profile detail columns (ID / Contact / Email / Address). |

> The design also uses `12px` inside sidebar buttons (`py-[12px]`) and `14px`
> inside tab buttons (`py-[14px]`); treat these as component-internal, not as
> general spacing tokens.

---

## Border Radius

| Token         | Value | Usage                                                            |
|---------------|-------|------------------------------------------------------------------|
| `radius-sm`   | 8px   | **[inferred]** Inputs, small buttons (not in current dashboard). |
| `radius-md`   | 20px  | Cards, sidebar buttons, tabs, navbar bar, content area.          |
| `radius-lg`   | 30px  | Theme toggle pill, large rounded containers.                     |
| `radius-full` | 999px | Pills, badges, the avatar circle.                                |

> Sidebar `Frame 4` only rounds its **right** corners (`rounded-tr-[20px]
> rounded-br-[20px]`) because it is flush against the left edge.

---

## Shadows / Effects

The Figma frames do **not** declare drop shadows on any inspected node — the
cards rely purely on the `#F5F5F5` background to separate from white surfaces.
The tokens below are **[inferred]** for use when separation is needed
(e.g. modal dialogs, hover lifts).

| Token       | JavaFX equivalent                                                        | Usage                  |
|-------------|--------------------------------------------------------------------------|------------------------|
| `shadow-sm` | `-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 1);`        | Subtle card lift.      |
| `shadow-md` | `-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0, 0, 4);`       | Elevated cards, popovers.|
| `shadow-lg` | `-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 24, 0, 0, 8);`       | Modal dialogs.         |

---

## Component Patterns

### Sidebar (`Frame 4`, node `34:191`)

- **Width:** 342 px (frame); inner content column = 300 px wide, starting at
  `left: 21px`.
- **Height:** full viewport (1024 px in the design frame).
- **Background:** `#FFFFFF`.
- **Corner radius:** right side only — `radius-md` (20px) on top-right and
  bottom-right.
- **Logo:** `cit-logo 1` PNG, 314.7 × 61.8 px, positioned at `left: 14px,
  top: 36px`.
- **Nav stack:** starts at `top: 168px`, 18 px gap between items.
- **Nav button (`Sidebar buttons`):**
  - Width: 300 px (fills inner column).
  - Padding: `12px 16px` (vertical / horizontal).
  - Corner radius: 20 px.
  - Inner layout: icon (30×30) + 24 px gap + label (Heading/H5 SemiBold 24).
  - **Active** state: background `#89343B`, label color `#FFFFFF`.
  - **Inactive** state: transparent background, label color `#000000`.
- **Bottom group (`Settings`, `Log out`):**
  - Anchored at `left: 37px, top: 900px`.
  - 16 px vertical gap; same icon+gap+label structure but transparent bg and
    `#1D1B20` text.

### Cards (profile header `Frame 5`, navbar bar, content panel)

- **Background:** `#FFFFFF` (`surface`).
- **Border:** none.
- **Corner radius:** 20 px (`radius-md`).
- **Padding:** varies by component; profile card uses ~40 px top inset for
  content alignment.
- **Shadow:** none in the Figma source; add `shadow-sm` if visual separation
  is needed against `background`.

### Profile header (`Frame 5`, node `34:197`)

- **Size:** 940 × 192 px.
- **Avatar:** 100 × 100 circle at `left: 39px, top: 41px`, fill `#E0E0E0`
  placeholder.
- **Name:** at `left: 174px, top: 41px`, Heading/H5 SemiBold, `#1D1B20`.
- **Detail row:** at `left: 174px, top: 98px`, horizontal flex with 60 px
  gaps between columns.
  - **Label (top):** `Label/Label-1/Regular`, `#828282`.
  - **Value (bottom):** `Label/Label-2/Medium`, `#1D1B20`.
  - 11 px vertical gap between label and value.

### Tab bar (`Dashboard navbar`, node `34:217`)

- **Outer container:** vertical stack with 18 px gap.
- **Tab bar card:** `#FFFFFF`, `radius-md`, padding `4px 32px`.
- **Tabs:** flex row, `justify-between` (tabs spread evenly across width).
  - Each tab: height 44 px, padding `14px 23px`, corner radius 20 px.
  - **Active** tab: background `#89343B`, text `#FFFFFF`.
  - **Inactive** tab: transparent background, text `#1D1B20`.
  - Text style: Heading/H5 SemiBold (Inter 24 / 600 / 30).
- **Content panel below tabs:** `#FFFFFF`, `radius-md`, height 552 px, full
  width of the navbar (940 px in the dashboard frame). Empty in the current
  design — populate per-tab.

### Tables — **[inferred]** (not present in current Figma frame)

| Property         | Value                                  |
|------------------|----------------------------------------|
| Header bg        | `#FFFFFF`                              |
| Header text      | Label/Label-1/Regular, `#828282`       |
| Row height       | 44 px                                  |
| Alternating row  | none / `#FAFAFA`                       |
| Hover state      | `#F5F5F5`                              |
| Selection state  | `rgba(137, 52, 59, 0.10)` (primary @ 10%) |
| Cell padding     | 12 px vertical, 16 px horizontal       |
| Cell text        | Label/Label-2/Medium, `#1D1B20`        |

### Buttons

The dashboard exposes one button-like pattern: the sidebar/tab pill. Other
button variants are **[inferred]** for forms.

| Variant     | Background           | Text                   | Border               | Padding       | Radius | Height |
|-------------|----------------------|------------------------|----------------------|---------------|--------|--------|
| Primary     | `#89343B`            | `#FFFFFF`              | none                 | `14px 23px`   | 20px   | 44px   |
| Secondary   | `#FFFFFF` **[inferred]** | `#89343B` **[inferred]** | `1px solid #89343B` | `14px 23px` | 20px | 44px |
| Danger      | `#C62828` **[inferred]** | `#FFFFFF`            | none                 | `14px 23px`   | 20px   | 44px   |
| Disabled    | apply `-fx-opacity: 0.5;` to primary **[inferred]** |                |                      |               |        |        |

### Form Fields (TextField, ComboBox, Spinner) — **[inferred]**

Not present in the dashboard frame; recommend:
- **Height:** 40 px.
- **Border:** 1 px solid `#E0E0E0`.
- **Border radius:** 8 px (`radius-sm`).
- **Padding:** 8 px vertical, 12 px horizontal.
- **Focus ring:** 2 px outline `#89343B`.
- **Error state:** border `#C62828`, helper text `#C62828`.
- **Text:** Label/Label-2/Medium (`#1D1B20`); placeholder `#828282`.

### Dialogs — **[inferred]**

- **Width:** 480 – 640 px (constrain to content).
- **Background:** `#FFFFFF`.
- **Corner radius:** 20 px (`radius-md`).
- **Padding:** 24 px inner.
- **Header:** Heading/H5 SemiBold (Inter 24 / 600), `#1D1B20`.
- **Button bar:** right-aligned, 12 px gap between buttons.
- **Backdrop:** `rgba(0, 0, 0, 0.32)`.
- **Shadow:** `shadow-lg` (see above).

### Progress Bars — **[inferred]**

- **Track height:** 8 px.
- **Track bg:** `#E0E0E0`.
- **Track radius:** 999 px (full pill).
- **Bar radius:** 999 px.
- **GPA color coding** (suggested):
  - `≥ 3.5` → `#2E7D32` (success)
  - `2.5 – 3.49` → `#89343B` (primary)
  - `1.5 – 2.49` → `#ED6C02` (warning)
  - `< 1.5` → `#C62828` (danger)

### Status Indicators — **[inferred — not in Figma]**

| Variant | Background                | Border           | Text       |
|---------|---------------------------|------------------|------------|
| Success | `rgba(46, 125, 50, 0.10)` | `1px solid #2E7D32` | `#2E7D32` |
| Warning | `rgba(237, 108, 2, 0.10)` | `1px solid #ED6C02` | `#ED6C02` |
| Danger  | `rgba(198, 40, 40, 0.10)` | `1px solid #C62828` | `#C62828` |

### Theme toggle (`Frame 10`, node `34:213`)

- **Size:** 132 × 48 px pill.
- **Corner radius:** 30 px (`radius-lg`).
- **Background:** `#FFFFFF`.
- **Active half:** 66 × 48 maroon (`#89343B`) panel with `radius-lg` on the
  active side only. Icon is 24 × 24, white-on-maroon when active, dark-on-white
  when inactive.

---

## JavaFX CSS quick-reference snippets

For convenience when wiring this into `styles.css`:

```css
/* Brand */
.root {
  -fx-font-family: "Inter", "Segoe UI", "System";

  /* Looked-up colors */
  -color-primary:       #89343B;
  -color-surface:       #FFFFFF;
  -color-background:    #F5F5F5;
  -color-text-primary:  #1D1B20;
  -color-text-secondary:#828282;
  -color-muted:         #E0E0E0;
}

/* Card */
.card {
  -fx-background-color: -color-surface;
  -fx-background-radius: 20;
  -fx-padding: 16;
}

/* Active sidebar/tab pill */
.nav-pill-active {
  -fx-background-color: -color-primary;
  -fx-background-radius: 20;
  -fx-text-fill: white;
  -fx-font-size: 24px;
  -fx-font-weight: 600;
  -fx-padding: 12 16 12 16;
}
```

---

## Open questions for the designer

1. Are `success`, `warning`, `danger` colors finalized somewhere outside this
   Figma file? The dashboard frame doesn't show grading or status UI.
2. Is the page background `#F5F5F5` exact, or a different gray (e.g. `#F7F7F7`,
   `#FAFAFA`)? Inferred visually.
3. Should cards carry a shadow, or rely purely on the gray background for
   separation? No shadows defined in the file.
4. Are there input/form designs in another frame not linked from `34:190`?
5. Is Inter intended to be bundled with the app, or should we fall back to
   Segoe UI on Windows?
