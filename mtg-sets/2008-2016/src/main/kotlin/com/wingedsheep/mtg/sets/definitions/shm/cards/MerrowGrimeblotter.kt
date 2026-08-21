package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Merrow Grimeblotter
 * {3}{U/B}
 * Creature — Merfolk Wizard
 * 2 / 2
 *
 * {1}{U/B}, {Q}: Target creature gets -2/-0 until end of turn. ({Q} is the untap symbol.)
 *
 * - `{Q}` is [Costs.Untap]: the Grimeblotter must already be **tapped** to pay it, and — like `{T}`
 *   — CR 302.6 gates it behind summoning sickness. That makes it a once-per-turn-cycle ability in
 *   practice (tap it for something, untap it in your untap step, activate).
 * - The `{U/B}` pip in the activation cost stays hybrid: the parser reads it out of the string, so
 *   either {U} or {B} pays it. Nothing is overridden.
 * - The stat modifier is authored as -2/-0 rather than -2/0 in text only; the effect carries a
 *   toughness modifier of exactly 0, so it never touches toughness (a 1/1 target survives at 0/1).
 */
val MerrowGrimeblotter = card("Merrow Grimeblotter") {
    manaCost = "{3}{U/B}"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "{1}{U/B}, {Q}: Target creature gets -2/-0 until end of turn. ({Q} is the untap symbol.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U/B}"), Costs.Untap)
        val t = target("target", Targets.Creature)
        effect = Effects.ModifyStats(-2, 0, t)
        description = "{1}{U/B}, {Q}: Target creature gets -2/-0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Cyril Van Der Haegen"
        flavorText = "Grimeblotters spend so much time in the Dark Meanders that they're able to bring a piece with them wherever they go."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b839c9d0-ef08-4661-8009-3bcb256bf508.jpg?1783942730"
    }
}
