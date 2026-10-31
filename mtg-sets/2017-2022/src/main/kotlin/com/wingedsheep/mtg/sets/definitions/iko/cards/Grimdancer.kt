package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs

private const val MENACE_DEATHTOUCH = "menace_deathtouch"
private const val MENACE_LIFELINK = "menace_lifelink"
private const val DEATHTOUCH_LIFELINK = "deathtouch_lifelink"

/**
 * Grimdancer
 * {1}{B}{B}
 * Creature — Nightmare
 * 3/3
 * This creature enters with your choice of two different counters on it from among menace,
 * deathtouch, and lifelink.
 *
 * A replacement effect, not an enters trigger (CR 614.1c): the Boot Nipper / Helica Glider shape.
 * Choosing two different counters from three is choosing one of the three pairs, so
 * `EntersWithChoice(MODE)` offers the pairs (CR 614.12), and each keyword counter enters gated on
 * either pair that contains it.
 */
val Grimdancer = card("Grimdancer") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare"
    power = 3
    toughness = 3
    oracleText = "This creature enters with your choice of two different counters on it from among " +
        "menace, deathtouch, and lifelink."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(id = MENACE_DEATHTOUCH, label = "Menace and deathtouch counters"),
                ModeOption(id = MENACE_LIFELINK, label = "Menace and lifelink counters"),
                ModeOption(id = DEATHTOUCH_LIFELINK, label = "Deathtouch and lifelink counters"),
            ),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.MENACE,
            count = 1,
            selfOnly = true,
            condition = AnyCondition(
                listOf(SourceChosenModeIs(MENACE_DEATHTOUCH), SourceChosenModeIs(MENACE_LIFELINK))
            ),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.DEATHTOUCH,
            count = 1,
            selfOnly = true,
            condition = AnyCondition(
                listOf(SourceChosenModeIs(MENACE_DEATHTOUCH), SourceChosenModeIs(DEATHTOUCH_LIFELINK))
            ),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.LIFELINK,
            count = 1,
            selfOnly = true,
            condition = AnyCondition(
                listOf(SourceChosenModeIs(MENACE_LIFELINK), SourceChosenModeIs(DEATHTOUCH_LIFELINK))
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Randy Vargas"
        flavorText = "Most monsters are more frightening when they skitter unseen in the dark. Most."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/819257c8-9806-48bd-ba49-e117ca31de54.jpg?1783931060"
    }
}
