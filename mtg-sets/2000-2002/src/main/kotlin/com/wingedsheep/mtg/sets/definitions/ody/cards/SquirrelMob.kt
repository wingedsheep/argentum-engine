package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Squirrel Mob
 * {1}{G}{G}
 * Creature — Squirrel
 * 2/2
 * This creature gets +1/+1 for each other Squirrel on the battlefield.
 *
 * The count has no controller clause, so it is [Player.Each] — opponents' Squirrels count too —
 * and a bare tribal noun means any permanent with the subtype, not just creatures. "Other" is the
 * aggregate's own `excludeSelf`.
 */
val SquirrelMob = card("Squirrel Mob") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Squirrel"
    power = 2
    toughness = 2
    oracleText = "This creature gets +1/+1 for each other Squirrel on the battlefield."

    staticAbility {
        val otherSquirrels = DynamicAmounts.battlefield(
            Player.Each,
            GameObjectFilter.Permanent.withSubtype(Subtype.SQUIRREL),
            excludeSelf = true
        ).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = otherSquirrels,
            toughnessBonus = otherSquirrels
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "273"
        artist = "Carl Critchlow"
        flavorText = "An army of squirrels is still an army."
        imageUri = "https://cards.scryfall.io/normal/front/1/8/181254ce-259a-4b31-8937-728564f2baf3.jpg?1783945210"
    }
}
