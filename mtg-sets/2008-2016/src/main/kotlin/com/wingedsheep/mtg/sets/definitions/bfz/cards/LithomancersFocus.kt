package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Lithomancer's Focus — Battle for Zendikar #38 (canonical printing)
 * {W} · Instant
 *
 * Target creature gets +2/+2 until end of turn. Prevent all damage that would be dealt to that
 * creature this turn by colorless sources.
 *
 * The shield is a `Matching` prevent-all shield on the one target: the creature is pinned at
 * resolution, but whether a source is colorless is read when its damage would be dealt, so a
 * source that wasn't colorless (or didn't exist) as the Focus resolved is still covered.
 */
val LithomancersFocus = card("Lithomancer's Focus") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. Prevent all damage that would be dealt to that creature this turn by colorless sources."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, t) then
            Effects.PreventDamage(
                target = t,
                sources = PreventionSourceFilter.Matching(
                    GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless)
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Cynthia Sheppard"
        flavorText = "\"Even in the hands of amateurs, Nahiri's hedrons can harm the Eldrazi.\"\n—Ugin"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd5a1376-88b3-4bdc-9ce9-e90617e6c55e.jpg?1783938217"
        ruling("2015-08-25", "Lithomancer's Focus will prevent damage to the creature from any colorless source that turn, even if that source didn't exist or wasn't colorless as Lithomancer's Focus resolved.")
    }
}
