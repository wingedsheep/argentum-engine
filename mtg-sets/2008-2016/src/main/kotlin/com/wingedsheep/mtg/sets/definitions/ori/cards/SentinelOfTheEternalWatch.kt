package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sentinel of the Eternal Watch
 * {5}{W}
 * Creature — Giant Soldier
 * 4/6
 *
 * Vigilance (Attacking doesn't cause this creature to tap.)
 * At the beginning of combat on each opponent's turn, tap target creature that player controls.
 */
val SentinelOfTheEternalWatch = card("Sentinel of the Eternal Watch") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant Soldier"
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "At the beginning of combat on each opponent's turn, tap target creature that player controls."
    power = 4
    toughness = 6

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.BEGIN_COMBAT)
        val t = target(TargetFilter(GameObjectFilter.Creature.controlledByTriggeringPlayer()))
        effect = Effects.Tap(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Bastien L. Deharme"
        flavorText = "The portcullis is superfluous."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a7b01ce-2729-4b70-ae0a-b9a1007be78f.jpg?1783938358"
    }
}
