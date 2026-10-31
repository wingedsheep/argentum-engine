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
 * Ferocious Tigorilla
 * {3}{R}
 * Creature — Cat Ape
 * 4/3
 * This creature enters with your choice of a trample counter or a menace counter on it.
 *
 * A replacement effect, not an enters trigger (CR 614.1c): the Helica Glider shape — an
 * `EntersWithChoice(MODE)` records the choice as it enters (CR 614.12), and one mode-gated
 * `EntersWithCounters` per option puts the chosen keyword counter on it.
 */
val FerociousTigorilla = card("Ferocious Tigorilla") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cat Ape"
    power = 4
    toughness = 3
    oracleText = "This creature enters with your choice of a trample counter or a menace counter on it."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(id = "trample", label = "A trample counter"),
                ModeOption(id = "menace", label = "A menace counter"),
            ),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.TRAMPLE,
            count = 1,
            selfOnly = true,
            condition = SourceChosenModeIs("trample"),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.MENACE,
            count = 1,
            selfOnly = true,
            condition = SourceChosenModeIs("menace"),
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Antonio José Manzanedo"
        flavorText = "From a certain perspective, it's an inspiring story of overcoming obstacles."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5976e8d8-5122-4423-b3eb-61170390ad7a.jpg?1783931051"
    }
}
