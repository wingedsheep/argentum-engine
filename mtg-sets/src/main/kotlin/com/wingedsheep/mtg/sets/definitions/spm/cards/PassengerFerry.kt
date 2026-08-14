package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Passenger Ferry
 * {3}
 * Artifact — Vehicle, 4/3
 *
 * Whenever this Vehicle attacks, you may pay {U}. When you do, another target attacking
 * creature can't be blocked this turn.
 * Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle
 * becomes an artifact creature until end of turn.)
 *
 * The attack trigger is a "When you do" reflexive: the optional {U} payment is the action, and
 * the reflexive ability — which targets another attacking creature, chosen as it goes on the
 * stack — only fires if the payment is made. `.other()` excludes the Vehicle itself so the
 * target is "another" attacking creature.
 */
val PassengerFerry = card("Passenger Ferry") {
    manaCost = "{3}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 3
    oracleText = "Whenever this Vehicle attacks, you may pay {U}. When you do, another target attacking " +
        "creature can't be blocked this turn.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = ReflexiveTriggerEffect(
            // "you may pay {U}"
            action = PayManaCostEffect(ManaCost.parse("{U}")),
            optional = true,
            // "When you do, another target attacking creature can't be blocked this turn."
            reflexiveEffect = GrantKeywordEffect(AbilityFlag.CANT_BE_BLOCKED.name, EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(
                TargetCreature(filter = TargetFilter.AttackingCreature.other())
            )
        )
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Leon Tukker"
        flavorText = "\"Staten Island in peril? Uh, give me an hour?\"\n—Ghost-Spider, Gwen Stacy"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2495f477-b88c-4938-a86a-f72c3c861188.jpg?1783905304"
    }
}
