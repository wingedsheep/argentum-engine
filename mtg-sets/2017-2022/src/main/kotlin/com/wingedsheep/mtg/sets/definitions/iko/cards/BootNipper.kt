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
 * Boot Nipper
 * {1}{B}
 * Creature — Beast
 * 2/1
 * This creature enters with your choice of a deathtouch counter or a lifelink counter on it.
 *
 * A replacement effect, not an enters trigger (CR 614.1c): the riot shape — an
 * `EntersWithChoice(MODE)` records the choice as it enters (CR 614.12), and one mode-gated
 * `EntersWithCounters` per option puts the chosen keyword counter on it.
 */
val BootNipper = card("Boot Nipper") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 1
    oracleText = "This creature enters with your choice of a deathtouch counter or a lifelink counter on it."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(id = "deathtouch", label = "A deathtouch counter"),
                ModeOption(id = "lifelink", label = "A lifelink counter"),
            ),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.DEATHTOUCH,
            count = 1,
            selfOnly = true,
            condition = SourceChosenModeIs("deathtouch"),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.LIFELINK,
            count = 1,
            selfOnly = true,
            condition = SourceChosenModeIs("lifelink"),
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Nicholas Gregory"
        flavorText = "\"I have a number of hyper-specific phobias and this somehow hits all of them.\"\n—Aaru, Zagoth lumberjack"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cff5a5b8-f823-4429-acd8-c4f34a676cb4.jpg?1783931066"
    }
}
