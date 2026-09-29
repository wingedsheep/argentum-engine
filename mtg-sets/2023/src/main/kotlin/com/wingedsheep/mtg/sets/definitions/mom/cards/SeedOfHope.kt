package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Seed of Hope
 * {G}
 * Instant
 *
 * Mill two cards. You may put a permanent card from among the milled cards into your hand.
 * You gain 2 life.
 *
 * Inline Gather → Move (mill) → optional select pipeline, the same shape as Cache Grab: the
 * gathered collection tracks the two milled cards into the graveyard, `chooseUpTo(1)` filtered
 * to permanent cards is the "you may", and the life gain happens regardless of the choice.
 */
val SeedOfHope = card("Seed of Hope") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Mill two cards. You may put a permanent card from among the milled cards into your hand. " +
        "You gain 2 life. (To mill two cards, put the top two cards of your library into your graveyard.)"

    spell {
        effect = Effects.Pipeline {
            val milled = gather(CardSource.TopOfLibrary(2))
            toGraveyard(milled)
            val selected = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Permanent,
                showAllCards = true,
                prompt = "You may put a permanent card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(selected)
            run(Effects.GainLife(2))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "204"
        artist = "Gaboleps"
        flavorText = "Wrenn had given everything and asked for nothing. She deserved a monument, but rest in a quiet meadow is all she would've wanted."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/084a8b94-0c5d-41e0-88e4-fe91ab92c09d.jpg?1783916962"
    }
}
