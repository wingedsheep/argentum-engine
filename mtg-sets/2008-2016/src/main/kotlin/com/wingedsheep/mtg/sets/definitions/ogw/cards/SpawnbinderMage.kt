package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spawnbinder Mage
 * {3}{W}
 * Creature — Human Wizard Ally
 * 2/4
 * Cohort — {T}, Tap an untapped Ally you control: Tap target creature.
 */
val SpawnbinderMage = card("Spawnbinder Mage") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard Ally"
    power = 2
    toughness = 4
    oracleText = "Cohort — {T}, Tap an untapped Ally you control: Tap target creature."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.Permanent.withSubtype(Subtype.ALLY)),
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Dan Murayama Scott"
        flavorText = "\"Follow me. Step lightly. And resist the urge to kick them.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5677bd49-69eb-41dc-8e64-2df3b7f5c45f.jpg"
    }
}
