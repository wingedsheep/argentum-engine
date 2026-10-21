package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gavony Trapper
 * {W}
 * Creature — Human Soldier
 * 0/2
 * {2}, {T}: Tap target creature.
 */
val GavonyTrapper = card("Gavony Trapper") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 0
    toughness = 2
    oracleText = "{2}, {T}: Tap target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Caio Monteiro"
        flavorText = "The elite cathars of the Parish-Blades train in all manner of techniques designed to delay and frustrate the beasts that hunt the crossways."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a1309ea-4411-4116-841f-5aef8611400c.jpg"
    }
}
