package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Stone Docent
 * {1}{W}
 * Creature — Spirit Chimera
 * 3/1
 *
 * {W}, Exile this card from your graveyard: You gain 2 life. Surveil 1.
 * Activate only as a sorcery.
 *
 * The recursion-style payoff is a graveyard-activated ability: the cost is `{W}` plus exiling the
 * card itself from the graveyard (`Costs.ExileSelf` + `activateFromZone = GRAVEYARD`), gated to
 * sorcery speed (`TimingRule.SorcerySpeed`). The effect chains gain-2-life with
 * `Patterns.Library.surveil(1)`.
 */
val StoneDocent = card("Stone Docent") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Chimera"
    power = 3
    toughness = 1
    oracleText = "{W}, Exile this card from your graveyard: You gain 2 life. Surveil 1. " +
        "Activate only as a sorcery. (Look at the top card of your library. You may put it " +
        "into your graveyard.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.ExileSelf)
        effect = Effects.GainLife(2).then(Patterns.Library.surveil(1))
        timing = TimingRule.SorcerySpeed
        activateFromZone = Zone.GRAVEYARD
        description = "{W}, Exile this card from your graveyard: You gain 2 life. Surveil 1. " +
            "Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Inkognit"
        flavorText = "\"For those who might consider cheating, its eyes can see everything. That " +
            "includes you, Esme. Please see me after class.\"\n—Professor Ghostforge"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2abfffb-bf36-44af-9a27-6e109e4d77dd.jpg?1775937164"
    }
}
