package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Moonlit Scavengers
 * {5}{U}
 * Creature — Merfolk Rogue
 * 4/5
 *
 * When this creature enters, if you control an artifact or enchantment, return target creature
 * an opponent controls to its owner's hand.
 *
 * Intervening-if: checked when the trigger would go on the stack and again on resolution.
 */
val MoonlitScavengers = card("Moonlit Scavengers") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Rogue"
    oracleText = "When this creature enters, if you control an artifact or enchantment, return target creature an opponent controls to its owner's hand."
    power = 4
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControl(GameObjectFilter.ArtifactOrEnchantment)
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Miranda Meeks"
        flavorText = "They comb the shallows of Lochmere for relics of a bygone era. And if they happen upon a lone traveler, all the better."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a658bbd-8d64-460b-87ae-ec8054204794.jpg?1783932653"
    }
}
