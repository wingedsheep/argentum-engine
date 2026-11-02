package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastThisWayExile
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Risk Factor
 * {2}{R}
 * Instant
 *
 * Target opponent may have Risk Factor deal 4 damage to them. If that player doesn't, you draw
 * three cards.
 * Jump-start (You may cast this card from your graveyard by discarding a card in addition to
 * paying its other costs. Then exile this card.)
 *
 * The punisher choice is a `May` answered by the targeted opponent (`decisionMaker`): accepting
 * has the spell deal 4 damage to them, declining runs `otherwise` — the draw, which still goes to
 * the spell's controller because the gate never rebinds "you". An illegal target at resolution
 * fizzles the spell, so nothing is drawn (per the ruling).
 *
 * Jump-start is the self graveyard-cast permission with a discard-a-card additional cost whose
 * spell is exiled on any stack exit (`CastThisWayExile.ON_ANY_STACK_EXIT`), like Chemister's Insight.
 */
val RiskFactor = card("Risk Factor") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target opponent may have Risk Factor deal 4 damage to them. If that player doesn't, " +
        "you draw three cards.\n" +
        "Jump-start (You may cast this card from your graveyard by discarding a card in addition to " +
        "paying its other costs. Then exile this card.)"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.May(
            effect = Effects.DealDamage(4, opponent, damageSource = EffectTarget.Self),
            otherwise = Effects.DrawCards(3),
            decisionMaker = opponent,
            // Shown to the opponent being asked, so it reads from their perspective.
            descriptionOverride = "Have Risk Factor deal 4 damage to you? If you don't, its " +
                "controller draws three cards.",
        )
    }

    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            additionalCost = Costs.additional.DiscardCards(1),
            exileCastThisWay = CastThisWayExile.ON_ANY_STACK_EXIT
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4eda89d9-9bd1-4a55-ac02-f9a0625d8e5b.jpg?1783934160"
        ruling(
            "2018-10-05",
            "If the target player is an illegal target by the time Risk Factor tries to resolve, the spell " +
                "doesn't resolve. You don't draw three cards."
        )
        ruling(
            "2018-10-05",
            "You must still follow any timing restrictions and permissions when casting a spell with jump-start, " +
                "including those based on the card's type. For instance, you can cast a sorcery using jump-start " +
                "only when you could normally cast a sorcery."
        )
        ruling(
            "2018-10-05",
            "A spell cast using jump-start will always be exiled afterward, whether it resolves, it's countered, " +
                "or it leaves the stack in some other way."
        )
        ruling(
            "2018-10-05",
            "If an effect allows you to pay an alternative cost rather than a spell's mana cost, you may pay that " +
                "alternative cost when you jump-start a spell. You'll still discard a card as an additional cost " +
                "to cast it."
        )
        ruling(
            "2018-10-05",
            "If a card with jump-start is put into your graveyard during your turn, you'll be able to cast it " +
                "right away if it's legal to do so, before an opponent can take any actions."
        )
    }
}
