package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Honden of Night's Reach
 * {3}{B}
 * Legendary Enchantment — Shrine
 * At the beginning of your upkeep, target opponent discards a card for each Shrine you control.
 *
 * The discarding opponent chooses which cards; the Shrine count is read on resolution.
 */
val HondenOfNightsReach = card("Honden of Night's Reach") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "At the beginning of your upkeep, target opponent discards a card for each Shrine you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count(),
            opponent
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Jim Nelson"
        flavorText = "To the sorrow of all, its dark reach grasped and crushed those who guarded its silent vigil."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/526419ae-5a49-40e8-ac45-f213a76c7326.jpg?1783944314"
    }
}
