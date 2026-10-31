package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Aerial Assault
 * {2}{W}
 * Sorcery
 *
 * Destroy target tapped creature. You gain 1 life for each creature you control with flying.
 *
 * The flier count is read as the life gain resolves, after the destroy — so a flier of yours that
 * was the target no longer counts, and an indestructible target that survives still lets you gain.
 */
val AerialAssault = card("Aerial Assault") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy target tapped creature. You gain 1 life for each creature you control with flying."

    spell {
        val victim = target(TargetFilter.TappedCreature)
        effect = Effects.Destroy(victim) then Effects.GainLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.withKeyword(Keyword.FLYING)).count()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Dan Murayama Scott"
        flavorText = "\"Oh, you must be mistaken. Hawks are solitary hunters, you see.\"\n—Bunder Diedreck, ornithologist"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64d9c182-cbb3-4791-90dd-0e533ddeebda.jpg?1783933035"
        ruling("2019-07-12", "If the target creature is an illegal target by the time Aerial Assault tries to resolve, the spell doesn't resolve. You won't gain life. If the target is legal but is not destroyed (most likely because it has indestructible), you will gain life.")
        ruling("2019-07-12", "Any abilities that trigger as the target creature is destroyed won't resolve until after you've gained life.")
    }
}
