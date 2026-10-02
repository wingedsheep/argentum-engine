package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vivisection Evangelist
 * {3}{W}{B}
 * Creature — Phyrexian Cleric
 * 4/4
 * Vigilance
 * Corrupted — When this creature enters, if an opponent has three or more poison counters,
 * destroy target creature or planeswalker an opponent controls.
 *
 * The corrupted clause is an intervening "if": checked when the trigger would fire and again
 * on resolution.
 */
val VivisectionEvangelist = card("Vivisection Evangelist") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Phyrexian Cleric"
    oracleText = "Vigilance\nCorrupted — When this creature enters, if an opponent has three or more poison counters, destroy target creature or planeswalker an opponent controls."
    power = 4
    toughness = 4

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.Corrupted
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.Destroy(victim)
        description = "Corrupted — When this creature enters, if an opponent has three or more poison counters, destroy target creature or planeswalker an opponent controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Igor Kieryluk"
        flavorText = "\"May you rejoice in the magnificence of Norn. May your flesh serve perfection.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/2/626c46a3-72b8-4e04-adf2-c9c7aaf94f04.jpg?1783917995"
    }
}
