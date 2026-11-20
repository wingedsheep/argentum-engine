package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Satyr Piper
 * {2}{G}
 * Creature — Satyr Rogue
 * 2/1
 *
 * {3}{G}: Target creature must be blocked this turn if able.
 */
val SatyrPiper = card("Satyr Piper") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Satyr Rogue"
    power = 2
    toughness = 1
    oracleText = "{3}{G}: Target creature must be blocked this turn if able."

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.MustBeBlocked(creature, allCreatures = false)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Christopher Moeller"
        flavorText = "\"When I asked my commander the reward for killing that prancing nuisance, he told me, 'None! I want to kill him myself!'\"\n—Phrogas, soldier of Akros"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e244c4d0-2c30-4a56-94bb-0fff9ffe4a6a.jpg?1783939738"
    }
}
