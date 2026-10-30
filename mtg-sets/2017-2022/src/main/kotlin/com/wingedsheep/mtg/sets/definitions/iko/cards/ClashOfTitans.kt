package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Clash of Titans
 * {3}{R}{R}
 * Instant
 * Target creature fights another target creature.
 *
 * One two-slot creature requirement — its slots must be distinct objects, which is the "another".
 * The two may share a controller (ruling).
 */
val ClashOfTitans = card("Clash of Titans") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature fights another target creature. (Each deals damage equal to its power to the other.)"

    spell {
        val (first, second) = targets(TargetFilter.Creature, count = 2)
        effect = Effects.Fight(first, second)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Viktor Titov"
        flavorText = "\"Let the brutes fight. If they kill each other, we lose no more soldiers today.\"\n—Orthion, Lavabrink captain"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca10e2b8-a317-4c98-a866-43bf274e82cb.jpg?1783931052"
        ruling("2020-04-17", "Clash of Titans can target two creatures controlled by the same player.")
        ruling("2020-04-17", "If either target is an illegal target as Clash of Titans tries to resolve, neither creature will deal or be dealt damage.")
    }
}
