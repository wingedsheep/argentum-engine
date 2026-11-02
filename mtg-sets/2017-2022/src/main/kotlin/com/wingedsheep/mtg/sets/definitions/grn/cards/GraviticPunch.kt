package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastThisWayExile
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gravitic Punch
 * {3}{R}
 * Sorcery
 *
 * Target creature you control deals damage equal to its power to target player.
 * Jump-start
 *
 * The creature is the damage source (Rabid Bite's shape), so its lifelink/deathtouch/etc. apply.
 * Jump-start works like Beacon Bolt's: cast from the graveyard by discarding a card, and the card
 * is exiled however it leaves the stack.
 */
val GraviticPunch = card("Gravitic Punch") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature you control deals damage equal to its power to target player.\n" +
        "Jump-start (You may cast this card from your graveyard by discarding a card in addition to " +
        "paying its other costs. Then exile this card.)"

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        val player = target(Targets.Player)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.powerOf(creature),
            target = player,
            damageSource = creature
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
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05fe0428-6e7d-49d4-9a29-80a3589f565a.jpg?1783934162"
        ruling(
            "2018-10-05",
            "If the target creature or the target player is an illegal target as Gravitic Punch tries to " +
                "resolve, the creature won't deal damage."
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
