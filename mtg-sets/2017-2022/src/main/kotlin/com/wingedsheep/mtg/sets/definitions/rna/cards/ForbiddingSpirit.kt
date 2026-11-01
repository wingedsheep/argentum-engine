package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Forbidding Spirit — Ravnica Allegiance #9
 * {1}{W}{W} · Creature — Spirit Cleric · 3 / 3
 *
 * - Summon: Yojimbo's player-anchored tax: the enters trigger grants *you* a planeswalker-covering
 *   [AttackTax] of {2} until your next turn, so it keeps applying after the Spirit leaves.
 */
val ForbiddingSpirit = card("Forbidding Spirit") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Cleric"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, until your next turn, creatures can't attack you or " +
        "planeswalkers you control unless their controller pays {2} for each of those creatures."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GrantStaticAbility(
            ability = AttackTax(DynamicAmounts.fixed(2), coversPlaneswalkers = true),
            target = EffectTarget.Controller,
            duration = Duration.UntilYourNextTurn,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Ryan Yee"
        flavorText = "\"You will respect the dead.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c17056e7-95c6-4bed-a747-3b40dcda275a.jpg?1783933722"
    }
}
