package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aspect of Gorgon
 * {2}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +1/+3 and has deathtouch.
 *
 * Two statics over the enchanted creature (the default scope of both): the pump in Layer 7c, the
 * keyword grant in Layer 6.
 */
val AspectOfGorgon = card("Aspect of Gorgon") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+3 and has deathtouch. (Any amount of damage it deals to a creature is enough to destroy it.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 3)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.DEATHTOUCH)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Willian Murai"
        flavorText = "\"My adopted children are loved no less.\"\n—Pharika, god of affliction"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be8956d0-5502-415d-bead-d8bbddf9871d.jpg?1783939440"
    }
}
