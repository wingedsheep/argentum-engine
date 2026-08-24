package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dawnstrider
 * {1}{G}
 * Creature — Dryad Spellshaper
 * 1 / 1
 * {G}, {T}, Discard a card: Prevent all combat damage that would be dealt this turn.
 *
 * A repeatable Fog on a body — the effect is the same [Effects.PreventAllCombatDamage] shield
 * Fog installs (a turn-scoped `PreventionScope.CombatOnly`).
 */
val Dawnstrider = card("Dawnstrider") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad Spellshaper"
    oracleText = "{G}, {T}, Discard a card: Prevent all combat damage that would be dealt this turn."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap, Costs.DiscardCard)
        effect = Effects.PreventAllCombatDamage()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "rk post"
        flavorText = "Morning's mists can hide many things."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d193a35-8950-4a77-ace3-c4d4085727f4.jpg"
    }
}
