package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Serpent-Blade Assailant
 * {2}{G}
 * Creature — Elf Warrior
 * 2/1
 * Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another
 * creature, it gains the following ability until end of turn.)
 * Deathtouch
 */
val SerpentBladeAssailant = card("Serpent-Blade Assailant") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following ability until end of turn.)\n" +
        "Deathtouch"
    power = 2
    toughness = 1

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.DEATHTOUCH, creature),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following ability until end of turn.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "205"
        artist = "Durion"
        flavorText = "Kaldheim's cosmos was never yours to corrupt."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6fabc49f-b665-4c56-b06e-03220f7918bb.jpg?1783916961"
    }
}
