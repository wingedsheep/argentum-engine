package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Whisper Squad
 * {B}
 * Creature — Human Soldier
 * 1/1
 *
 * {1}{B}: Search your library for a card named Whisper Squad, put it onto the
 * battlefield tapped, then shuffle.
 */
val WhisperSquad = card("Whisper Squad") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "{1}{B}: Search your library for a card named Whisper Squad, put it onto the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.named("Whisper Squad"),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Stepan Alekseev"
        flavorText = "Ears can't hear them. Eyes see too many of them."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/097eeb32-cfd5-4adb-ac30-d7762e6ea48f.jpg?1783931056"
    }
}
