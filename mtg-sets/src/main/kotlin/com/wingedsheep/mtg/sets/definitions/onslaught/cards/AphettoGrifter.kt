package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TapUntapEffect

/**
 * Aphetto Grifter
 * {2}{U}
 * Creature — Human Wizard
 * 1/1
 * Tap two untapped Wizards you control: Tap target permanent.
 */
val AphettoGrifter = card("Aphetto Grifter") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Creature.withSubtype("Wizard"))
        target = Targets.Permanent
        effect = TapUntapEffect(
            target = EffectTarget.ContextTarget(0),
            tap = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Greg Staples"
        flavorText = "Aphetto con artists started working in pairs to make it less likely they'd be the victims of con artists."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a7a7bf3-1b0c-415d-9c57-73ac55b1f915.jpg?1562908758"
    }
}
