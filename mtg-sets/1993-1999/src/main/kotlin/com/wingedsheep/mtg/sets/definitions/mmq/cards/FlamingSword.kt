package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Flaming Sword
 * {1}{R}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature gets +1/+0 and has first strike.
 *
 * Same flash-Aura frame as [Buoyancy]. "gets +1/+0 **and** has first strike" is two statics, not
 * one — [ModifyStats] lives in layer 7c and [GrantKeyword] in layer 6, so the SDK keeps them
 * separate (Agility / Cursed Flesh shape). Both default their filter to the attached creature.
 */
val FlamingSword = card("Flaming Sword") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature gets +1/+0 and has first strike."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Randy Gallegos"
        flavorText = "\"It's not Talruum crystal, but I must admit—it gets the job done.\"\n" +
            "—Tahngarth"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17ecd9ff-8c30-4e17-8cff-dd40d653c4af.jpg"
    }
}
