package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lurking Deadeye
 * {3}{B}
 * Creature — Human Assassin
 * 4/2
 * Flash
 * When this creature enters, destroy target creature that was dealt damage this turn.
 */
val LurkingDeadeye = card("Lurking Deadeye") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    oracleText = "Flash\nWhen this creature enters, destroy target creature that was dealt damage this turn."
    power = 4
    toughness = 2
    keywords(Keyword.FLASH)
    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.wasDealtDamageThisTurn())
        effect = Effects.Destroy(t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Livia Prima"
        flavorText = "\"There's no roar so mighty that it can't be silenced.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43925a8d-dd02-4907-929e-c015d678bb49.jpg?1783931058"
    }
}
