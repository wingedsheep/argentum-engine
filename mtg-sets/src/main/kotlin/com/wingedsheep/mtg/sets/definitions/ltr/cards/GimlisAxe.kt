package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Gimli's Axe
 * {2}{R}
 * Artifact — Equipment
 *
 * Equipped creature gets +3/+0.
 * As long as equipped creature is legendary, it has menace.
 * Equip {2}
 */
val GimlisAxe = card("Gimli's Axe") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+0.\n" +
        "As long as equipped creature is legendary, it has menace. (It can't be blocked except by two or more creatures.)\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    // Equipped creature gets +3/+0
    staticAbility {
        ability = ModifyStats(+3, 0, Filters.EquippedCreature)
    }

    // As long as equipped creature is legendary, it has menace.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.MENACE),
            condition = Conditions.EnchantedCreatureIsLegendary()
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Pablo Mendoza"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8999b6c-e501-48d0-ae51-a35b96f996ec.jpg?1686968971"
    }
}
