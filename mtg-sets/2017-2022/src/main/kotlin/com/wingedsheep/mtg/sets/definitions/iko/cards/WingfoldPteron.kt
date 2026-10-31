package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs

/**
 * Wingfold Pteron
 * {5}{U}
 * Creature — Dinosaur
 * 3/6
 * This creature enters with your choice of a flying counter or a hexproof counter on it.
 *
 * A replacement effect, not an enters trigger (CR 614.1c): the same shape as Helica Glider and
 * Boot Nipper — an `EntersWithChoice(MODE)` records the choice as it enters, and one mode-gated
 * `EntersWithCounters` per option puts the chosen keyword counter on it.
 */
val WingfoldPteron = card("Wingfold Pteron") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 6
    oracleText = "This creature enters with your choice of a flying counter or a hexproof counter on it. " +
        "(A creature with hexproof can't be the target of spells or abilities your opponents control.)"

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(id = "flying", label = "A flying counter"),
                ModeOption(id = "hexproof", label = "A hexproof counter"),
            ),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.FLYING,
            count = 1,
            selfOnly = true,
            condition = SourceChosenModeIs("flying"),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.HEXPROOF,
            count = 1,
            selfOnly = true,
            condition = SourceChosenModeIs("hexproof"),
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Johann Bodin"
        flavorText = "\"Its weak spot is easily spotted, but only if it's swooping down on you.\"\n—Jonald, mission naturalist"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11e21a5f-fae7-4488-9463-f0af6e4c5233.jpg?1783931068"
    }
}
