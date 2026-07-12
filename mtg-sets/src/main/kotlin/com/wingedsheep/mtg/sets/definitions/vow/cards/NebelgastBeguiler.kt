package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Nebelgast Beguiler
 * {4}{W}
 * Creature — Spirit
 * 2/5
 * {W}, {T}: Tap target creature.
 */
val NebelgastBeguiler = card("Nebelgast Beguiler") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    oracleText = "{W}, {T}: Tap target creature."
    power = 2
    toughness = 5
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Tap(t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Andreas Zafiratos"
        flavorText = "A moment of distraction, an hour off course."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04c68dbd-e61b-49a7-aa17-da6b26c9fd29.jpg?1782703176"
    }
}
