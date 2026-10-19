package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Old Gnawbone
 * {5}{G}{G}
 * Legendary Creature — Dragon
 * 7/7
 * Flying
 * Whenever a creature you control deals combat damage to a player, create that many Treasure tokens.
 */
val OldGnawbone = card("Old Gnawbone") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Dragon"
    oracleText = "Flying\nWhenever a creature you control deals combat damage to a player, create that many Treasure tokens."
    power = 7
    toughness = 7
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateTreasure(DynamicAmounts.triggerDamageAmount())
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "197"
        artist = "Filip Burburan"
        flavorText = "The ancient green dragon Claugiyliamatar is often seen with a mangled corpse dangling from her mouth."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77ceba8b-de19-4db8-b5a7-f5df49bf241f.jpg?1783926458"
    }
}
