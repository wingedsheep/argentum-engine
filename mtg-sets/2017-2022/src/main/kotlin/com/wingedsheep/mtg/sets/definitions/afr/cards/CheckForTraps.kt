package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Check for Traps
 * {1}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it. Exile that card. If an
 * instant card or a card with flash is exiled this way, they lose 1 life. Otherwise, you lose 1 life.
 *
 * The life-loss branch reads the cards that actually moved ("exiled this way"), so a hand with no
 * nonland card — nothing exiled — falls through to "Otherwise, you lose 1 life".
 */
val CheckForTraps = card("Check for Traps") {
    manaCost = "{1}{B}"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. Exile that card. " +
        "If an instant card or a card with flash is exiled this way, they lose 1 life. Otherwise, you lose 1 life."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val chosen = chooseExactly(
                1,
                from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to exile",
                alwaysPrompt = true,
                showAllCards = true
            )
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE, opponent.asPlayer))
            ifNotEmpty(exiled, filter = GameObjectFilter.Instant or GameObjectFilter.Any.withKeyword(Keyword.FLASH)) {
                run(Effects.LoseLife(1, opponent))
            } orElse {
                run(Effects.LoseLife(1, EffectTarget.Controller))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Zoltan Boros"
        flavorText = "\"Relax. I'm a professional.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f58ea8a-30ac-45b0-a701-8b8d8262cfcf.jpg?1783926499"
    }
}
