package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Surtland Elementalist
 * {5}{U}{U}
 * Creature — Giant Wizard
 * 8/8
 * As an additional cost to cast this spell, reveal a Giant card from your hand or pay {2}.
 * Whenever this creature attacks, you may cast an instant or sorcery spell from your hand without
 * paying its mana cost.
 *
 * Per the ruling, a "Giant card" is a creature card with the creature type Giant, hence
 * `Creature.withSubtype(GIANT)` rather than any Giant-typed card. The reveal leaves the card in hand.
 *
 * The attack payoff is the gather → choose-up-to-one → cast-without-paying pipeline (as on Yue, the
 * Moon Spirit): the spell is cast during the trigger's resolution, ignoring type-based timing, and
 * declining is choosing none.
 */
val SurtlandElementalist = card("Surtland Elementalist") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Giant Wizard"
    power = 8
    toughness = 8
    oracleText = "As an additional cost to cast this spell, reveal a Giant card from your hand or pay {2}.\n" +
        "Whenever this creature attacks, you may cast an instant or sorcery spell from your hand " +
        "without paying its mana cost."

    additionalCost(
        Costs.additional.RevealFromHandOrPay(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.GIANT),
            alternativeManaCost = "{2}"
        )
    )

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val surtlandCandidates = gather(
                CardSource.FromZone(Zone.HAND, filter = GameObjectFilter.InstantOrSorcery)
            )
            val surtlandChosen = chooseUpTo(
                1,
                from = surtlandCandidates,
                selectedLabel = "Cast without paying its mana cost"
            )
            run(Effects.CastFromCollectionWithoutPayingCost(surtlandChosen))
        }
        description = "Whenever this creature attacks, you may cast an instant or sorcery spell from your " +
            "hand without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "375"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c9e7fdc-676f-4901-83ff-bfea3a96d937.jpg?1783928123"

        ruling("2021-02-05", "A Giant card is a creature card with the creature type Giant.")
        ruling(
            "2021-02-05",
            "If you cast a spell using Surtland Elementalist's triggered ability, you do so as part of the " +
                "resolution of that ability. You can't wait to cast the spell later in the turn. Timing " +
                "permissions based on the card's type are ignored, and the spell will resolve before " +
                "blockers are declared."
        )
        ruling(
            "2021-02-05",
            "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any " +
                "alternative costs. You can, however, pay additional costs, such as kicker costs. If the " +
                "card has any mandatory additional costs, those must be paid to cast the spell."
        )
        ruling("2021-02-05", "If the spell has {X} in its mana cost, you must choose 0 as the value of X.")
    }
}
