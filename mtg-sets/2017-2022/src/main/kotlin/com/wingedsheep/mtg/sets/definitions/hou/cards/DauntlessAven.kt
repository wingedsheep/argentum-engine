package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val DauntlessAven = card("Dauntless Aven") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Warrior"
    oracleText = "Flying\nWhenever this creature attacks, untap target creature you control."
    power = 2
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.Untap(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Zack Stella"
        flavorText = "In the chaos brought by the new gods, swift-winged aven served as pathfinders, leading the last of the people away from the ruins of Naktamun."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01e6320c-6f8b-4582-ad11-9b868bff08a1.jpg?1783936067"
    }
}
