package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Inscribed Tablet
 * {1}
 * Artifact
 *
 * {1}, {T}, Sacrifice this artifact: Reveal the top five cards of your library. Put a land card
 * from among them into your hand and the rest on the bottom of your library in a random order.
 * If you didn't put a card into your hand this way, draw a card.
 *
 * The land pick is mandatory (no "may"): `chooseExactlySplit(1)` auto-selects nothing when none of
 * the five is a land, and the draw keys off the kept pile being empty.
 */
val InscribedTablet = card("Inscribed Tablet") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Reveal the top five cards of your library. Put a land card " +
        "from among them into your hand and the rest on the bottom of your library in a random order. " +
        "If you didn't put a card into your hand this way, draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(5))
            reveal(revealed)
            val (kept, rest) = chooseExactlySplit(
                1,
                from = revealed,
                filter = GameObjectFilter.Land,
                prompt = "Put a land card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Random)
            ifNotEmpty(kept) {
                run(Effects.Nothing)
            } orElse {
                run(Effects.DrawCards(1))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Jarel Threat"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/699d8655-d250-4ab6-92c5-376979bbabc7.jpg?1783921269"
    }
}
