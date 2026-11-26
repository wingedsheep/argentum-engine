package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mark of Mutiny — Zendikar #137 (canonical printing)
 * {2}{R} · Sorcery
 *
 * Gain control of target creature until end of turn. Put a +1/+1 counter on it and untap it.
 * That creature gains haste until end of turn.
 */
val MarkOfMutiny = card("Mark of Mutiny") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Put a +1/+1 counter on it and untap it. " +
        "That creature gains haste until end of turn. (It can attack and {T} this turn.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Mike Bierek"
        flavorText = "The flame of anger is hard to douse once lit."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58a0a019-239d-428e-85a2-e19cae8f4b58.jpg?1783942142"
        ruling("2017-11-17", "Mark of Mutiny can target any creature, even one that you already control or that is already untapped.")
        ruling("2017-11-17", "The +1/+1 counter remains on the creature after the control-changing effect ends.")
    }
}
