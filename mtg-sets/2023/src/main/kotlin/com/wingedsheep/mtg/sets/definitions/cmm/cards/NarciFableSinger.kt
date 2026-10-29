package com.wingedsheep.mtg.sets.definitions.cmm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Narci, Fable Singer
 * {1}{W}{B}{G}
 * Legendary Creature — Human Bard
 * 3/3
 *
 * Lifelink
 * Whenever you sacrifice an enchantment, draw a card.
 * Whenever the final chapter ability of a Saga you control resolves, each opponent loses X life
 * and you gain X life, where X is that Saga's mana value.
 *
 * "That Saga" is the triggering entity of `Triggers.you.sagaChapterResolves(finalOnly = true)`;
 * its mana value is read through the Saga's battlefield snapshot once CR 714.4 has sacrificed it
 * (that sacrifice also draws a card off the first trigger).
 */
val NarciFableSinger = card("Narci, Fable Singer") {
    manaCost = "{1}{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Legendary Creature — Human Bard"
    power = 3
    toughness = 3
    oracleText = "Lifelink\n" +
        "Whenever you sacrifice an enchantment, draw a card.\n" +
        "Whenever the final chapter ability of a Saga you control resolves, each opponent loses X " +
        "life and you gain X life, where X is that Saga's mana value."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Enchantment)
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.you.sagaChapterResolves(finalOnly = true)
        val sagaManaValue = DynamicAmounts.triggeringManaValue()
        effect = Effects.LoseLife(sagaManaValue, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(sagaManaValue, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "710"
        artist = "Miranda Meeks"
        flavorText = "\"All stories eventually come to an end.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a98a2fa9-82d6-4cf7-adb4-65b187cd9cda.jpg?1783915490"
    }
}
