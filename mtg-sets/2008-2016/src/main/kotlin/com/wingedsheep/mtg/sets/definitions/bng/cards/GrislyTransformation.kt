package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Grisly Transformation
 * {2}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, draw a card.
 * Enchanted creature has intimidate. (It can't be blocked except by artifact creatures and/or
 * creatures that share a color with it.)
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val GrislyTransformation = card("Grisly Transformation") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, draw a card.\n" +
        "Enchanted creature has intimidate. (It can't be blocked except by artifact creatures and/or creatures that share a color with it.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.INTIMIDATE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4eb0ce84-560f-4e39-a0ab-7bd710d541cc.jpg?1783939555"
    }
}
