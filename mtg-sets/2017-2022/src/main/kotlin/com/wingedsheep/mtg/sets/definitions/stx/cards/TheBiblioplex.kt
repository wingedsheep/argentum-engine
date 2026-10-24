package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * The Biblioplex — Strixhaven: School of Mages #264 (canonical printing)
 * Land
 *
 * {T}: Add {C}.
 * {2}, {T}: Look at the top card of your library. If it's an instant or sorcery card, you may
 * reveal it and put it into your hand. If you don't put the card into your hand, you may put it
 * into your graveyard. Activate only if you have exactly zero or seven cards in hand.
 *
 * The look is Archghoul of Thraben's two-stage pipeline: an optional instant-or-sorcery pick into
 * hand (revealed), then an optional trip to the graveyard for whatever wasn't taken, anything
 * left going back on top untouched (ruling). "Exactly zero or seven" is checked only on
 * activation (ruling) — [Conditions.EmptyHand] or a seven-or-more-and-seven-or-fewer pair.
 */
val TheBiblioplex = card("The Biblioplex") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{2}, {T}: Look at the top card of your library. If it's an instant or sorcery card, you " +
        "may reveal it and put it into your hand. If you don't put the card into your hand, you may " +
        "put it into your graveyard. Activate only if you have exactly zero or seven cards in hand."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val (toHandCards, notTaken) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.InstantOrSorcery,
                prompt = "Reveal the instant or sorcery card and put it into your hand?",
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Leave it"
            )
            toHand(toHandCards, revealed = true)
            val (toGraveyardCards, staysOnTop) = chooseUpToSplit(
                1,
                from = notTaken,
                prompt = "Put the card into your graveyard?",
                selectedLabel = "Put into your graveyard",
                remainderLabel = "Leave on top of your library"
            )
            toGraveyard(toGraveyardCards)
            toLibraryTop(staysOnTop, order = CardOrder.Preserve)
        }
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Any(
                    Conditions.EmptyHand,
                    Conditions.All(Conditions.CardsInHandAtLeast(7), Conditions.CardsInHandAtMost(7)),
                )
            )
        )
        description = "{2}, {T}: Look at the top card of your library. If it's an instant or sorcery " +
            "card, you may reveal it and put it into your hand. If you don't put the card into your " +
            "hand, you may put it into your graveyard. Activate only if you have exactly zero or " +
            "seven cards in hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "264"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8eae4481-977f-4bb7-bb1a-ab7100e6ba47.jpg?1783927275"
        ruling("2021-04-16", "If you don't choose to either reveal the card or put it into your graveyard, the card stays on top of your library.")
        ruling("2021-04-16", "Although you must have exactly zero or seven cards in your hand to activate the last ability, it doesn't matter how many cards you have in your hand as that ability resolves.")
    }
}
