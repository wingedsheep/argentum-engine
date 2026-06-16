package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Razzle-Dazzler
 * {1}{U}
 * Creature — Human Wizard
 * 1/2
 *
 * Whenever you cast your second spell each turn, put a +1/+1 counter on this creature.
 * It can't be blocked this turn.
 */
val RazzleDazzler = card("Razzle-Dazzler") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "Whenever you cast your second spell each turn, put a +1/+1 counter on this creature. " +
        "It can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.NthSpellCast(2, Player.You)
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Wayne Wu"
        flavorText = "\"Now you see me, now you—\""
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7d08008-9272-405e-82ef-566e6d42bb17.jpg?1712355485"
    }
}
