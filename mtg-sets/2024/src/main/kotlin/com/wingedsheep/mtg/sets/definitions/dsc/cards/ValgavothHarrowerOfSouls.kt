package com.wingedsheep.mtg.sets.definitions.dsc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Valgavoth, Harrower of Souls {2}{B}{R} — Legendary Creature — Elder Demon 4/4
 *
 * Flying
 * Ward—Pay 2 life.
 * Whenever an opponent loses life for the first time during each of their turns, put a +1/+1
 * counter on Valgavoth and draw a card.
 *
 * "For the first time" is the opponent's turn history (`losesLife(firstTimeEachTurn = true)`), so a
 * loss earlier that turn — even before Valgavoth entered — means no trigger (the card's ruling);
 * "during each of their turns" is a trigger-time restriction on the losing (triggering) player.
 */
val ValgavothHarrowerOfSouls = card("Valgavoth, Harrower of Souls") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Elder Demon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Ward—Pay 2 life.\n" +
        "Whenever an opponent loses life for the first time during each of their turns, put a +1/+1 counter on Valgavoth and draw a card."

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Ward(WardCost.Life(2)))

    triggeredAbility {
        trigger = Triggers.anOpponent.losesLife(firstTimeEachTurn = true)
        triggerRestriction = Conditions.IsPlayersTurn(Player.TriggeringPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.DrawCards(1)
        description = "Whenever an opponent loses life for the first time during each of their turns, " +
            "put a +1/+1 counter on Valgavoth and draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d7c1f6c-af45-4449-8cf8-e13830b3df8a.jpg?1783909662"
        ruling("2024-09-20", "If Valgavoth somehow enters the battlefield during an opponent's turn and that opponent had lost life earlier in the turn, its last ability won't trigger if that opponent loses life later in the turn.")
    }
}
