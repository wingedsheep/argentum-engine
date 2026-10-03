package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fowl Strike — Modern Horizons 3 #155
 * {1}{G} · Instant
 *
 * Destroy target creature with flying.
 * Reinforce 2—{2}{G} ({2}{G}, Discard this card: Put two +1/+1 counters on target creature.)
 *
 * Reinforce (CR 702.77) is an activated ability that functions only from hand: modelled as
 * `activateFromZone = Zone.HAND` with a [Costs.DiscardSelf] + mana composite cost, the same shape
 * channel and Spinewoods Armadillo use. It is activated at instant speed, like any activated ability.
 */
val FowlStrike = card("Fowl Strike") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target creature with flying.\n" +
        "Reinforce 2—{2}{G} ({2}{G}, Discard this card: Put two +1/+1 counters on target creature.)"

    spell {
        val t = target(TargetFilter.Creature.withKeyword(Keyword.FLYING))
        effect = Effects.Destroy(t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, t)
        description = "Reinforce 2—{2}{G}: Put two +1/+1 counters on target creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Warren Mahy"
        flavorText = "With a resounding wallop, the ancient battle between buzzard and druid was finally finished."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83b6c7ab-f42f-40d2-9cb6-89291d57e27f.jpg?1783911260"
    }
}
