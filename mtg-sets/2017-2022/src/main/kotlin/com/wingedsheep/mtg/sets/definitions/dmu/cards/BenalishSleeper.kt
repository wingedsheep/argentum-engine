package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val BenalishSleeper = card("Benalish Sleeper") {
    manaCost = "{1}{W}"
    colorIdentity = "WB"
    typeLine = "Creature — Phyrexian Human Soldier"
    power = 3
    toughness = 1
    oracleText = "Kicker {B} (You may pay an additional {B} as you cast this spell.)\nWhen this creature enters, if it was kicked, each player sacrifices a creature of their choice."

    keywordAbility(KeywordAbility.kicker("{B}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f96089ea-20cf-496d-84a6-9b660c2ef672.jpg?1783921370"
    }
}
