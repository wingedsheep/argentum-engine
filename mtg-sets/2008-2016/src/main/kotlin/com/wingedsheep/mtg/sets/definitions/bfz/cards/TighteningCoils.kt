package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tightening Coils
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -6/-0 and loses flying.
 *
 * The flying removal applies in layer 6 in timestamp order, so a flying grant with a later
 * timestamp still gives the creature flying (per the ruling).
 */
val TighteningCoils = card("Tightening Coils") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets -6/-0 and loses flying."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-6, 0)
    }

    staticAbility {
        ability = RemoveKeywordStatic(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Tyler Jacobson"
        flavorText = "Having gone to the trouble of stealing a bident from a god on another world, " +
            "Kiora was eager to test its capabilities."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a014dd06-626d-4917-8981-e0064718b571.jpg?1783938206"

        ruling(
            "2015-08-25",
            "If an effect gives the enchanted creature flying after Tightening Coils became " +
                "attached to it, the creature will have flying.",
        )
    }
}
