package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Heartbeat of Spring
 * {2}{G}
 * Enchantment
 * Whenever a player taps a land for mana, that player adds one mana of any type that land produced.
 *
 * The global Mana Flare shape: [AdditionalManaOnSourceTap] over every land (any controller) with
 * `color = null`, which mirrors the type the land produced for the tapping player. The bonus is a
 * triggered mana ability, so it resolves immediately without using the stack.
 */
val HeartbeatOfSpring = card("Heartbeat of Spring") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
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
        collectorNumber = "212"
        artist = "Rob Alexander"
        flavorText = "\"It is true that we monks hold the key to paradise, but most don't understand that the paradise we guard is the one within.\"\n—Diary of Azusa"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6be3ba05-dce0-463b-bfe6-d51df63cc1a8.jpg?1783944290"
        ruling("2020-08-07", "If you tap a land for more than one mana, you choose one type that was produced and add one mana of that type.")
        ruling("2020-08-07", "Heartbeat of Spring doesn't care about any restrictions or riders your lands put on the mana they produce, such as those of Unclaimed Territory and Cavern of Souls. It just produces one mana of the appropriate type, with no restrictions or riders.")
    }
}
