package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Malakir Soothsayer
 * {4}{B}
 * Creature — Vampire Shaman Ally
 * 4/4
 * Cohort — {T}, Tap an untapped Ally you control: You draw a card and you lose 1 life.
 */
val MalakirSoothsayer = card("Malakir Soothsayer") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Shaman Ally"
    power = 4
    toughness = 4
    oracleText = "Cohort — {T}, Tap an untapped Ally you control: You draw a card and you lose 1 life."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.Permanent.withSubtype(Subtype.ALLY)),
        )
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Greg Opalinski"
        flavorText = "\"Your blood whispers secrets your mind could never comprehend.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72cb3c2c-5d5a-4c9b-a381-ab4dd477d84d.jpg"
    }
}
