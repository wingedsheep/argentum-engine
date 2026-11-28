package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Frightful Delusion
 * {2}{U}
 * Instant
 *
 * Counter target spell unless its controller pays {1}. That player discards a card.
 */
val FrightfulDelusion = card("Frightful Delusion") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {1}. That player discards a card."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.Pipeline {
            val spell = gather(CardSource.ChosenTargets)
            // Snapshot "that player" while the spell is still on the stack: once countered it is a
            // graveyard card with no controller.
            val controllers = captureControllers(spell)
            run(Effects.CounterUnlessPays("{1}"))
            // The discard happens whether or not {1} was paid.
            forEachCaptured(spell, spell, controllers) {
                run(Effects.Discard(1))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Anthony Palumbo"
        flavorText = "Whether he actually exists is in question, but the terror she feels is excruciatingly real."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38c9ba98-90b4-4c28-9eef-a4fe0913b921.jpg?1783940974"
        ruling("2011-09-22", "You must target a spell in order to cast Frightful Delusion. You can't cast it without a legal target just to make a player discard a card.")
        ruling("2011-09-22", "The player discards a card even if they pay {1}.")
    }
}
