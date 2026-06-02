package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Essence Anchor
 * {2}{U}
 * Artifact
 *
 * At the beginning of your upkeep, surveil 1.
 * {T}: Create a 2/2 black Zombie Druid creature token. Activate only during your turn
 * and only if a card left your graveyard this turn.
 *
 * The upkeep trigger is [Triggers.YourUpkeep] + [LibraryPatterns.surveil] (look at the top
 * card, optionally bin it — which itself can feed the activation condition by sending a card
 * out of the graveyard later). The token ability is a tap activation gated by two
 * [ActivationRestriction]s: [OnlyDuringYourTurn] and [OnlyIfCondition] with
 * [Conditions.CardsLeftGraveyardThisTurn] (≥1), matching the printed "only during your turn
 * and only if a card left your graveyard this turn" restriction.
 */
val EssenceAnchor = card("Essence Anchor") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, surveil 1. " +
        "(Look at the top card of your library. You may put it into your graveyard.)\n" +
        "{T}: Create a 2/2 black Zombie Druid creature token. Activate only during your turn " +
        "and only if a card left your graveyard this turn."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = LibraryPatterns.surveil(1)
        description = "At the beginning of your upkeep, surveil 1."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = CreateTokenEffect(
            count = 1,
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie", "Druid"),
            name = "Zombie Druid",
            imageUri = "https://cards.scryfall.io/normal/front/f/1/f10d5813-7818-43e8-b08d-4ed8c54d0366.jpg?1748452772",
        )
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.OnlyIfCondition(Conditions.CardsLeftGraveyardThisTurn(1)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "David Astruga"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e91c4509-918e-44ba-aa13-1991199fee9f.jpg?1743204135"
    }
}
