package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Warkite Marauder {1}{U}
 * Creature — Human Pirate
 * 2/1
 * Flying
 * Whenever this creature attacks, target creature defending player controls loses all abilities
 * and has base power and toughness 0/1 until end of turn.
 */
val WarkiteMarauder = card("Warkite Marauder") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhenever this creature attacks, target creature defending player controls loses all " +
        "abilities and has base power and toughness 0/1 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val victim = target(TargetFilter(GameObjectFilter.Creature.defendingPlayerControls()))
        effect = Effects.RemoveAllAbilities(victim, Duration.EndOfTurn) then
            Effects.SetBasePowerAndToughness(0, 1, victim, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/802ff9cf-f739-40cb-8974-77f6850150eb.jpg?1783935317"
        ruling("2018-01-19", "Warkite Marauder's ability overwrites all previous effects that set the creature's " +
            "base power and toughness to specific values. Any power- or toughness-setting effects that start to " +
            "apply after the ability resolves will overwrite this effect.")
        ruling("2018-01-19", "If the affected creature gains an ability after Warkite Marauder's ability resolves, " +
            "it will keep that ability. If the affected creature has an ability that grants abilities to other " +
            "objects, Warkite Marauder's effect will stop it from doing so.")
        ruling("2018-01-19", "Effects that modify a creature's power and/or toughness, such as the effect of " +
            "Titanic Growth, will apply to the creature no matter when they started to take effect. The same is " +
            "true for any counters that change its power and/or toughness and effects that switch its power and " +
            "toughness.")
    }
}
