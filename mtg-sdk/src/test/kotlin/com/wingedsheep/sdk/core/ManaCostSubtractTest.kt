package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * [ManaCost.subtract] — the pip-wise cost reduction of CR 118.7, used by power-up
 * (CR 702.193b: "Generic mana in the permanent's mana cost reduces generic mana in the cost to
 * activate its power-up ability. Colored and colorless mana … reduces mana of the same type, and
 * any excess reduces that much generic mana") and, identically worded, by offering (CR 702.48c).
 *
 * The first block is the ground truth: every power-up ability printed in Marvel Super Heroes,
 * reduced by its own permanent's mana cost. If the arithmetic here is wrong, the mechanic is wrong
 * on a real card. The blocks after it pin the CR 118.7 subrules that the printed set happens not
 * to exercise.
 */
class ManaCostSubtractTest : StringSpec({

    fun subtract(cost: String, reduction: String) =
        ManaCost.parse(cost).subtract(ManaCost.parse(reduction))

    fun check(cost: String, reduction: String, expected: String) =
        subtract(cost, reduction) shouldBe ManaCost.parse(expected)

    // ── Every printed MSH power-up ability, reduced by its source's mana cost ──────────────

    "Brave Brawler {4}{W} reduced by {1}{W} is {3}" { check("{4}{W}", "{1}{W}", "{3}") }
    "Captain Marvel {5}{W}{W} reduced by {3}{W}{W} is {2}" { check("{5}{W}{W}", "{3}{W}{W}", "{2}") }
    "Nick Fury {W}{U}{B}{R}{G} reduced by {W} is {U}{B}{R}{G}" {
        check("{W}{U}{B}{R}{G}", "{W}", "{U}{B}{R}{G}")
    }
    "Aerial Doombot {5}{U} reduced by {U} is {5}" { check("{5}{U}", "{U}", "{5}") }
    "Bold Biochemist {5}{U} reduced by {1}{U} is {4}" { check("{5}{U}", "{1}{U}", "{4}") }
    "Kang {5}{U}{U}{U} reduced by {2}{U}{U} is {3}{U}" { check("{5}{U}{U}{U}", "{2}{U}{U}", "{3}{U}") }
    "Ninja of the Hand {4}{B} reduced by {2}{B} is {2}" { check("{4}{B}", "{2}{B}", "{2}") }
    "Unliving Legionnaire {5}{B}{B} reduced by {3}{B} is {2}{B}" {
        check("{5}{B}{B}", "{3}{B}", "{2}{B}")
    }
    "Human Torch {6}{R} reduced by {2}{R} is {4}" { check("{6}{R}", "{2}{R}", "{4}") }
    "Loki Laufeyson {4}{R} reduced by {1}{R} is {3}" { check("{4}{R}", "{1}{R}", "{3}") }
    "Quicksilver {4}{R} reduced by {R} is {4}" { check("{4}{R}", "{R}", "{4}") }
    "Volcanic Villain {5}{R} reduced by {2}{R} is {3}" { check("{5}{R}", "{2}{R}", "{3}") }
    "Wonder Man {5}{R}{R} reduced by {3}{R}{R} is {2}" { check("{5}{R}{R}", "{3}{R}{R}", "{2}") }
    "Hercules {4}{G} reduced by {2}{G} is {2}" { check("{4}{G}", "{2}{G}", "{2}") }
    "Pet Avengers {6}{G} reduced by {3}{G} is {3}" { check("{6}{G}", "{3}{G}", "{3}") }
    "Serpent Specialist {3}{G} reduced by {G} is {3}" { check("{3}{G}", "{G}", "{3}") }
    "She-Hulk {4}{G}{G} reduced by {3}{G} is {1}{G}" { check("{4}{G}{G}", "{3}{G}", "{1}{G}") }
    "White Tiger {5}{G} reduced by {1}{G} is {4}" { check("{5}{G}", "{1}{G}", "{4}") }
    "Hulk {6}{R}{G} reduced by {3}{R}{G} is {3}" { check("{6}{R}{G}", "{3}{R}{G}", "{3}") }
    "Ultron Drone {6} reduced by {3} is {3}" { check("{6}", "{3}", "{3}") }
    "Viv Vision {7} reduced by {3} is {4}" { check("{7}", "{3}", "{4}") }

    // The three that carry a symbol kind the rest of the cycle doesn't.

    "Stature {X}{U}{U} reduced by {U} keeps the X: {X}{U}" { check("{X}{U}{U}", "{U}", "{X}{U}") }
    "Abomination {5}{R/G}{R/G} reduced by {3}{R/G} is {2}{R/G} — hybrid cancels hybrid" {
        check("{5}{R/G}{R/G}", "{3}{R/G}", "{2}{R/G}")
    }
    "Thanos {C}{W}{U}{B}{R}{G} reduced by {R}{W}{B} is {C}{U}{G} — colorless pip survives" {
        check("{C}{W}{U}{B}{R}{G}", "{R}{W}{B}", "{C}{U}{G}")
    }

    // ── CR 118.7a: generic in the reduction reduces generic only ──────────────────────────

    "generic reduction never touches a colored pip" { check("{2}{U}{U}", "{5}", "{U}{U}") }
    "generic reduction never touches a colorless pip" { check("{2}{C}", "{5}", "{C}") }
    "a cost reduced to nothing is {0}, never negative (CR 118.7)" {
        subtract("{3}", "{9}") shouldBe ManaCost.ZERO
    }

    // ── CR 118.7b/c: colored reduction with no pip of that color spills into generic ───────

    "colored reduction with no matching pip reduces one generic instead" {
        check("{3}{U}", "{B}", "{2}{U}")
    }
    "excess colored reduction spills the difference into generic (CR 118.7c)" {
        check("{4}{R}", "{R}{R}{R}", "{2}")
    }
    "colored spill is floored at zero rather than eating colored pips" {
        check("{W}{W}", "{B}{B}{B}", "{W}{W}")
    }

    // ── CR 118.7d: colorless reduction behaves the same way ───────────────────────────────

    "colorless reduction cancels a colorless pip" { check("{2}{C}{C}", "{C}", "{2}{C}") }
    "excess colorless reduction spills into generic (CR 118.7d)" { check("{3}{C}", "{C}{C}", "{2}") }
    "a colorless reduction does not pay a colored pip" { check("{3}{U}", "{C}", "{2}{U}") }

    // ── CR 118.7e/f: hybrid and Phyrexian pips ────────────────────────────────────────────

    "an unmatched hybrid pays one of its colored halves" { check("{2}{R}{G}", "{R/G}", "{2}{G}") }
    "a hybrid prefers an identical hybrid over a colored half" {
        // Both {R/G} pips get spent: the first cancels the identical hybrid, the second falls back
        // to the {R}. Matching colored-half-first would waste a pip and leave {R/G} behind.
        check("{R}{R/G}", "{R/G}{R/G}", "")
    }
    "hybrids are assigned to cancel as many pips as possible, not first-fit" {
        // {W/U} taking the {W} would strand {W/B} on a generic spill. The assignment that reduces
        // most is {W/U} -> {U} and {W/B} -> {W}, which cancels the whole cost.
        check("{W}{U}", "{W/U}{W/B}", "")
    }
    "a hybrid with neither half present spills one generic" { check("{3}{W}", "{R/G}", "{2}{W}") }
    "a Phyrexian reduction pays a pip of its color (CR 118.7f)" { check("{2}{G}", "{G/P}", "{2}") }
    "a colored reduction pays a Phyrexian pip of the same color" { check("{2}{G/P}", "{G}", "{2}") }
    "a monocolored hybrid with no pip of its color spills its generic half" {
        check("{4}{W}", "{2/B}", "{2}{W}")
    }
    "a monocolored hybrid takes its generic half when that half reduces more (CR 118.7e)" {
        // The colored half would remove one mana and leave {3}; the generic half removes two.
        check("{3}{W}", "{2/W}", "{1}{W}")
    }
    "a monocolored hybrid takes its colored half when the generic half can't be spent in full" {
        // Only one generic to remove, so both halves are worth one mana — take the colored half,
        // which also clears the color requirement.
        check("{1}{W}", "{2/W}", "{1}")
    }
    "a monocolored hybrid takes its colored half when the cost has no generic at all" {
        check("{W}{U}", "{2/W}", "{U}")
    }

    // ── {X} is inert on both sides ────────────────────────────────────────────────────────

    "an X in the reduction contributes nothing — a permanent's printed X is 0 (CR 202.3b)" {
        // Only the {R} is spent; the {X} reduces neither generic nor colored mana.
        check("{4}{R}", "{X}{R}", "{4}")
    }
    "an X in the cost is never consumed by a colored reduction" { check("{X}{G}", "{G}", "{X}") }

    // ── Degenerate inputs ─────────────────────────────────────────────────────────────────

    "subtracting nothing is identity" { check("{2}{U}", "", "{2}{U}") }
    "subtracting from nothing is nothing" { subtract("", "{2}{U}") shouldBe ManaCost.ZERO }
    "subtracting a cost from itself is {0}" { check("{3}{W}{U}", "{3}{W}{U}", "") }
})
