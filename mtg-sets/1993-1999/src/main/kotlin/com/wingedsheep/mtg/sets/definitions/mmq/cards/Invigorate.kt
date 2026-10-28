package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invigorate
 * {2}{G}
 * Instant
 * If you control a Forest, rather than pay this spell's mana cost, you may have an opponent gain 3 life.
 * Target creature gets +4/+4 until end of turn.
 *
 * Snuff Out's conditional pitch shape with an [Costs.additional.OthersGainLife] half: the caster
 * names the opponent while casting.
 */
val Invigorate = card("Invigorate") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "If you control a Forest, rather than pay this spell's mana cost, you may have an opponent gain 3 life.\n" +
        "Target creature gets +4/+4 until end of turn."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(Costs.additional.OthersGainLife(3)),
        condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Forest")),
    )

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(4, 4, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "254"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/406b343c-90b5-4a4d-91c3-2fddcc9a0e05.jpg?1783945923"
    }
}
