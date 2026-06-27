package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.sneak
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Leonardo, Sewer Samurai
 * {3}{W}
 * Legendary Creature — Mutant Ninja Turtle Samurai
 * 3/3
 *
 * Sneak {2}{W}{W}
 * Double strike
 * During your turn, you may cast creature spells with power or toughness 1 or less
 * from your graveyard. If you cast a spell this way, that creature enters with a
 * finality counter on it. (If a creature with a finality counter on it would die,
 * exile it instead.)
 */
val LeonardoSewerSamurai = card("Leonardo, Sewer Samurai") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Mutant Ninja Turtle Samurai"
    oracleText = "Sneak {2}{W}{W} (You may cast this spell for {2}{W}{W} if you also return an unblocked attacker you control to hand during the declare blockers step.)\nDouble strike\nDuring your turn, you may cast creature spells with power or toughness 1 or less from your graveyard. If you cast a spell this way, that creature enters with a finality counter on it. (If a creature with a finality counter on it would die, exile it instead.)"
    power = 3
    toughness = 3

    keywords(Keyword.DOUBLE_STRIKE)
    sneak("{2}{W}{W}")

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.Creature.powerOrToughnessAtMost(1),
            duringYourTurnOnly = true
        )
    }

    // Creatures you control cast from the graveyard (the only graveyard-cast permission here is
    // Leonardo's) enter with a finality counter. selfOnly = false applies the replacement to the
    // *other* creature it casts; WasCastFromGraveyard is evaluated against that entering creature.
    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.FINALITY),
            count = 1,
            selfOnly = false,
            condition = Conditions.WasCastFromGraveyard
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "17"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e3a0a26-c163-4f31-bed8-1be52a35feea.jpg?1764512256"
    }
}
