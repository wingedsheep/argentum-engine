package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Obsessive Skinner (Shadows over Innistrad #220)
 * {1}{G}
 * Creature — Human Rogue
 * 1/1
 *
 * When this creature enters, put a +1/+1 counter on target creature.
 * Delirium — At the beginning of each opponent's upkeep, if there are four or more card types among
 * cards in your graveyard, put a +1/+1 counter on target creature.
 */
val ObsessiveSkinner = card("Obsessive Skinner") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, put a +1/+1 counter on target creature.\n" +
        "Delirium — At the beginning of each opponent's upkeep, if there are four or more card types " +
        "among cards in your graveyard, put a +1/+1 counter on target creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.Delirium()
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2cc3b9d-4b32-4b2e-9027-876210ccf18e.jpg"
    }
}
