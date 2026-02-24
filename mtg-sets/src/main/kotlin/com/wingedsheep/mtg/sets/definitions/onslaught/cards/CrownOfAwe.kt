package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.effects.GrantToEnchantedCreatureTypeGroupEffect

/**
 * Crown of Awe
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has protection from black and from red.
 * Sacrifice Crown of Awe: Enchanted creature and other creatures that share
 * a creature type with it gain protection from black and from red until end of turn.
 */
val CrownOfAwe = card("Crown of Awe") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has protection from black and from red.\nSacrifice Crown of Awe: Enchanted creature and other creatures that share a creature type with it gain protection from black and from red until end of turn."

    auraTarget = Targets.Creature

    staticAbility {
        ability = GrantProtection(Color.BLACK)
    }

    staticAbility {
        ability = GrantProtection(Color.RED)
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = GrantToEnchantedCreatureTypeGroupEffect(
            protectionColors = setOf(Color.BLACK, Color.RED)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Randy Elliott"
        flavorText = "The crown never rests easy on a thoughtless brow."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aeaea4bc-dcea-4340-a039-ebc97b944673.jpg?1562936272"
    }
}
