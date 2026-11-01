package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sky Tether
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has defender and loses flying.
 *
 * Both statics apply in layer 6 in timestamp order, so a flying grant with a later timestamp
 * still gives the creature flying (per the ruling).
 */
val SkyTether = card("Sky Tether") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has defender and loses flying."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.DEFENDER)
    }

    staticAbility {
        ability = RemoveKeywordStatic(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Zezhou Chen"
        flavorText = "\"If you can't control your mount, I will control it for you.\"\n—Mirela, Azorius hussar"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39c47be3-0228-46de-b4b5-911fb86d4596.jpg?1783933718"

        ruling("2019-01-25", "Sky Tether can enchant a creature that never had flying at all.")
        ruling("2019-01-25", "The enchanted creature can later gain flying from another effect.")
    }
}
