package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skirsdag Flayer
 * {1}{B}
 * Creature — Human Cleric
 * 1/1
 * {3}{B}, {T}, Sacrifice a Human: Destroy target creature.
 */
val SkirsdagFlayer = card("Skirsdag Flayer") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "{3}{B}, {T}, Sacrifice a Human: Destroy target creature."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{B}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN))
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Austin Hsu"
        flavorText = "Certain initiates into the Skirsdag cult take a vow of blindness, pledging obedience to none but demonkind."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/274976b0-2bb5-46e6-b62e-b50d80a77e28.jpg"
    }
}
