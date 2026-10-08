package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gixian Skullflayer
 * {2}{B}
 * Creature — Phyrexian Human Assassin
 * 2/3
 * At the beginning of your upkeep, if there are three or more creature cards in your graveyard,
 * put a +1/+1 counter on this creature.
 */
val GixianSkullflayer = card("Gixian Skullflayer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Human Assassin"
    power = 2
    toughness = 3
    oracleText = "At the beginning of your upkeep, if there are three or more creature cards in your " +
        "graveyard, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CreatureCardsInGraveyardAtLeast(3)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Anna Pavleeva"
        flavorText = "\"With every culling, we bring this world closer to *compleation*.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebdcf02b-1876-44d7-8a22-679ea1272856.jpg"
    }
}
