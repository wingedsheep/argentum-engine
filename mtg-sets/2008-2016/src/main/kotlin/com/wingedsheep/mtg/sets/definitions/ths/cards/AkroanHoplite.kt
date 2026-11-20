package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Akroan Hoplite
 * {R}{W}
 * Creature — Human Soldier
 * 1/2
 * Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the number of
 * attacking creatures you control.
 */
val AkroanHoplite = card("Akroan Hoplite") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 2
    oracleText = "Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the number of attacking creatures you control."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(
            DynamicAmounts.attackingCreaturesYouControl(),
            DynamicAmounts.fixed(0),
            EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "185"
        artist = "Igor Kieryluk"
        flavorText = "\"Fair fight? How could it be a fair fight? We're Akroans. They're not.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/9/695ccac5-c87e-49af-9131-8f1730e84bc0.jpg?1783939734"
    }
}
