package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastThisWayExile
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sonic Assault
 * {1}{U}{R}
 * Instant
 *
 * Tap target creature. Sonic Assault deals 2 damage to that creature's controller.
 * Jump-start (You may cast this card from your graveyard by discarding a card in addition to
 * paying its other costs. Then exile this card.)
 */
val SonicAssault = card("Sonic Assault") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Tap target creature. Sonic Assault deals 2 damage to that creature's controller.\n" +
        "Jump-start (You may cast this card from your graveyard by discarding a card in addition to " +
        "paying its other costs. Then exile this card.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then Effects.DealDamage(2, EffectTarget.TargetController)
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
        collectorNumber = "199"
        artist = "Tyler Walpole"
        flavorText = "He heard stars and saw thunder."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc61a398-cf16-415b-b3cf-897217dc7cc9.jpg?1783934124"
        ruling(
            "2018-10-05",
            "Sonic Assault can target a tapped creature. The creature can't be tapped again, but Sonic Assault " +
                "still deals 2 damage to that creature's controller."
        )
        ruling(
            "2018-10-05",
            "If the target creature becomes an illegal target for Sonic Assault, the spell doesn't resolve. " +
                "No player is dealt damage."
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
                "alternative cost when you jump-start a spell. You'll still discard a card as an additional cost to cast it."
        )
        ruling(
            "2018-10-05",
            "If a card with jump-start is put into your graveyard during your turn, you'll be able to cast it right " +
                "away if it's legal to do so, before an opponent can take any actions."
        )
    }
}
