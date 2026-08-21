package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAlternativeCastingCost

/**
 * Conspiracy Unraveler {5}{U}{U}
 * Creature — Sphinx Detective
 * 6/6
 *
 * Flying
 * You may collect evidence 10 rather than pay the mana cost for spells you cast.
 *
 * The same [GrantAlternativeCastingCost] Jodah, Archmage Eternal uses, with the substituted cost
 * in the grant's **non-mana** half: "rather than pay the mana cost" says nothing about the
 * replacement being mana, so the mana half is the `{0}` idiom (as on Fireblast and Force of Vigor,
 * whose own alternative costs are likewise entirely non-mana) and `collect evidence 10` is the
 * whole price. It is paid by the ordinary additional-cost rail, through the same
 * `CollectEvidenceResolver` every other collect-evidence context uses — which is what gives it the
 * CR 701.59b fail-closed gate (a graveyard that can't reach total mana value 10 never offers the
 * option) and the sum-gated client picker, for free.
 *
 * Two consequences of it being an *alternative* cost rather than an additional one, both covered by
 * the rulings below and both already true of every alternative cost the engine models:
 *  - it can't be combined with another alternative cost (CR 601.2f), but additional costs still
 *    apply — including a *second* collect evidence, which triggers "whenever you collect evidence"
 *    twice;
 *  - it does not satisfy a spell's own linked "if evidence was collected" clause
 *    (`Conditions.WasEvidenceCollected` reads the `ChoiceSlot.EVIDENCE_COLLECTED` declaration on
 *    the spell, which this cost never stamps).
 */
val ConspiracyUnraveler = card("Conspiracy Unraveler") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx Detective"
    power = 6
    toughness = 6
    oracleText = "Flying\nYou may collect evidence 10 rather than pay the mana cost for spells " +
        "you cast. (To collect evidence 10, exile cards with total mana value 10 or greater from " +
        "your graveyard.)"

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantAlternativeCastingCost(
            cost = "{0}",
            additionalCosts = listOf(Costs.additional.CollectEvidence(10))
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "47"
        artist = "Wayne Reynolds"
        flavorText = "\"There can be many motives and opportunities, but there is always only " +
            "one truth.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88e791fc-bf9f-49b6-b5f2-a24d4b3e360e.jpg?1783912913"
        ruling(
            "2024-02-02",
            "If a spell has {X} in its mana cost, you must choose 0 as the value of X when " +
                "casting it without paying its mana cost."
        )
        ruling(
            "2024-02-02",
            "If you cast a spell for another cost \"rather than pay its mana cost\", you can't " +
                "choose to cast it for any alternative costs. You can, however, pay additional " +
                "costs. If the spell has any mandatory additional costs, such as that of Demand " +
                "Answers, those must be paid to cast the card."
        )
        ruling(
            "2024-02-02",
            "If you are casting a spell from your graveyard (for example, a spell with flashback) " +
                "you can't also exile that card to pay the alternative collect evidence cost " +
                "offered by Conspiracy Unraveler."
        )
        ruling(
            "2024-02-02",
            "If you cast a spell with the alternative collect evidence cost offered by Conspiracy " +
                "Unraveler and you also collect evidence as an additional cost to cast that " +
                "spell, abilities of permanents that trigger \"whenever you collect evidence\" " +
                "will trigger twice. This is also true for abilities that trigger \"whenever one " +
                "or more cards leave your graveyard.\""
        )
        ruling(
            "2024-02-02",
            "If a spell has an additional cost that includes collecting evidence, such as that of " +
                "Analyze the Pollen, any additional effects that occur \"if evidence was " +
                "collected\" will occur only if that additional cost was paid. Using the " +
                "alternative cost from Conspiracy Unraveler will not cause those additional " +
                "effects to occur."
        )
        ruling(
            "2024-02-02",
            "If you can't exile enough cards to meet or exceed the required mana value, you can't " +
                "choose to collect evidence at all."
        )
    }
}
