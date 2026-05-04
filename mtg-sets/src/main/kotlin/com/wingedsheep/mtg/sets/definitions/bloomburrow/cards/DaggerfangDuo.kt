package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Daggerfang Duo
 * {2}{B}
 * Creature — Rat Squirrel
 * 3/2
 * Deathtouch
 * When this creature enters, mill two cards.
 */
val DaggerfangDuo = card("Daggerfang Duo") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Rat Squirrel"
    power = 3
    toughness = 2
    oracleText = "Deathtouch\nWhen this creature enters, you may mill two cards."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            effect = EffectPatterns.mill(2),
            descriptionOverride = "You may mill two cards."
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Nereida"
        flavorText = "\"Should we act now or later?\" the rat asked. \"Both,\" whispered the squirrel."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cea2bb34-e328-44fb-918a-72208c9457e4.jpg?1721426379"
    }
}
