package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Enlightened Ascetic
 * {1}{W}
 * Creature — Cat Monk
 * 1/1
 * When this creature enters, you may destroy target enchantment.
 */
val EnlightenedAscetic = card("Enlightened Ascetic") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Monk"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, you may destroy target enchantment."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target enchantment", Targets.Enchantment)
        effect = MayEffect(Effects.Destroy(t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "James Zapata"
        flavorText = "\"I do not reject the gods. I reject their authority, their pettiness, and their arrogance.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76549fc3-5798-4c70-bb70-802b6f597eb7.jpg?1783938363"
    }
}
