package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cut Short
 * {2}{W}
 * Instant
 * Convoke
 * Destroy target planeswalker that was activated this turn or tapped creature.
 *
 * "Was activated this turn" is `activatedThisTurn()`: any of the planeswalker's abilities —
 * loyalty or otherwise — was activated this turn, even if it has since left the stack.
 */
val CutShort = card("Cut Short") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Destroy target planeswalker that was activated this turn or tapped creature."

    keywords(Keyword.CONVOKE)

    spell {
        val permanent = target(
            TargetFilter(GameObjectFilter.Planeswalker.activatedThisTurn() or GameObjectFilter.Creature.tapped())
        )
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Tran Nguyen"
        flavorText = "With one swift, merciful stroke, Tamiyo's story came to an end."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0a45d4d-d16a-43c6-843c-916d4629aae1.jpg?1783917065"
        ruling(
            "2023-04-14",
            "A planeswalker was \"activated\" during a turn if one of its abilities was activated that turn. " +
                "This includes its loyalty abilities and any other activated abilities it may have. That ability " +
                "may still be on the stack, or it could have resolved, failed to resolve, been countered, or have " +
                "been removed from the stack some other way."
        )
        ruling(
            "2023-04-14",
            "Once an ability of a planeswalker is activated, it doesn't matter if the planeswalker loses that " +
                "ability. That planeswalker has still been \"activated\" that turn."
        )
    }
}
