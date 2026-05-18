package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Teferi's Response
 * {1}{U}
 * Instant
 * Counter target spell or ability an opponent controls that targets a land you control.
 * If a permanent's ability is countered this way, destroy that permanent.
 * Draw two cards.
 */
val TeferisResponse = card("Teferi's Response") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell or ability an opponent controls that targets a land you control. " +
        "If a permanent's ability is countered this way, destroy that permanent.\n" +
        "Draw two cards."

    spell {
        // Targets either a spell or an activated/triggered ability on the stack. The
        // outer filter pins controller=opponent and requires at least one of the chosen
        // targets to be a land you control (via CardPredicate.TargetsMatching).
        target = TargetObject(
            filter = TargetFilter(
                baseFilter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.TargetsMatching(
                            GameObjectFilter.Land.youControl()
                        )
                    )
                ),
                zone = Zone.STACK
            ).opponentControls()
        )
        effect = Effects.Composite(
            // Destroy the source permanent BEFORE countering — at this point the stack
            // entity still carries its ability component so we can read sourceId.
            Effects.DestroySourceOfTargetedAbility(),
            Effects.CounterSpellOrAbility(),
            Effects.DrawCards(2)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Scott Bailey"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3bb2df8-c559-4a34-83b0-d48fbc694cc8.jpg?1562944007"
    }
}
