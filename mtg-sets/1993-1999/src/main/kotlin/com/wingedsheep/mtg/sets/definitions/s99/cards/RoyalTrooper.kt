package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val RoyalTrooper = card("Royal Trooper") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Soldier"
    oracleText = "Whenever this creature blocks, it gets +2/+2 until end of turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Scott M. Fischer"
        flavorText = "\"Fortune does not side with the faint-hearted.\"\n—Sophocles, *Phaedra*"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1dc22489-6754-4418-a991-7046c14a0934.jpg?1783946048"
    }
}
