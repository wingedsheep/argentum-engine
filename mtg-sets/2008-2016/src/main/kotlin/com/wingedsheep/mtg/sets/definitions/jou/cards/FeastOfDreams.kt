package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Feast of Dreams
 * {1}{B}
 * Instant
 * Destroy target enchanted creature or enchantment creature.
 *
 * Temple Thief's union filter as a target: creatures with an Aura attached (`enchanted()`) or
 * creatures that are also enchantments. The branches carry different state predicates, so `or`
 * builds an `anyOf` union rather than flattening (as Cut Short's target does).
 */
val FeastOfDreams = card("Feast of Dreams") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target enchanted creature or enchantment creature."

    spell {
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.enchanted() or (GameObjectFilter.Creature and GameObjectFilter.Enchantment)
            )
        )
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Clint Cearley"
        flavorText = "\"We gave the gods the power to war against us. It is time we took that power back.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de07e21e-c12a-47a6-ad2c-ef6fed343407.jpg?1783939436"
    }
}
