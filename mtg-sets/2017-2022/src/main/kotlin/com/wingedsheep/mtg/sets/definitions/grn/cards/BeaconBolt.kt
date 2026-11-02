package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastThisWayExile
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Beacon Bolt
 * {1}{U}{R}
 * Sorcery
 *
 * Beacon Bolt deals damage to target creature equal to the total number of instant and sorcery
 * cards you own in exile and in your graveyard.
 * Jump-start
 *
 * The count is the sum of two zone counts. Exile zones are keyed by owner, so
 * `count(Player.You, Zone.EXILE)` is exactly "cards you own in exile". The filter is `faceUp()`
 * because face-down exiled cards have no characteristics (ruling 2018-10-05). Beacon Bolt is on
 * the stack while it resolves, so it never counts itself.
 *
 * Jump-start works like Radical Idea's: the card may be cast from its owner's graveyard by
 * discarding a card, and it is exiled however it leaves the stack.
 */
val BeaconBolt = card("Beacon Bolt") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Beacon Bolt deals damage to target creature equal to the total number of instant and " +
        "sorcery cards you own in exile and in your graveyard.\n" +
        "Jump-start (You may cast this card from your graveyard by discarding a card in addition to " +
        "paying its other costs. Then exile this card.)"

    spell {
        val creature = target(TargetFilter.Creature)
        val spells = GameObjectFilter.InstantOrSorcery.faceUp()
        effect = Effects.DealDamage(
            DynamicAmounts.count(Player.You, Zone.EXILE, spells) +
                DynamicAmounts.count(Player.You, Zone.GRAVEYARD, spells),
            creature
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
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39c9e4b5-364b-4c0b-bb47-266563a6abf2.jpg?1783934142"
        ruling(
            "2018-10-05",
            "Beacon Bolt is still on the stack while you count your instant and sorcery cards in your graveyard " +
                "and in exile. It doesn't count itself."
        )
        ruling(
            "2018-10-05",
            "If any exiled cards you own are face down, they have no characteristics. If they're normally " +
                "instants or sorceries, they won't be counted."
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
