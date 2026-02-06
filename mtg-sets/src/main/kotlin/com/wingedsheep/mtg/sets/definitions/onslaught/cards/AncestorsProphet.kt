package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ancestor's Prophet
 * {4}{W}
 * Creature — Human Cleric
 * 1/5
 * Tap five untapped Clerics you control: You gain 10 life.
 */
val AncestorsProphet = card("Ancestor's Prophet") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 5

    activatedAbility {
        cost = Costs.TapPermanents(5, GameObjectFilter.Creature.withSubtype("Cleric"))
        effect = GainLifeEffect(10)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Kev Walker"
        flavorText = "\"We have faced horrors of war and terrors beyond imagining. We will overcome the doom of this day as well.\""
        imageUri = "https://cards.scryfall.io/large/front/c/d/cdee956e-76b1-4ba7-a387-2fbfb853507d.jpg?1562943650"
    }
}
