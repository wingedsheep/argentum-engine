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
 * Helica Glider
 * {2}{W}
 * Creature — Nightmare Squirrel
 * 2/2
 * This creature enters with your choice of a flying counter or a first strike counter on it.
 *
 * A replacement effect, not an enters trigger (CR 614.1c): the same shape as Boot Nipper — an
 * `EntersWithChoice(MODE)` records the choice as it enters (CR 614.12), and one mode-gated
 * `EntersWithCounters` per option puts the chosen keyword counter on it.
 */
val HelicaGlider = card("Helica Glider") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Nightmare Squirrel"
    power = 2
    toughness = 2
    oracleText = "This creature enters with your choice of a flying counter or a first strike counter on it."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(id = "flying", label = "A flying counter"),
                ModeOption(id = "first_strike", label = "A first strike counter"),
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
            counterType = CounterType.FIRST_STRIKE,
            count = 1,
            selfOnly = true,
            condition = SourceChosenModeIs("first_strike"),
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Jehan Choo"
        flavorText = "It could subsist on seeds but its preferred fare is your face."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fc88fe0-2424-49a7-9abf-4773a547d010.jpg?1783931088"
    }
}
