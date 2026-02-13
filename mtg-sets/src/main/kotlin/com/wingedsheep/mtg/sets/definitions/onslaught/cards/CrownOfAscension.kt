package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantToEnchantedCreatureTypeGroupEffect

/**
 * Crown of Ascension
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has flying.
 * Sacrifice Crown of Ascension: Enchanted creature and other creatures that share
 * a creature type with it gain flying until end of turn.
 */
val CrownOfAscension = card("Crown of Ascension") {
    manaCost = "{1}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has flying.\nSacrifice Crown of Ascension: Enchanted creature and other creatures that share a creature type with it gain flying until end of turn."

    auraTarget = Targets.Creature

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = GrantToEnchantedCreatureTypeGroupEffect(
            keyword = Keyword.FLYING
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Bradley Williams"
        flavorText = "\"Wisdom, clear my eyes.\""
        imageUri = "https://cards.scryfall.io/large/front/2/f/2fe86733-7851-4c2a-8d94-dba6f071b94d.jpg?1562906205"
    }
}
