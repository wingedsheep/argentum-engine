package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Myr Scrapling — Modern Horizons 2 #230
 * {1} · Artifact Creature — Myr · 1 / 1
 *
 * Sacrifice this creature: Put a +1/+1 counter on target creature.
 *
 * A free sacrifice outlet that hands its body's worth of stats to something else — the artifact
 * half makes it fodder for the set's Arcbound and affinity shells as well.
 *
 * The sacrifice is the whole cost ([Costs.SacrificeSelf], no mana), so the Myr is gone before the
 * ability resolves. That matters for the target: "target creature" is unrestricted, but the Myr
 * itself can never be the target it pays for, since a creature that has left the battlefield is
 * no longer a legal target and the ability would simply be countered on resolution.
 *
 * Counters use the string vocabulary ([Counters.PLUS_ONE_PLUS_ONE]) here because this is an
 * effect, not a replacement effect — `CounterTypeFilter` is the counterpart used by
 * `EntersWithCounters`.
 */
val MyrScrapling = card("Myr Scrapling") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Myr"
    power = 1
    toughness = 1
    oracleText = "Sacrifice this creature: Put a +1/+1 counter on target creature."

    activatedAbility {
        cost = Costs.SacrificeSelf
        val t = target("target creature", Targets.Creature)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
        description = "Sacrifice this creature: Put a +1/+1 counter on target creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "230"
        artist = "Svetlin Velinov"
        flavorText = "\"They're useful creatures, but quite replaceable. I find it best not to get too attached.\"\n—Pontifex, elder researcher"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b9072a5-bd7f-4007-a34a-ebe251c95356.jpg?1783926803"
    }
}
