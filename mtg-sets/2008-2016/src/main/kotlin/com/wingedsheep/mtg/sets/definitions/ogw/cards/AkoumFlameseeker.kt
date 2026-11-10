package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Akoum Flameseeker
 * {2}{R}
 * Creature — Human Shaman Ally
 * 3/2
 * Cohort — {T}, Tap an untapped Ally you control: Discard a card. If you do, draw a card.
 */
val AkoumFlameseeker = card("Akoum Flameseeker") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman Ally"
    power = 3
    toughness = 2
    oracleText = "Cohort — {T}, Tap an untapped Ally you control: Discard a card. If you do, draw a card."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.Permanent.withSubtype(Subtype.ALLY)),
        )
        effect = Patterns.Hand.rummage(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Willian Murai"
        flavorText = "In the face of extinction, the most perilous endeavors can be cause for hope."
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ef4e1e2-7961-47d2-bb55-9168ea201214.jpg?1783937908"
    }
}
