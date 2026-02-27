package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wingbeat Warrior
 * {2}{W}
 * Creature — Bird Soldier Warrior
 * 2/1
 * Flying
 * Morph {2}{W}
 * When this creature is turned face up, target creature gains first strike until end of turn.
 */
val WingbeatWarrior = card("Wingbeat Warrior") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Bird Soldier Warrior"
    power = 2
    toughness = 1
    oracleText = "Flying\nMorph {2}{W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, target creature gains first strike until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("creature", Targets.Creature)
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, t)
    }

    morph = "{2}{W}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd58d164-861d-4c80-ad2f-6283ed82faa1.jpg?1562936257"
    }
}
