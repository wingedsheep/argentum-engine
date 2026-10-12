package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Arbalest Engineers
 * {1}{R}{G}
 * Creature — Human Artificer
 * 2/2
 * When this creature enters, choose one —
 * • This creature deals 1 damage to any target.
 * • Put a +1/+1 counter on target creature. It gains trample and haste until end of turn.
 * • Create a tapped Powerstone token.
 *
 * "This creature deals" is the trigger's own source, [Effects.DealDamage]'s default.
 */
val ArbalestEngineers = card("Arbalest Engineers") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, choose one —\n" +
        "• This creature deals 1 damage to any target.\n" +
        "• Put a +1/+1 counter on target creature. It gains trample and haste until end of turn.\n" +
        "• Create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("This creature deals 1 damage to any target.") {
                    val anyTarget = target(Targets.Any)
                    effect = Effects.DealDamage(1, anyTarget)
                },
                mode("Put a +1/+1 counter on target creature. It gains trample and haste until end of turn.") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
                        Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
                        Effects.GrantKeyword(Keyword.HASTE, creature)
                },
                mode("Create a tapped Powerstone token.") {
                    effect = Effects.CreatePowerstone(tapped = true)
                },
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df343bf9-f535-42c7-a2d0-8adfd068b354.jpg?1783920034"
        ruling(
            "2022-10-14",
            "Although all the cards in The Brothers' War that create Powerstone tokens create a tapped " +
                "Powerstone token, entering the battlefield tapped isn't part of the token's definition. " +
                "Notably, if you create a token that is a copy of a Powerstone token, the token copy won't " +
                "enter the battlefield tapped."
        )
    }
}
