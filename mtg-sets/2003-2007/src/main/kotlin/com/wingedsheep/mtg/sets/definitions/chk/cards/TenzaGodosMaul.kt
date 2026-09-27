package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tenza, Godo's Maul
 * {3}
 * Legendary Artifact — Equipment
 * Equipped creature gets +1/+1. As long as it's legendary, it gets an additional +2/+2.
 * As long as it's red, it has trample.
 * Equip {1}
 *
 * Both riders are statics gated by [Conditions.EntityMatches] on [EffectTarget.EquippedCreature],
 * so they track the equipped creature's (projected) supertype and color continuously.
 */
val TenzaGodosMaul = card("Tenza, Godo's Maul") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1. As long as it's legendary, it gets an additional +2/+2. " +
        "As long as it's red, it has trample.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }
    staticAbility {
        condition = Conditions.EntityMatches(
            EffectTarget.EquippedCreature,
            GameObjectFilter.Creature.legendary()
        )
        ability = ModifyStats(2, 2, Filters.EquippedCreature)
    }
    staticAbility {
        condition = Conditions.EntityMatches(
            EffectTarget.EquippedCreature,
            GameObjectFilter.Creature.withColor(Color.RED)
        )
        ability = GrantKeyword(Keyword.TRAMPLE)
    }
    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "271"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1aa03d7-ac4f-4793-8f8d-5ffd2f8f38b1.jpg?1783944275"
    }
}
