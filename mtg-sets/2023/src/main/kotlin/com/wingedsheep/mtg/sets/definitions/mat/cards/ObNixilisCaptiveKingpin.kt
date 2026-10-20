package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ob Nixilis, Captive Kingpin — March of the Machine: The Aftermath #41
 * {2}{B}{R} · Legendary Creature — Demon · 4/3
 *
 * Flying, trample
 * Whenever one or more opponents each lose exactly 1 life, put a +1/+1 counter on Ob Nixilis.
 * Exile the top card of your library. Until your next end step, you may play that card.
 *
 * The trigger is a batched `losesLife(exactly = 1)`: each opponent's losses in one simultaneous
 * event are summed, so three 1/1s connecting at once are a loss of 3 and don't trigger it, and it
 * fires once however many opponents lost exactly 1.
 */
val ObNixilisCaptiveKingpin = card("Ob Nixilis, Captive Kingpin") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Demon"
    power = 4
    toughness = 3
    oracleText = "Flying, trample\n" +
        "Whenever one or more opponents each lose exactly 1 life, put a +1/+1 counter on Ob Nixilis. " +
        "Exile the top card of your library. Until your next end step, you may play that card."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.anOpponent.losesLife(exactly = 1)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Patterns.Exile.impulse(expiry = MayPlayExpiry.UntilNextEndStep)
        description = "Whenever one or more opponents each lose exactly 1 life, put a +1/+1 counter on " +
            "Ob Nixilis. Exile the top card of your library. Until your next end step, you may play that card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "41"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddb68233-3683-41bd-9b6e-4f07a1b54244.jpg?1783916511"
        ruling(
            "2023-05-12",
            "Ob Nixilis's last ability will trigger if an opponent is dealt exactly 1 damage, pays exactly " +
                "1 life, or loses exactly 1 life some other way."
        )
        ruling(
            "2023-05-12",
            "Ob Nixilis's last ability looks at all combat damage dealt to an opponent in total to determine " +
                "whether it triggers. For example, if three 1/1 creatures deal combat damage to an opponent, " +
                "that player loses 3 life. Ob Nixilis's ability won't trigger."
        )
    }
}
