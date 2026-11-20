package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Soldier of the Pantheon
 * {W}
 * Creature — Human Soldier
 * 2/1
 * Protection from multicolored
 * Whenever an opponent casts a multicolored spell, you gain 1 life.
 */
val SoldierOfThePantheon = card("Soldier of the Pantheon") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "Protection from multicolored\n" +
        "Whenever an opponent casts a multicolored spell, you gain 1 life."

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Multicolored))

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Multicolored)
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Eric Deschamps"
        flavorText = "\"I hear the gods' voices in my dreams each night, and I offer bloody trophies on their altars each day.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cfc0f46-4df4-4ae3-bfe6-391916eb590f.jpg?1783939806"
    }
}
