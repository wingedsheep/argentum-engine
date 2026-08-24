package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Festival
 * {W}
 * Instant
 * Cast this spell only during an opponent's upkeep.
 * Creatures can't attack this turn.
 *
 * "An opponent's upkeep" is two independent restrictions, and both have to hold: the upkeep step,
 * and a turn that isn't yours. Splitting them this way rather than inventing a combined restriction
 * keeps each half readable and reuses what the engine already enforces.
 *
 * The fog half is `CantAttackGroup` over every creature, which the executor installs as a floating
 * rule-modifying effect with a live filter rather than a snapshot — so per CR 611.2c a creature that
 * enters after Festival resolves still can't attack, which is what "creatures can't attack this
 * turn" means.
 */
val Festival = card("Festival") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Cast this spell only during an opponent's upkeep.\nCreatures can't attack this turn."

    spell {
        castOnlyDuring(Step.UPKEEP)
        castOnlyIf(Conditions.IsNotYourTurn)
        effect = Effects.CantAttackGroup(GroupFilter.AllCreatures)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Mark Poole"
        flavorText = "Only after the townsfolk had drawn us into their merry celebration did we " +
            "discover that their holiday rituals held a deeper purpose."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9357990-701a-4336-b545-ac5a24d89cad.jpg?1783947948"
    }
}
