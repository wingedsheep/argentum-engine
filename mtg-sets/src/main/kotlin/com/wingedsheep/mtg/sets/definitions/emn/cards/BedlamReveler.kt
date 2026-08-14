package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Bedlam Reveler
 * {6}{R}{R}
 * Creature — Devil Horror
 * 3/4
 *
 * This spell costs {1} less to cast for each instant and sorcery card in your graveyard.
 * Prowess
 * When this creature enters, discard your hand, then draw three cards.
 *
 * The cost reduction is a self-cast [ModifySpellCost] over
 * [CostReductionSource.CardsInGraveyardMatchingFilter] — it only ever eats generic mana, so the
 * {R}{R} survives (2025-01-24 ruling), and the mana value stays 8 no matter what was paid.
 * The enters trigger is an ordered composite: discard the whole hand first, *then* draw three, so
 * the drawn cards are never discarded. An empty hand discards nothing and still draws three.
 */
val BedlamReveler = card("Bedlam Reveler") {
    manaCost = "{6}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil Horror"
    power = 3
    toughness = 4
    oracleText = "This spell costs {1} less to cast for each instant and sorcery card in your graveyard.\n" +
        "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "When this creature enters, discard your hand, then draw three cards."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardsInGraveyardMatchingFilter(
                    filter = GameObjectFilter.InstantOrSorcery,
                    amountPerCard = 1
                )
            )
        )
    }

    keywords(Keyword.PROWESS)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Patterns.Hand.discardHand(),
            Effects.DrawCards(3)
        )
        description = "When this creature enters, discard your hand, then draw three cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Jama Jurabaev"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/0232f188-44f2-4aee-963c-6f6edc4a21ac.jpg?1783937466"

        ruling(
            "2025-01-24",
            "To determine the total cost of a spell, start with the mana cost or alternative cost " +
                "you're paying, add any cost increases, then apply any cost reductions (such as that " +
                "of Bedlam Reveler's first ability). The mana value of the spell is determined by " +
                "only its mana cost, no matter what the total cost to cast that spell was."
        )
        ruling("2025-01-24", "Bedlam Reveler's first ability can't reduce the {R}{R} in its cost.")
        ruling(
            "2025-01-24",
            "If you have no cards in hand when Bedlam Reveler's enters ability resolves, you just " +
                "draw three cards."
        )
    }
}
