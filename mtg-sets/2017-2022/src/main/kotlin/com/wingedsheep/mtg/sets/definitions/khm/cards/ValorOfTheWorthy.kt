package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Valor of the Worthy
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1.
 * When enchanted creature leaves the battlefield, create a 1/1 white Spirit creature token with flying.
 */
val ValorOfTheWorthy = card("Valor of the Worthy") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1.\n" +
        "When enchanted creature leaves the battlefield, create a 1/1 white Spirit creature token with flying."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    triggeredAbility {
        trigger = Triggers.attached.leaves()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Mathias Kollros"
        flavorText = "\"If I am to die, let it be a hero's death!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83632e73-be3f-43fc-932f-45cb1af4f8fb.jpg?1783928272"
    }
}
