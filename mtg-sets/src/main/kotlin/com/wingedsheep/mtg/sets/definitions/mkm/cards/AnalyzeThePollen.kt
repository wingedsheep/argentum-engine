package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.collectEvidence
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Analyze the Pollen — Murders at Karlov Manor #150
 * {G} · Sorcery · Rare
 *
 * As an additional cost to cast this spell, you may collect evidence 8.
 * Search your library for a basic land card. If evidence was collected, instead search your library
 * for a creature or land card. Reveal that card, put it into your hand, then shuffle.
 *
 * A one-mana Lay of the Land early and a one-mana unconditional tutor late — the whole card is the
 * upgrade clause, and eight total mana value is a real price to reach. Note what the upgraded mode
 * actually finds: **any** creature or land card, which per the printed ruling includes nonbasic
 * lands, so it also fetches a Karlov Manor surveil land or a bomb creature outright.
 *
 * The **branch** shape of the collect-evidence linkage, and the third distinct one in the set
 * alongside [VituGhaziInspector]'s intervening-if and [CrimestopperSprite]'s rider. Here neither
 * branch is optional and the spell always does *something*, so the condition is neither on the
 * trigger nor a bolted-on extra step: it is a [ConditionalEffect] whose `elseEffect` carries the
 * un-upgraded search. "Instead" in the oracle text is literally an else.
 *
 * The optional cast cost rides the shared optional-additional-cost rail via `collectEvidence(8)`,
 * which stamps `ChoiceSlot.EVIDENCE_COLLECTED` on the spell — that is what
 * [Conditions.WasEvidenceCollected] reads at resolution. Per CR 701.59b a caster whose graveyard
 * can't reach total mana value 8 is never offered the choice at all, so the else-branch is what an
 * early-game cast always takes.
 *
 * Both branches are the same [Patterns.Library.searchLibrary] recipe with `reveal = true` and
 * `shuffleAfter = true`, differing only in the filter — "Reveal that card, put it into your hand,
 * then shuffle" applies to whichever search happened. Failing to find is legal in both modes: the
 * search is a `ChooseUpTo(1)`, so an empty library or a player who declines to find simply shuffles
 * and moves on.
 */
val AnalyzeThePollen = card("Analyze the Pollen") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, you may collect evidence 8. (Exile " +
        "cards with total mana value 8 or greater from your graveyard.)\n" +
        "Search your library for a basic land card. If evidence was collected, instead search " +
        "your library for a creature or land card. Reveal that card, put it into your hand, then " +
        "shuffle."

    collectEvidence(8)

    spell {
        effect = ConditionalEffect(
            condition = Conditions.WasEvidenceCollected,
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature or GameObjectFilter.Land,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            ),
            elseEffect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Anna Christenson"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5563967f-09fd-4ccf-8892-4dd0c2544c98.jpg?1783912872"

        ruling(
            "2024-02-02",
            "The collect evidence ability of Analyze the Pollen allows you to find a nonbasic land card."
        )
        ruling(
            "2024-02-02",
            "If you can't exile enough cards to meet or exceed the required mana value, you can't " +
                "choose to collect evidence at all."
        )
        ruling(
            "2024-02-02",
            "Once you've announced that you're casting a spell, players can't take actions until " +
                "you've finished doing so. Notably, opponents can't try to remove cards from your " +
                "graveyard to stop you from collecting evidence."
        )
    }
}
