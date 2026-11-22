package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Corpse Hauler
 * {1}{B}
 * Creature — Human Rogue
 * 2/1
 * {2}{B}, Sacrifice this creature: Return target creature card from your graveyard to your hand.
 */
val CorpseHauler = card("Corpse Hauler") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "{2}{B}, Sacrifice this creature: Return target creature card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.SacrificeSelf)
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Jesper Ejsing"
        flavorText = "\"He expects a reward, but the only piece of silver he'll get from me is my blade in his gut.\"\n—Shelac, necromancer"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca6adc5e-9221-4a18-8d41-4675797e5d46.jpg?1783939925"
    }
}
