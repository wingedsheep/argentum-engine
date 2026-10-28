package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Coveted Prize — Zendikar Rising #95
 * {4}{B} · Sorcery
 *
 * This spell costs {1} less to cast for each creature in your party.
 * Search your library for a card, put it into your hand, then shuffle. If you have a full party, you
 * may cast a spell with mana value 4 or less from your hand without paying its mana cost.
 *
 * - The cost reduction is Sea Gate Colossus's party-size self-cast [ModifySpellCost].
 * - The search is Demonic Tutor's; the full-party check is made at resolution, after the search, so
 *   the card just found may be the one cast (ruling).
 * - The free cast is Baral and Kari Zev's hand pipeline: gather the hand, keep nonland cards with mana
 *   value 4 or less, choose up to one ("you may"), and cast it during resolution without paying its
 *   mana cost — card-type timing is ignored.
 */
val CovetedPrize = card("Coveted Prize") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "This spell costs {1} less to cast for each creature in your party. (Your party consists of " +
        "up to one each of Cleric, Rogue, Warrior, and Wizard.)\nSearch your library for a card, put it into " +
        "your hand, then shuffle. If you have a full party, you may cast a spell with mana value 4 or less " +
        "from your hand without paying its mana cost."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.partySize()),
            ),
        )
    }

    spell {
        effect = Patterns.Library.searchLibrary(filter = GameObjectFilter.Any, destination = SearchDestination.HAND) then
            Effects.If(
                Conditions.FullParty(),
                Effects.Pipeline {
                    val inHand = gather(CardSource.FromZone(Zone.HAND))
                    val eligible = filter(inHand, GameObjectFilter.Nonland.manaValueAtMost(4))
                    val chosen = chooseUpTo(1, from = eligible, selectedLabel = "Cast without paying its mana cost")
                    run(Effects.CastFromCollectionWithoutPayingCost(chosen))
                },
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "95"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5afbc5d3-318f-4545-88d6-fc81e5466238.jpg?1783929381"
        ruling(
            "2020-09-25",
            "If you have a full party and wish to cast a spell with Coveted Prize's effect, you do so as part of " +
                "the resolution of Coveted Prize after searching for a card. You can't wait to cast it later in " +
                "the turn. Timing permissions based on a card's type are ignored."
        )
        ruling("2020-09-25", "The card you search for may be the one you cast.")
        ruling(
            "2020-09-25",
            "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative " +
                "costs. You can, however, pay additional costs. If the card has any mandatory additional costs, " +
                "you must pay those to cast the card."
        )
        ruling(
            "2020-09-25",
            "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without " +
                "paying its mana cost."
        )
        ruling(
            "2020-09-25",
            "If a spell has a cost reduction based on the number of creatures in your party, no player may " +
                "attempt to change that number after you begin to cast the spell but before you pay the cost."
        )
    }
}
