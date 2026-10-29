package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Rousing Read
 * {2}{U}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, draw two cards, then discard a card.
 * Enchanted creature gets +1/+1 and has flying.
 */
val RousingRead = card("Rousing Read") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, draw two cards, then discard a card.\n" +
        "Enchanted creature gets +1/+1 and has flying."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(2) then Effects.Discard(1)
        description = "When this Aura enters, draw two cards, then discard a card."
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Campbell White"
        flavorText = "Great literature can transport the reader."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c82be2ca-8350-4cf5-83b6-e8b60a9e21c6.jpg?1783930721"
        ruling("2020-06-23", "If the creature Rousing Read would enchant is an illegal target by the time the Aura spell resolves, the entire spell doesn't resolve. It won't enter the battlefield, so its ability won't trigger.")
    }
}
