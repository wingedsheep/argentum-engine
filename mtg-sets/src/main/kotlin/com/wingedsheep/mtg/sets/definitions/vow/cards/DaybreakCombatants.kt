package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Daybreak Combatants
 * {2}{R}
 * Creature — Human Warrior
 * 2/2
 * Haste
 * When this creature enters, target creature gets +2/+0 until end of turn.
 */
val DaybreakCombatants = card("Daybreak Combatants") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\nWhen this creature enters, target creature gets +2/+0 until end of turn."
    power = 2
    toughness = 2
    keywords(Keyword.HASTE)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.ModifyStats(2, 0, t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Joshua Raphael"
        flavorText = "After weeks of unending darkness, a glimpse of dawn was all the people of Stensia needed to rekindle their will to fight."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8697a861-0f67-4ed3-bed8-f264fc78565e.jpg?1782703080"
    }
}
