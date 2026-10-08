package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Spectrum Sentinel
 * {1}
 * Artifact Creature — Soldier
 * 1/2
 * Protection from multicolored (This creature can't be blocked, targeted, dealt damage, enchanted,
 * or equipped by anything multicolored.)
 * Whenever a nonbasic land an opponent controls enters, you gain 1 life.
 */
val SpectrumSentinel = card("Spectrum Sentinel") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Soldier"
    power = 1
    toughness = 2
    oracleText = "Protection from multicolored (This creature can't be blocked, targeted, dealt damage, enchanted, or equipped by anything multicolored.)\n" +
        "Whenever a nonbasic land an opponent controls enters, you gain 1 life."

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Multicolored))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.NonbasicLand.opponentControls()).enters()
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "Olivier Bernard"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/936cbe05-7b36-4569-94f3-671c28c7468a.jpg"
    }
}
