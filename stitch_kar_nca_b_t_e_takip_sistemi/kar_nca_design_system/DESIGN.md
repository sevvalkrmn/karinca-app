---
name: Karınca Design System
colors:
  surface: '#fff8f6'
  surface-dim: '#eed4d0'
  surface-bright: '#fff8f6'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fff0ee'
  surface-container: '#ffe9e5'
  surface-container-high: '#fde2de'
  surface-container-highest: '#f7ddd8'
  on-surface: '#261816'
  on-surface-variant: '#5a413c'
  inverse-surface: '#3c2d2a'
  inverse-on-surface: '#ffedea'
  outline: '#8e706b'
  outline-variant: '#e2bfb8'
  surface-tint: '#b12c19'
  primary: '#ae2a17'
  on-primary: '#ffffff'
  primary-container: '#d0432d'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb4a6'
  secondary: '#006c49'
  on-secondary: '#ffffff'
  secondary-container: '#6cf8bb'
  on-secondary-container: '#00714d'
  tertiary: '#825100'
  on-tertiary: '#ffffff'
  tertiary-container: '#a36700'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdad4'
  primary-fixed-dim: '#ffb4a6'
  on-primary-fixed: '#3f0200'
  on-primary-fixed-variant: '#8f1203'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#fff8f6'
  on-background: '#261816'
  surface-variant: '#f7ddd8'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 28px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  2xl: 48px
  3xl: 64px
  container-margin: 20px
  gutter: 16px
---

## Brand & Style

The design system is centered around the concept of industriousness, community, and small, consistent efforts—embodied by the ant mascot. The brand personality is **friendly, encouraging, and vibrant**, yet maintains a **softness** that feels approachable for younger audiences while remaining functional for adults.

The visual style is a blend of **Modern / Corporate** reliability and **Tactile Playfulness**. It utilizes soft, organic shapes and a cheerful color palette to demystify the often-stressful topic of personal finance. The mascot is integrated as a progress guide, celebratory anchor, and motivational coach throughout the interface, turning the "chore" of budgeting into a rewarding experience.

The emotional goal is to evoke a sense of **calm control** and **optimistic growth**, supporting the slogan "karınca kararınca" (each according to their means/ability).

## Colors

The palette is vibrant and multi-tonal to allow for clear categorization of expenses and savings goals.

*   **Primary (Deep Red/Coral):** Used for brand identity, main actions, and high-importance status.
*   **Success (Emerald):** Represents savings, income, and positive progress.
*   **Warning (Amber):** Used for nearing budget limits or pending transactions.
*   **Information (Cyan):** Used for educational tooltips, mascots' speech bubbles, and neutral data points.
*   **Neutrals:** High-clarity off-whites and charcoal grays ensure that the vibrant brand colors remain readable and don't overwhelm the user.

Use the primary color sparingly for key CTAs to maintain its visual impact against the softer secondary and tertiary tones.

## Typography

This design system uses **Plus Jakarta Sans** across all levels. Its soft, geometric curves provide a modern feel that is exceptionally legible for children and professional enough for adults.

*   **Headlines:** Utilize heavy weights (700-800) to create a clear hierarchy. The display size is reserved for onboarding and major milestone celebrations.
*   **Body Text:** Primarily uses a medium weight (400-500) to ensure readability on mobile screens where budget tracking usually occurs.
*   **Numerics:** When displaying currency, use Bold weights to emphasize financial clarity.

## Layout & Spacing

The layout philosophy follows a **Fluid Grid** model with a soft, spacious rhythm. 

*   **Mobile (Default):** 4-column grid with 20px outside margins. This is the primary target for the app.
*   **Tablet/Desktop:** 12-column grid with a max-width container of 1200px. Content should center-align to avoid excessive line lengths.
*   **Spacing Rhythm:** Based on a 4px baseline, but defaults to 16px (md) for most internal element spacing to maintain an "airy" and "uncluttered" feel that reduces cognitive load when viewing financial data.

## Elevation & Depth

This design system uses **Tonal Layers** combined with **Ambient Shadows** to create a sense of tactile interaction.

1.  **Base Surface:** Neutral #FAFAFA.
2.  **Primary Cards:** Pure White (#FFFFFF) with a very soft, diffused shadow (0px 4px 20px rgba(0,0,0, 0.05)).
3.  **Interactive Elements:** Buttons and active states use a more pronounced shadow to appear "lifted" and ready to be tapped, mimicking a physical button.
4.  **Mascot Depth:** The ant mascot should always appear on the topmost layer, often slightly overlapping cards or headers to break the grid and add playfulness.

## Shapes

The shape language is **distinctly rounded** to reinforce the friendly brand personality. 

*   **Standard Radius:** 0.5rem (8px) for input fields and small chips.
*   **Large Radius:** 1rem (16px) for cards and main containers, creating a "bubble-like" container system.
*   **Interactive Radius:** Buttons and progress bars use a 100px (Pill) radius to feel inviting and safe to touch.

## Components

*   **Buttons:** Primary buttons are pill-shaped, using the Deep Red (#E04E37) with white text. Secondary buttons use a light tint of the primary color with primary text.
*   **Budget Cards:** Large containers with a 16px corner radius. They include a "Goal" progress bar (Emerald) and a small mascot icon indicating "Current Status."
*   **Chips:** Used for transaction categories (e.g., "Food", "Toys"). These use a semi-transparent background of their respective category color (15% opacity) with a fully opaque text label.
*   **Input Fields:** Soft gray borders (1px) that turn Primary Red on focus. Large, legible placeholder text is mandatory for accessibility.
*   **Progress Bars:** Thick, rounded bars. The "track" is a light gray, and the "fill" uses the success Emerald or primary Red depending on the budget health.
*   **Mascot Integration:** The ant mascot appears in empty states (carrying a leaf), error states (looking confused), and success states (holding a gold coin).