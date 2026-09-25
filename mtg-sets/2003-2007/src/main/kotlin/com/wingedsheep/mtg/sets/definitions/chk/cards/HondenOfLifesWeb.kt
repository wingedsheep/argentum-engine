package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Honden of Life's Web
 * {4}{G}
 * Legendary Enchantment — Shrine
 * At the beginning of your upkeep, create a 1/1 colorless Spirit creature token for each Shrine
 * you control.
 *
 * CHK printed no tokens; the art is Eternal Masters' Spirit, the set that reprinted the cycle.
 */
val HondenOfLifesWeb = card("Honden of Life's Web") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "At the beginning of your upkeep, create a 1/1 colorless Spirit creature token for each Shrine you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count(),
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Spirit"),
            imageUri = "https://cards.scryfall.io/normal/front/0/8/082c3bad-3fea-4c3f-8263-4b16139bb32a.jpg?1783937532"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Rob Alexander"
        flavorText = "To the sorrow of all, its web became a net that strangled those who helped weave it."
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c618af6-b02e-448f-9131-3c50ef0d433b.jpg?1783944289"
    }
}
