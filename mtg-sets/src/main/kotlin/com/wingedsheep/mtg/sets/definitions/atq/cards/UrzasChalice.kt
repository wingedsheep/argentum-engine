package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Urza's Chalice
 * {1}
 * Artifact
 * Whenever a player casts an artifact spell, you may pay {1}. If you do, you gain 1 life.
 */
val UrzasChalice = card("Urza's Chalice") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever a player casts an artifact spell, you may pay {1}. If you do, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.anyPlayerCasts(GameObjectFilter.Artifact)
        effect = MayPayManaEffect(ManaCost.parse("{1}"), Effects.GainLife(1))
        description = "Whenever a player casts an artifact spell, you may pay {1}. If you do, you gain 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Jeff A. Menges"
        flavorText = "When sorely wounded or tired, Urza would often retreat to the workshops of his apprentices. They were greatly amazed at how much better he looked each time he took a sip of water."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3728537-86d3-42be-9046-90bba1bfafc1.jpg?1562946411"
    }
}
