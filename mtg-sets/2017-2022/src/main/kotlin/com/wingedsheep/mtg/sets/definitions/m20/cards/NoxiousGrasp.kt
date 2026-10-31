package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Noxious Grasp (M20 #110)
 * {1}{B}  Instant
 *
 * Destroy target creature or planeswalker that's green or white. You gain 1 life.
 */
val NoxiousGrasp = card("Noxious Grasp") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature or planeswalker that's green or white. You gain 1 life."

    spell {
        val victim = target(
            TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.withAnyColor(Color.GREEN, Color.WHITE))
        )
        effect = Effects.Destroy(victim) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Tomasz Jedruszek"
        flavorText = "\"The bog doesn't give a stinking bubble about your title, wealth, or piety. It's just hungry.\"\n—Byaras, backwoods guide"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e5758cc-1f84-455d-a983-8ec471727eaf.jpg"
    }
}
