package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Revelation of Power — Streets of New Capenna #28
 * {1}{W} · Instant
 *
 * Target creature gets +2/+2 until end of turn. If it has a counter on it, it also gains flying
 * and lifelink until end of turn.
 *
 * The +2/+2 always applies; the flying + lifelink grant is a resolution-time rider
 * ([Effects.If] over [Conditions.TargetMatchesFilter] on the same target) checking for a counter
 * of any kind, so a creature with counters gets all three, not one instead of the other.
 */
val RevelationOfPower = card("Revelation of Power") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. If it has a counter on it, it also gains " +
        "flying and lifelink until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, creature) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withAnyCounter(), creature),
                then = Effects.GrantKeyword(Keyword.FLYING, creature) then
                    Effects.GrantKeyword(Keyword.LIFELINK, creature),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Eric Deschamps"
        flavorText = "As Ob Nixilis closed in, Halo swept through Giada like an electrical current, and " +
            "inside her, something long dormant unfurled its wings."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e010b11-8f3e-4792-afe4-0a376650b6c4.jpg?1783923152"
    }
}
