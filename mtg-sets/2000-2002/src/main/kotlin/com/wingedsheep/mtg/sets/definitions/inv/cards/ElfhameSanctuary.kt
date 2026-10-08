package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.core.Step

/**
 * Elfhame Sanctuary
 * {1}{G}
 * Enchantment
 * At the beginning of your upkeep, you may search your library for a basic land card, reveal that
 * card, put it into your hand, then shuffle. If you do, you skip your draw step this turn.
 *
 * The whole ability is optional ("you may"), so the controller chooses yes/no when it resolves.
 * On accepting, the search runs (basic land, reveal, into hand, shuffle) and the controller's draw
 * step for this turn is skipped via [Effects.SkipNextStepOrPhase] (`DRAW_STEP`) — the marker is consumed by the
 * upcoming draw step.
 */
val ElfhameSanctuary = card("Elfhame Sanctuary") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, you may search your library for a basic land card, " +
        "reveal that card, put it into your hand, then shuffle. If you do, you skip your draw step this turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        // The search is itself optional ("you may search ... for a basic land card") — the
        // controller declines by choosing zero cards. Effects.IfYouDo gates the draw-step skip on
        // the search actually putting a card into hand (SuccessCriterion.Auto detects the terminal
        // move into HAND), matching the "If you do, you skip your draw step this turn" clause.
        effect = Effects.IfYouDo(
            action = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            ),
            then = Effects.SkipNextStepOrPhase(TurnPart.DRAW_STEP)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "185"
        artist = "Alan Rabinowitz"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6ab9a90c-5fd8-4f8c-b692-f98a2974810c.jpg?1562916434"
    }
}
