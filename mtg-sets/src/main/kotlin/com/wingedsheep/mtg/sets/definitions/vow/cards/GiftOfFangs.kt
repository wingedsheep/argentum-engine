package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gift of Fangs
 * {B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+2 as long as it's a Vampire. Otherwise, it gets -2/-2.
 */
val GiftOfFangs = card("Gift of Fangs") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2 as long as it's a Vampire. Otherwise, it gets -2/-2."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, 2, GroupFilter.attachedCreature())
        condition = EnchantedCreatureHasSubtype(Subtype("Vampire"))
    }

    staticAbility {
        ability = ModifyStats(-2, -2, GroupFilter.attachedCreature())
        condition = Conditions.Not(EnchantedCreatureHasSubtype(Subtype("Vampire")))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Dominik Mayer"
        flavorText = "\"You are one of the few to whom we offer our blessing. What fool would reject " +
            "immortality?\"\n—Runo Stromkirk"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a864375f-99c3-4c68-9440-bc25ff6d0dc0.jpg?1782703111"
    }
}
