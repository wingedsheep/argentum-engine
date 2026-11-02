package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ral's Staticaster — Guilds of Ravnica #268 (planeswalker deck)
 * {2}{U}{R} · Creature — Lizard Wizard · 3/3
 *
 * Trample
 * Whenever this creature attacks, if you control a Ral planeswalker, this creature gets +1/+0 for
 * each card in your hand until end of turn.
 *
 * The intervening-if is checked on trigger and on resolution; the hand count is read once as the
 * ability resolves and the bonus stays locked in for the turn (rulings).
 */
val RalsStaticaster = card("Ral's Staticaster") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Lizard Wizard"
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "Whenever this creature attacks, if you control a Ral planeswalker, this creature gets +1/+0 for each " +
        "card in your hand until end of turn."
    power = 3
    toughness = 3

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Ral"))
        effect = Effects.ModifyStats(
            DynamicAmounts.cardsInYourHand(),
            DynamicAmounts.fixed(0),
            EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "268"
        inBooster = false
        artist = "Even Amundsen"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adfa16dc-03b3-4533-884c-a23b992b8d86.jpg?1783934095"
        ruling("2018-10-05", "The number of cards in your hand is checked only as the triggered ability of Ral’s Staticaster resolves. If that number changes later in the turn, its power is unaffected.")
        ruling("2018-10-05", "Whether you control a Ral planeswalker is checked as the ability triggers and as it resolves. After it has resolved while you controlled a Ral planeswalker, Ral’s Staticaster keeps the bonus for the rest of the turn even if you lose your planeswalker.")
    }
}
