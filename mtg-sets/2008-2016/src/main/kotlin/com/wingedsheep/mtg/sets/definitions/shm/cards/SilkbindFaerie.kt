package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Silkbind Faerie
 * {2}{W/U}
 * Creature — Faerie Rogue
 * 1 / 3
 *
 * Flying
 * {1}{W/U}, {Q}: Tap target creature. ({Q} is the untap symbol.)
 *
 * - `{Q}` is [Costs.Untap]: the Faerie must already be **tapped** to pay it, and CR 302.6 gates the
 *   untap symbol behind summoning sickness just like `{T}`. The usual line is to attack, then untap
 *   during your untap step and tap a blocker on the next turn.
 * - The target is any creature, not just an opponent's — the printed text has no controller
 *   restriction, so it is the bare [Targets.Creature] requirement.
 * - The `{W/U}` pip in the activation cost stays hybrid in the string; either {W} or {U} pays it.
 */
val SilkbindFaerie = card("Silkbind Faerie") {
    manaCost = "{2}{W/U}"
    typeLine = "Creature — Faerie Rogue"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "{1}{W/U}, {Q}: Tap target creature. ({Q} is the untap symbol.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W/U}"), Costs.Untap)
        val t = target("target", Targets.Creature)
        effect = Effects.Tap(t)
        description = "{1}{W/U}, {Q}: Tap target creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Matt Cavotta"
        flavorText = "\"The bigger they are, the more fun it is to watch them fall flat on their faces.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12d06328-a124-40e4-a9d8-2342a881970e.jpg?1783942736"
    }
}
