package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Fugitive Codebreaker — Murders at Karlov Manor #127
 * {1}{R} · Creature — Goblin Rogue · 2/1
 *
 * Prowess, haste
 * Disguise {5}{R}. This cost is reduced by {1} for each instant and sorcery card in your graveyard.
 * When this creature is turned face up, discard your hand, then draw three cards.
 *
 * Bedlam Reveler's reduction moved onto a disguise cost. The reduction source is literally the same
 * one — [CostReductionSource.CardsInGraveyardMatchingFilter] over instants and sorceries — but it
 * can't ride a `ModifySpellCost` static: that family scans the battlefield and prices *spells*,
 * while this prices a special action taken by a permanent that, being face down, has no abilities
 * at all to scan (CR 702.168a / 708.2). So it rides the disguise ability itself
 * ([com.wingedsheep.sdk.scripting.KeywordAbility.Disguise.costReduction]) and travels with the card
 * into the face-down permanent's turn-up procedure.
 *
 * Generic-only, like every other cost reduction: the {R} survives an arbitrarily stocked graveyard,
 * so the floor is {R}, not free. And it is re-read at every price check rather than locked in when
 * the permanent came down — casting an instant in response to your own turn-up plan makes the flip
 * cheaper, which is the whole point of a card that wants you to have burned through your hand.
 *
 * The refill is a turned-face-up trigger, not an enters trigger, and the difference is the card:
 * turning a permanent face up doesn't cause enters abilities to trigger (2024-02-02 ruling), so a
 * hard-cast {1}{R} 2/1 gets no cards. Discard resolves fully before the draw, so the three new
 * cards are never discarded, and an empty hand still draws three.
 */
val FugitiveCodebreaker = card("Fugitive Codebreaker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Rogue"
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until " +
        "end of turn.)\n" +
        "Haste\n" +
        "Disguise {5}{R}. This cost is reduced by {1} for each instant and sorcery card in your " +
        "graveyard. (You may cast this card face down for {3} as a 2/2 creature with ward {2}. " +
        "Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, discard your hand, then draw three cards."
    power = 2
    toughness = 1
    keywords(Keyword.PROWESS, Keyword.HASTE)

    disguise = "{5}{R}"
    disguiseCostReduction = CostReductionSource.CardsInGraveyardMatchingFilter(
        filter = GameObjectFilter.InstantOrSorcery,
        amountPerCard = 1
    )

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.Composite(
            Patterns.Hand.discardHand(),
            Effects.DrawCards(3)
        )
        description = "When this creature is turned face up, discard your hand, then draw three cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Joseph Weston"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b682bf8a-06dc-4828-bc46-9e1427bf981f.jpg?1783912881"

        ruling(
            "2024-02-02",
            "If you have no cards in hand when Fugitive Codebreaker's last ability resolves, you " +
                "won't discard any cards, but you'll still draw three cards."
        )
        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
        ruling(
            "2024-02-02",
            "If a face-down creature loses its abilities, it can't be turned face up with a " +
                "disguise ability because it will no longer have a disguise ability (or a " +
                "disguise cost) once face up."
        )
    }
}
