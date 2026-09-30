package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Volatile Stormdrake
 * {1}{U}
 * Creature — Drake
 * 3/2
 *
 * - "Hexproof from activated and triggered abilities" is two hexproof abilities (CR 702.11f) over
 *   the source-kind scopes [ProtectionScope.ActivatedAbilities] and
 *   [ProtectionScope.TriggeredAbilities]: opponents' abilities can't target it, their spells can.
 * - "If you do" gates on the exchange actually happening ([SuccessCriterion.ControlChanged]): if
 *   the Stormdrake has left the battlefield the exchange can't be completed (CR 701.12a) and nothing
 *   else happens. The exchange is permanent (ruling).
 * - "Sacrifice that creature unless you pay" is a [Effects.MayPay] of an exact energy amount whose
 *   decline branch sacrifices the stolen creature.
 */
val VolatileStormdrake = card("Volatile Stormdrake") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    power = 3
    toughness = 2
    oracleText = "Flying, hexproof from activated and triggered abilities\n" +
        "When this creature enters, exchange control of this creature and target creature an opponent " +
        "controls. If you do, you get {E}{E}{E}{E}, then sacrifice that creature unless you pay an " +
        "amount of {E} equal to its mana value."

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Hexproof(ProtectionScope.ActivatedAbilities))
    keywordAbility(KeywordAbility.Hexproof(ProtectionScope.TriggeredAbilities))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val theirs = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.IfYouDo(
            action = Effects.ExchangeControl(EffectTarget.Self, theirs),
            then = Effects.GetEnergy(4) then Effects.MayPay(
                cost = Effects.PayExactCounters(CounterType.ENERGY, DynamicAmounts.manaValueOf(theirs)),
                then = Effects.Nothing,
                otherwise = Effects.SacrificeTarget(theirs)
            ),
            successCriterion = SuccessCriterion.ControlChanged
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e6e3232-8bb8-4504-9597-dfdfc6d634bd.jpg?1783911285"
        ruling("2024-06-07", "Volatile Stormdrake can't be the target of any activated or triggered abilities your opponents control.")
        ruling(
            "2024-06-07",
            "The effect of Volatile Stormdrake's last ability lasts indefinitely. It doesn't wear off during " +
                "the cleanup step, and it doesn't expire if Volatile Stormdrake leaves the battlefield."
        )
        ruling(
            "2024-06-07",
            "As Volatile Stormdrake's last ability resolves, Volatile Stormdrake must be on the battlefield " +
                "and the target creature must be a legal target. If either of these things isn't true, the " +
                "ability does nothing."
        )
    }
}
