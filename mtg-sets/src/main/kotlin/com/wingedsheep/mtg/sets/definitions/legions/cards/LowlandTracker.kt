package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lowland Tracker
 * {4}{W}
 * Creature — Human Soldier
 * 2/2
 * First strike
 * Provoke (Whenever this creature attacks, you may have target creature defending player
 * controls untap and block it if able.)
 */
val LowlandTracker = card("Lowland Tracker") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "First strike\nProvoke (Whenever this creature attacks, you may have target creature defending player controls untap and block it if able.)"

    keywords(Keyword.FIRST_STRIKE, Keyword.PROVOKE)

    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        target = Targets.CreatureOpponentControls
        effect = Effects.Provoke(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Brian Snõddy"
        flavorText = "\"I feed my hatred to the righteous and they join my crusade.\"\n—Akroma, angelic avenger"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8eaaded-b18a-4614-b5b5-b4bb49a2e1b1.jpg?1562945177"
    }
}
