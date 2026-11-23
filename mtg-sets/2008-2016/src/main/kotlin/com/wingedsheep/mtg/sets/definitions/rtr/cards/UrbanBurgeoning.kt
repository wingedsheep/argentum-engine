package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.UntapSelfDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Urban Burgeoning
 * {G}
 * Enchantment — Aura
 *
 * Enchant land
 * Enchanted land has "Untap this land during each other player's untap step."
 *
 * The granted ability lives on the land, so "each other player" is relative to the land's
 * controller — who may not be the Aura's controller.
 */
val UrbanBurgeoning = card("Urban Burgeoning") {
    manaCost = "{G}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"Untap this land during each other player's untap step.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantStaticAbility(UntapSelfDuringOtherUntapSteps, filter = Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Nic Klein"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/393c230f-5bc3-4b71-b5ac-81d5ce227df5.jpg?1783940346"
    }
}
