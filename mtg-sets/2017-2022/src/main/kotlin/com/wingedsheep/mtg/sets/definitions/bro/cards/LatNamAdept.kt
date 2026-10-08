package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lat-Nam Adept
 * {3}{U}
 * Creature — Human Wizard
 * 3/3
 * Whenever you draw your second card each turn, put a +1/+1 counter on this creature.
 */
val LatNamAdept = card("Lat-Nam Adept") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "Whenever you draw your second card each turn, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Zara Alfonso"
        flavorText = "\"Every book is a doorway to enlightenment. Curiosity is your key.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1e088de-e99f-4706-89e0-a7efdaf9403a.jpg"
    }
}
