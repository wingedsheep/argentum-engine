package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard

/**
 * Overflow Submatrix
 * {2}{B}
 * Artifact Creature — Borg
 * 3/3
 *
 * Once during each of your turns, you may cast an artifact creature spell from your graveyard.
 * If you cast a spell this way, that artifact creature enters with a finality counter on it.
 *
 * One [MayCastFromGraveyard] grant: `oncePerTurn` tracks the allowance on this permanent, and the
 * `entersWithCounter` rider applies only to a spell cast under this grant ("If you cast a spell
 * this way"), not to artifact creatures cast from the graveyard by some other permission.
 */
val OverflowSubmatrix = card("Overflow Submatrix") {
    manaCost = "{2}{B}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "B"
    oracleText = "Once during each of your turns, you may cast an artifact creature spell from your graveyard. " +
        "If you cast a spell this way, that artifact creature enters with a finality counter on it. " +
        "(If a creature with a finality counter on it would die, exile it instead.)"
    power = 3
    toughness = 3

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.ArtifactCreature,
            duringYourTurnOnly = true,
            oncePerTurn = true,
            entersWithCounter = CounterType.FINALITY,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Carlos Dattoli"
        flavorText = "\"When they decide to come, they're going to come in force. They don't do anything piecemeal.\"\n—Guinan"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e5654a7-ce83-4801-8978-7294a39dbef8.jpg?1793100705"
    }
}
