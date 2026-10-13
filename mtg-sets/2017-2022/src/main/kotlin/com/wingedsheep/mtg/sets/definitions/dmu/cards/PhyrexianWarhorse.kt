package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val PhyrexianWarhorse = card("Phyrexian Warhorse") {
    manaCost = "{3}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Phyrexian Horse"
    power = 3
    toughness = 3
    oracleText = "Kicker {W} (You may pay an additional {W} as you cast this spell.)\nWhen this creature enters, if it was kicked, create a 1/1 white Soldier creature token.\n{1}, Sacrifice another creature: This creature gets +2/+1 until end of turn."

    keywordAbility(KeywordAbility.kicker("{W}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/8/c/8c4b0257-2ca5-4015-9d63-d7cf6e87ab9d.jpg?1783921132",
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeAnother(GameObjectFilter.Creature))
        effect = Effects.ModifyStats(2, 1, EffectTarget.Self)
        description = "{1}, Sacrifice another creature: This creature gets +2/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f42167ef-7858-4c66-b1bc-41c3288a8ee8.jpg?1783921328"
    }
}
