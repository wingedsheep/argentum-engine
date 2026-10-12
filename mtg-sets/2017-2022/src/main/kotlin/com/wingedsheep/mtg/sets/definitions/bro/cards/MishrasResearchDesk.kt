package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Mishra's Research Desk — The Brothers' War #162
 * {1} · Artifact · Uncommon
 *
 * {1}, {T}, Sacrifice this artifact: Exile the top two cards of your library. Choose one of them.
 * Until the end of your next turn, you may play that card.
 * Unearth {1}{R}
 *
 * The selective impulse is Feldon, Ronom Excavator's pipeline over a fixed two cards: both are
 * exiled, only the chosen one gains may-play with the turn-keyed `UntilEndOfNextTurn` expiry.
 */
val MishrasResearchDesk = card("Mishra's Research Desk") {
    manaCost = "{1}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Exile the top two cards of your library. " +
        "Choose one of them. Until the end of your next turn, you may play that card.\n" +
        "Unearth {1}{R} ({1}{R}: Return this card from your graveyard to the battlefield. " +
        "Exile it at the beginning of the next end step or if it would leave the battlefield. " +
        "Unearth only as a sorcery.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(2))
            exile(exiled)
            val chosen = chooseExactly(1, from = exiled, prompt = "Choose a card you may play until the end of your next turn")
            run(Effects.GrantMayPlayFromExile(from = chosen, expiry = MayPlayExpiry.UntilEndOfNextTurn))
        }
        description = "{1}, {T}, Sacrifice this artifact: Exile the top two cards of your library. " +
            "Choose one of them. Until the end of your next turn, you may play that card."
    }

    unearth("{1}{R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb142d99-b210-47c8-897c-be62f90d2192.jpg?1783920054"
        ruling("2022-10-14", "You must pay all costs and follow all normal timing rules for spells cast using the first ability of Mishra's Research Desk.")
    }
}
