package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Conduit of Ruin
 * {6}
 * Creature — Eldrazi
 * 5/5
 * When you cast this spell, you may search your library for a colorless creature card with mana
 * value 7 or greater, reveal it, then shuffle and put that card on top.
 * The first creature spell you cast each turn costs {2} less to cast.
 *
 *  - The tutor is a *cast* trigger, so it resolves while Conduit is still on the stack — the card
 *    it finds is on top of the library before Conduit itself resolves.
 *  - The discount is gated by [CostGating.NthOfTypePerTurn] off the caster's spells-cast-this-turn
 *    record, which covers the whole turn even while Conduit wasn't on the battlefield: casting
 *    Conduit itself uses up the turn's first creature spell. Two Conduits each reduce that same
 *    first spell, for {4} total. Only generic mana is reduced.
 */
val ConduitOfRuin = card("Conduit of Ruin") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 5
    toughness = 5
    oracleText = "When you cast this spell, you may search your library for a colorless creature " +
        "card with mana value 7 or greater, reveal it, then shuffle and put that card on top.\n" +
        "The first creature spell you cast each turn costs {2} less to cast."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature
                    .withCardPredicate(CardPredicate.IsColorless)
                    .manaValueAtLeast(7),
                destination = SearchDestination.TOP_OF_LIBRARY,
                reveal = true,
            )
        )
        description = "When you cast this spell, you may search your library for a colorless " +
            "creature card with mana value 7 or greater, reveal it, then shuffle and put that " +
            "card on top."
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature),
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.NthOfTypePerTurn(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bc855d3-817c-4748-a7f5-1533d8b0e930.jpg?1783938225"
        ruling(
            "2015-08-25",
            "Conduit of Ruin's last ability will look at the entire turn, even if Conduit of Ruin " +
                "wasn't on the battlefield for some of it. Notably, if you cast Conduit of Ruin in a " +
                "turn, then no other creature spell you cast that turn can be your first."
        )
        ruling(
            "2015-08-25",
            "If you control more than one Conduit of Ruin, the last ability of each of them applies " +
                "only to the first creature spell you cast each turn, not to different spells; the " +
                "first creature spell you cast each turn will cost {4} less."
        )
    }
}
