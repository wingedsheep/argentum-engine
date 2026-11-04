package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val AerialGuide = card("Aerial Guide") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    oracleText = "Flying\nWhenever this creature attacks, another target attacking creature gains flying until end of turn."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val otherAttacker = target(TargetFilter.AttackingCreature.other())
        effect = Effects.GrantKeyword(Keyword.FLYING, otherAttacker)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"Kefnet taught us to turn obstacles into advantages.\"\n—Neponem, vizier of Kefnet"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b744c138-9b9e-47f6-9d56-feb6a78ce377.jpg?1783936054"
    }
}
