package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Witchstalker
 * {1}{G}{G}
 * Creature — Wolf
 * 3/3
 * Hexproof
 * Whenever an opponent casts a blue or black spell during your turn, put a +1/+1 counter on this
 * creature.
 *
 * "During your turn" restricts the trigger event (checked when the spell is cast), not an
 * intervening "if" — hence [triggerRestriction], as on Eyes of the Wisent.
 */
val Witchstalker = card("Witchstalker") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    power = 3
    toughness = 3
    oracleText = "Hexproof (This creature can't be the target of spells or abilities your opponents control.)\n" +
        "Whenever an opponent casts a blue or black spell during your turn, put a +1/+1 counter on this creature."

    keywords(Keyword.HEXPROOF)

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Any.withAnyColor(Color.BLUE, Color.BLACK))
        triggerRestriction = Conditions.IsYourTurn
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a5ce47d-ea4f-4e15-adb6-5bb66981ed24.jpg?1783939897"
        ruling("2013-07-01", "The counter will be put on Witchstalker before the spell cast by an opponent resolves.")
        ruling(
            "2013-07-01",
            "If an opponent casts a copy of a card that's blue or black during your turn (as opposed to putting a " +
                "copy of such a spell directly on the stack), the ability will also trigger. Check for the word " +
                "\"cast\" in the effect that creates the copy.",
        )
    }
}
