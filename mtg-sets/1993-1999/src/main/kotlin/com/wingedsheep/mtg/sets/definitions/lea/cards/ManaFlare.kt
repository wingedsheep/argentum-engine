package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mana Flare
 * {2}{R}
 * Enchantment
 * Whenever a player taps a land for mana, that player adds one mana of any type that land produced.
 *
 * Heartbeat of Spring's shape: [AdditionalManaOnSourceTap] over every land (any controller) with
 * `color = null`, which mirrors the type the land produced for the tapping player. The bonus is a
 * triggered mana ability, so it resolves immediately without using the stack.
 */
val ManaFlare = card("Mana Flare") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a player taps a land for mana, that player adds one mana of any type that land produced."

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Land,
            color = null // mirror the produced type ("any type that land produced")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fb99a26-beeb-4aca-bb02-b2d2ce0595f9.jpg?1783948683"
        ruling("2023-09-01", "If you tap a land for more than one mana, you choose one type that was produced and add one mana of that type.")
        ruling("2023-09-01", "Mana Flare doesn't care about any restrictions or riders your lands put on the mana they produce, such as those of Unclaimed Territory and Cavern of Souls. It just produces one mana of the appropriate type, with no restrictions or riders.")
    }
}
