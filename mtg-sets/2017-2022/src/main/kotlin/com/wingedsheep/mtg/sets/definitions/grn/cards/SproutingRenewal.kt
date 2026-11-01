package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sprouting Renewal
 * {2}{G}
 * Sorcery
 * Convoke
 * Choose one —
 * • Create a 2/2 green and white Elf Knight creature token with vigilance.
 * • Destroy target artifact or enchantment.
 */
val SproutingRenewal = card("Sprouting Renewal") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting " +
        "this spell pays for {1} or one mana of that creature's color.)\n" +
        "Choose one —\n" +
        "• Create a 2/2 green and white Elf Knight creature token with vigilance.\n" +
        "• Destroy target artifact or enchantment."

    keywords(Keyword.CONVOKE)

    spell {
        modal {
            mode("Create a 2/2 green and white Elf Knight creature token with vigilance") {
                effect = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.GREEN, Color.WHITE),
                    creatureTypes = setOf("Elf", "Knight"),
                    keywords = setOf(Keyword.VIGILANCE)
                )
            }
            mode("Destroy target artifact or enchantment") {
                val permanent = target(TargetFilter.ArtifactOrEnchantment)
                effect = Effects.Destroy(permanent)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/668232ab-5e58-49f3-804a-c938252ff177.jpg?1783934146"
    }
}
