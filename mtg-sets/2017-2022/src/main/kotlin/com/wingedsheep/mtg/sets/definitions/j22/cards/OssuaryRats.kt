package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ossuary Rats
 * {5}{B}
 * Creature — Rat
 * 3/2
 *
 * When this creature enters, it deals X damage to target creature or planeswalker an opponent
 * controls, where X is the number of creature cards in your graveyard.
 *
 * X is counted on resolution.
 */
val OssuaryRats = card("Ossuary Rats") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, it deals X damage to target creature or planeswalker an " +
        "opponent controls, where X is the number of creature cards in your graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.DealDamage(DynamicAmounts.creatureCardsInYourGraveyard(), victim)
        description = "When this creature enters, it deals X damage to target creature or planeswalker " +
            "an opponent controls, where X is the number of creature cards in your graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Ralph Horsley"
        flavorText = "You know you've ventured too deep when the rats stop running and hold their ground."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c93dc52c-6f62-45cc-a5e2-95f794e04b94.jpg?1783919186"
    }
}
