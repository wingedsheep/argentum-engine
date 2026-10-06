package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Roots of Wisdom
 * {1}{G}
 * Sorcery — Common (KHM #190)
 *
 * "Mill three cards, then return a land card or Elf card from your graveyard to your hand.
 * If you can't, draw a card."
 *
 * Implementation:
 *  - Mill first, so the milled cards are legal picks for the return (no targeting — ruling).
 *  - The return is mandatory ("you must return one" — ruling), so it's a `chooseExactly(1)` over
 *    the graveyard's land-or-Elf cards; an empty pool returns nothing.
 *  - "If you can't, draw a card" is [Effects.IfYouDo] scored on the named selection being
 *    non-empty, with the draw on the `otherwise` branch.
 *  - "Elf card" is any card with the Elf subtype (kindred Elf spells included), not only creatures.
 */
val RootsOfWisdom = card("Roots of Wisdom") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Mill three cards, then return a land card or Elf card from your graveyard to your hand. " +
        "If you can't, draw a card. (To mill a card, put the top card of your library into your graveyard.)"

    spell {
        effect = Patterns.Library.mill(3) then Effects.IfYouDo(
            action = Effects.Pipeline {
                val pool = gather(
                    CardSource.FromZone(
                        Zone.GRAVEYARD,
                        Player.You,
                        GameObjectFilter.Land or GameObjectFilter.Any.withSubtype("Elf")
                    )
                )
                // Named: the "if you can't" check below reads it from outside the pipeline.
                val returned = chooseExactly(
                    1,
                    from = pool,
                    prompt = "Return a land card or Elf card from your graveyard to your hand",
                    selectedLabel = "Return to hand",
                    name = "returned"
                )
                toHand(returned)
            },
            then = Effects.Nothing,
            otherwise = Effects.DrawCards(1),
            successCriterion = SuccessCriterion.CollectionNonEmpty("returned")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Sidharth Chaturvedi"
        flavorText = "Though imprisoned in Jaspera trees, the elvish gods known as the Einir still whisper through the ages."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/734afb5b-3163-47fa-856f-8a85b9da22d3.jpg?1783928206"
        ruling("2021-02-05", "Roots of Wisdom doesn't target any cards in graveyards. You may return a land card or an Elf card that was just milled or one that was already there.")
        ruling("2021-02-05", "If there's a land card or Elf card in your graveyard, you must return one. You can't choose not to in order to draw a card.")
    }
}
