package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Arashin Foremost
 * {1}{W}{W}
 * Creature — Human Warrior
 * 2/2
 * Double strike
 * Whenever this creature enters or attacks, another target Warrior creature you control gains
 * double strike until end of turn.
 *
 * "Enters or attacks" is two triggered abilities sharing one targeted grant; `other()` keeps the
 * Foremost itself out of its own target set.
 */
val ArashinForemost = card("Arashin Foremost") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Double strike\n" +
        "Whenever this creature enters or attacks, another target Warrior creature you control gains double strike until end of turn."

    keywords(Keyword.DOUBLE_STRIKE)

    val otherWarriorYouControl = TargetFilter.Creature.withSubtype(Subtype.WARRIOR).youControl().other()

    triggeredAbility {
        trigger = Triggers.self.enters()
        val warrior = target(otherWarriorYouControl)
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, warrior)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val warrior = target(otherWarriorYouControl)
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, warrior)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "David Palumbo"
        flavorText = "\"I would gladly give my life if it would inspire my clan to victory.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f88ecc5-df1d-47e6-8cf3-1d708e2389e4.jpg"
    }
}
