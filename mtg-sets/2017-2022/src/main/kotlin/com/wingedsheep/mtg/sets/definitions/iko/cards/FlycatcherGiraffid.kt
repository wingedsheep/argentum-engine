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
 * Flycatcher Giraffid
 * {4}{G}
 * Creature — Antelope Lizard
 * 3/5
 * This creature enters with your choice of a reach counter or a vigilance counter on it.
 *
 * Same shape as Boot Nipper: an `EntersWithChoice(MODE)` replacement records the choice as it
 * enters, and one mode-gated `EntersWithCounters` per option puts the chosen keyword counter on it.
 */
val FlycatcherGiraffid = card("Flycatcher Giraffid") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Antelope Lizard"
    power = 3
    toughness = 5
    oracleText = "This creature enters with your choice of a reach counter or a vigilance counter on it."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(id = "reach", label = "A reach counter"),
                ModeOption(id = "vigilance", label = "A vigilance counter"),
            ),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.REACH,
            count = 1,
            selfOnly = true,
            condition = SourceChosenModeIs("reach"),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.VIGILANCE,
            count = 1,
            selfOnly = true,
            condition = SourceChosenModeIs("vigilance"),
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Dan Murayama Scott"
        flavorText = "Poachers use the harvested tongues to snare pests, plumb caves, and scale tall buildings."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/1988e220-d746-46e4-a534-164203e63c14.jpg?1783931036"
    }
}
