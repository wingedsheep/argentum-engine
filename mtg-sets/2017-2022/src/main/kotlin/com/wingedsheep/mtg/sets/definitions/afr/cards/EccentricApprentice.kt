package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eccentric Apprentice
 * {2}{U}
 * Creature — Tiefling Wizard
 * 2/2
 * Flying
 * When this creature enters, venture into the dungeon. (Enter the first room or advance to the next room.)
 * At the beginning of combat on your turn, if you've completed a dungeon, up to one target creature
 * becomes a Bird with base power and toughness 1/1 and flying until end of turn.
 *
 * "If you've completed a dungeon" is an intervening-if on the combat trigger. The Bird transformation
 * is the same until-end-of-turn stack as Mordenkainen's Polymorph: subtypes replaced with Bird
 * (layer 4, it loses its other creature types), base P/T set to 1/1 (layer 7b) and flying granted
 * (layer 6). Its abilities are untouched.
 */
val EccentricApprentice = card("Eccentric Apprentice") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Tiefling Wizard"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, venture into the dungeon. (Enter the first room or advance to the next room.)\n" +
        "At the beginning of combat on your turn, if you've completed a dungeon, up to one target creature " +
        "becomes a Bird with base power and toughness 1/1 and flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.VentureIntoTheDungeon()
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouCompletedADungeon
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.SetCreatureSubtypes(
            subtypes = setOf("Bird"),
            target = creature,
            duration = Duration.EndOfTurn
        ) then
            Effects.SetBasePowerAndToughness(
                power = 1,
                toughness = 1,
                target = creature,
                duration = Duration.EndOfTurn
            ) then
            Effects.GrantKeyword(
                keyword = Keyword.FLYING,
                target = creature,
                duration = Duration.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3baa08ac-9a94-4e22-91bb-c6966cd0a0de.jpg?1783926516"
        ruling("2021-07-23", "Eccentric Apprentice's last ability works even if it wasn't on the battlefield when you completed a dungeon and even if the dungeon was completed on a previous turn.")
        ruling("2021-07-23", "A creature that becomes a Bird this way loses all other creature types but does not lose any of its abilities.")
        ruling("2021-07-23", "The last ability will overwrite any previous effects that set the creature's power and toughness to specific numbers. Effects that otherwise modify the target creature's power and toughness will still apply no matter when they took effect. The same is true for +1/+1 counters.")
    }
}
