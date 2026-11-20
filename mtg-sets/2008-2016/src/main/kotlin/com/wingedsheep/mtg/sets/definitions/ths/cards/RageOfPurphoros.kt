package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rage of Purphoros
 * {4}{R}
 * Sorcery
 *
 * Rage of Purphoros deals 4 damage to target creature. It can't be regenerated this turn.
 * Scry 1. (Look at the top card of your library. You may put that card on the bottom.)
 */
val RageOfPurphoros = card("Rage of Purphoros") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Rage of Purphoros deals 4 damage to target creature. It can't be regenerated this turn. " +
        "Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBeRegenerated(creature) then
            Effects.DealDamage(4, creature) then
            Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e249f31-cc67-4d0c-9db5-962d10cf74ca.jpg?1783939756"
    }
}
