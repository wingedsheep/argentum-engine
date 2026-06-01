package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nimrodel Watcher
 * {1}{U}
 * Creature — Elf Scout
 * 2/1
 *
 * Whenever you scry, this creature gets +1/+0 until end of turn and can't be
 * blocked this turn. This ability triggers only once each turn.
 */
val NimrodelWatcher = card("Nimrodel Watcher") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elf Scout"
    power = 2
    toughness = 1
    oracleText = "Whenever you scry, this creature gets +1/+0 until end of turn " +
        "and can't be blocked this turn. This ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.WheneverYouScry
        oncePerTurn = true
        effect = CompositeEffect(listOf(
            Effects.ModifyStats(power = 1, toughness = 0, target = EffectTarget.Self),
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Justyna Dura"
        flavorText = "\"But there are some of us still who go abroad for the gathering of news and the watching of our enemies.\"\n—Haldir"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c253ee40-41c9-4cb5-a1a3-94b0aaed09d4.jpg?1686968223"
    }
}
