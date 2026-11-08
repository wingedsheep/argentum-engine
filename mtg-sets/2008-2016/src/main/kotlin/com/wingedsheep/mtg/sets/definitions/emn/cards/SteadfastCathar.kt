package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Steadfast Cathar
 * {1}{W}
 * Creature — Human Soldier
 * 2/1
 * Whenever this creature attacks, it gets +0/+2 until end of turn.
 */
val SteadfastCathar = card("Steadfast Cathar") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Whenever this creature attacks, it gets +0/+2 until end of turn."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(0, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Dan Murayama Scott"
        flavorText = "\"The most resolute soldiers will press forward in the face of overwhelming odds.\"\n" +
            "—Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e1b65a7-d385-428b-986c-a0d9283a5f75.jpg"
    }
}
