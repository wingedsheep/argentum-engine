package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Copperhoof Vorrac — Mirrodin #116
 * {3}{G}{G} · Creature — Boar Beast · 2/2
 *
 * This creature gets +1/+1 for each untapped permanent your opponents control.
 *
 * Modelling notes:
 * - "permanent", not "creature" — lands, artifacts, and enchantments all count, so the filter is
 *   the unrestricted [GameObjectFilter.Any] narrowed to untapped, counted over the battlefield of
 *   every opponent.
 * - [GrantDynamicStatsEffect] is a continuously-recomputed layer 7c bonus, which is what the card
 *   needs: the Vorrac shrinks the moment an opponent taps out and grows back on their untap step,
 *   including mid-combat.
 */
val CopperhoofVorrac = card("Copperhoof Vorrac") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Boar Beast"
    power = 2
    toughness = 2
    oracleText = "This creature gets +1/+1 for each untapped permanent your opponents control."

    staticAbility {
        val untappedOpponentPermanents =
            DynamicAmounts.battlefield(Player.EachOpponent, GameObjectFilter.Any.untapped()).count()
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = untappedOpponentPermanents,
            toughnessBonus = untappedOpponentPermanents
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "116"
        artist = "Matt Cavotta"
        flavorText = "Like all forest beasts, it lives by one rule: if there's no room to grow, make some."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81fff4cc-b2ab-4a41-bede-0d807552ba46.jpg?1783944535"
    }
}
