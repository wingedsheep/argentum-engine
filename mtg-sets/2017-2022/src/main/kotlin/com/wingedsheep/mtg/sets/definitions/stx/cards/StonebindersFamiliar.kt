package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stonebinder's Familiar — Strixhaven: School of Mages #31 (canonical printing)
 * {W} · Creature — Spirit Dog · 1/1
 *
 * Whenever one or more cards are put into exile during your turn, put a +1/+1 counter on this
 * creature. This ability triggers only once each turn.
 *
 * The exile batch (`Triggers.oneOrMore(filter).putIntoExile(from, includeTokens)`, Ketramose's
 * trigger) widened to "from anywhere" — every zone a card can be exiled from — and left
 * unscoped, so anyone's cards count. "Cards" keeps tokens out (CR 111.6). "During your turn" is
 * the `triggerRestriction`; "triggers only once each turn" is the `oncePerTurn` cap, and one
 * exile event moving several cards is one batch anyway (ruling).
 */
val StonebindersFamiliar = card("Stonebinder's Familiar") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Dog"
    power = 1
    toughness = 1
    oracleText = "Whenever one or more cards are put into exile during your turn, put a +1/+1 " +
        "counter on this creature. This ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any).putIntoExile(
            from = setOf(Zone.LIBRARY, Zone.HAND, Zone.BATTLEFIELD, Zone.GRAVEYARD, Zone.STACK, Zone.COMMAND),
        )
        triggerRestriction = Conditions.IsYourTurn
        oncePerTurn = true
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever one or more cards are put into exile during your turn, put a " +
            "+1/+1 counter on this creature. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Sam Rowan"
        flavorText = "\"Hey, my great-great-grandfather had a dog just like that!\"\n" +
            "—Dradiel, Lorehold mage-student"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21de4a01-98d3-4af3-9c6e-29e16d2c1767.jpg?1783927384"
        ruling("2021-04-16", "If multiple cards are put into exile as part of the same event on your turn, the triggered ability will trigger only once.")
    }
}
