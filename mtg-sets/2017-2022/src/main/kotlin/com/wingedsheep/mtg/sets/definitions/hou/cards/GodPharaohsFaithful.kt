package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * God-Pharaoh's Faithful
 * {W}
 * Creature — Human Wizard
 * 0/4
 *
 * Whenever you cast a blue, black, or red spell, you gain 1 life.
 *
 * The colour filter is an OR: a spell that is any one of blue, black or red qualifies (a multicoloured
 * spell triggers it once, not once per matching colour).
 */
val GodPharaohsFaithful = card("God-Pharaoh's Faithful") {
    manaCost = "{W}"
    typeLine = "Creature — Human Wizard"
    power = 0
    toughness = 4
    oracleText = "Whenever you cast a blue, black, or red spell, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withAnyColor(Color.BLUE, Color.BLACK, Color.RED))
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Bastien L. Deharme"
        flavorText = "\"This is the final trial! Rejoice, for the God-Pharaoh soars among us!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cb8a25b-2ad0-4ffe-b41a-6b3f8b48c1e9.jpg?1783936061"
    }
}
