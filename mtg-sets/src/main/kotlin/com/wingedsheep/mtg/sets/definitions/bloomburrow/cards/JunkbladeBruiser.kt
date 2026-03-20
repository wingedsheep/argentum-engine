package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Junkblade Bruiser
 * {3}{R/G}{R/G}
 * Creature — Raccoon Berserker
 * 4/5
 *
 * Trample
 *
 * Whenever you expend 4, this creature gets +2/+1 until end of turn.
 * (You expend 4 as you spend your fourth total mana to cast spells during a turn.)
 *
 * Note: The "expend" trigger mechanic is not yet implemented in the engine.
 * The trample keyword and base stats are correct. The expend-triggered
 * +2/+1 buff is not modeled.
 */
val JunkbladeBruiser = card("Junkblade Bruiser") {
    manaCost = "{3}{R/G}{R/G}"
    typeLine = "Creature — Raccoon Berserker"
    power = 4
    toughness = 5
    oracleText = "Trample\nWhenever you expend 4, this creature gets +2/+1 until end of turn. (You expend 4 as you spend your fourth total mana to cast spells during a turn.)"

    keywords(Keyword.TRAMPLE)

    // TODO: Expend 4 trigger not yet supported by the engine.
    // When implemented, add:
    // triggeredAbility {
    //     trigger = Triggers.Expend(4)
    //     effect = Effects.ModifyStats(2, 1, EffectTarget.Self)
    // }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "220"
        artist = "Omar Rayyan"
        flavorText = "\"Each scrap shed from her battle-axe is more likely to end up stored permanently in an enemy's skull.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/1/918fd89b-5ab7-4ae2-920c-faca5e9da7b9.jpg?1721427100"

        ruling("2024-07-26", "Abilities that trigger whenever you \"expend N\" only trigger when you reach that specific amount of mana spent on casting spells that turn. This can only happen once per turn.")
    }
}
