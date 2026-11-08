package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blood Feud
 * {4}{R}{R}
 * Sorcery
 * Target creature fights another target creature.
 *
 * Clash of Titans's shape: one two-slot creature requirement — its slots must be distinct
 * objects, which is the "another". The two may share a controller (ruling).
 */
val BloodFeud = card("Blood Feud") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature fights another target creature. (Each deals damage equal to its power to the other.)"

    spell {
        val (first, second) = targets(TargetFilter.Creature, count = 2)
        effect = Effects.Fight(first, second)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Winona Nelson"
        flavorText = "Succession is a matter of blood, and by blood it is often decided."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/634d59b8-6046-4796-95c5-eec75a239986.jpg?1783940821"
        ruling("2011-01-22", "Blood Feud can target two creatures with the same controller.")
    }
}
