package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deadly Visit (GRN 68)
 * {3}{B}{B} — Sorcery
 *
 * Destroy target creature.
 * Surveil 2.
 */
val DeadlyVisit = card("Deadly Visit") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature.\nSurveil 2. (Look at the top two cards of your library, then put " +
        "any number of them into your graveyard and the rest on top of your library in any order.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then Effects.Surveil(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/462fe190-5264-42d8-bd27-23c5aa0c641f.jpg?1783934178"
        ruling("2018-10-05", "You'll surveil before resolving any abilities that trigger on the target creature dying.")
        ruling("2018-10-05", "Deadly Visit can target a creature that's indestructible. It won't be destroyed, but you'll surveil.")
        ruling("2024-01-12", "Some spells that instruct you to surveil require targets. You can't cast a spell without choosing legal targets. If all of those targets become illegal, the spell doesn't resolve and you won't surveil.")
    }
}
