package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vampire Scrivener
 * {4}{B}
 * Creature — Vampire Warlock
 * 2 / 2
 * Flying
 * Whenever you gain life during your turn, put a +1/+1 counter on this creature.
 * Whenever you lose life during your turn, put a +1/+1 counter on this creature.
 *
 * "During your turn" is each trigger's timing restriction ([Conditions.IsYourTurn]).
 */
val VampireScrivener = card("Vampire Scrivener") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Warlock"
    oracleText = "Flying\n" +
        "Whenever you gain life during your turn, put a +1/+1 counter on this creature.\n" +
        "Whenever you lose life during your turn, put a +1/+1 counter on this creature."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        triggerRestriction = Conditions.IsYourTurn
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.you.losesLife()
        triggerRestriction = Conditions.IsYourTurn
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Bastien L. Deharme"
        flavorText = "No one remains on her list for long."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/702315d7-dec9-49e8-a508-feacd47198dc.jpg"
    }
}
