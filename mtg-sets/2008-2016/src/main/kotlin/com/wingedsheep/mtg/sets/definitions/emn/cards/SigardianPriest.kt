package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sigardian Priest
 * {1}{W}
 * Creature — Human Cleric
 * 1/2
 * {1}, {T}: Tap target non-Human creature.
 */
val SigardianPriest = card("Sigardian Priest") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "{1}, {T}: Tap target non-Human creature."
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val creature = target(TargetFilter(GameObjectFilter.Creature.notSubtype(Subtype("Human"))))
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Terese Nielsen"
        flavorText = "Many whose faith was destroyed by Avacyn turned to the only angel who remained " +
            "true to humanity."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8e798c7-0cbc-444e-8033-629baec7dca8.jpg"
    }
}
