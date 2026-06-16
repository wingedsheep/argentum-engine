package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kraum, Violent Cacophony
 * {2}{U}{R}
 * Legendary Creature — Zombie Horror
 * 2/3
 *
 * Flying
 * Whenever you cast your second spell each turn, put a +1/+1 counter on Kraum and draw a card.
 *
 * The trigger uses the per-player Nth-spell tracker scoped to [Player.You] (it watches *your*
 * spell count, not any player's). Only the second spell each turn fires it.
 */
val KraumViolentCacophony = card("Kraum, Violent Cacophony") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Zombie Horror"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever you cast your second spell each turn, put a +1/+1 counter on Kraum and draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.NthSpellCast(2, Player.You)
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Artur Nakhodkin"
        flavorText = "Concerned for his apprentice's welfare, Ludevic sent his greatest creation " +
            "to make sure Geralf got into the right kind of trouble."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/958a3e6b-7e20-40ea-8b2c-7c728934b5e5.jpg?1712356134"
    }
}
