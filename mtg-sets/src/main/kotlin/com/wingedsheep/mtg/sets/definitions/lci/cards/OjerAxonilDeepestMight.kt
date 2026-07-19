package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SetMinimumDamage
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ojer Axonil, Deepest Might // Temple of Power (The Lost Caverns of Ixalan)
 * {2}{R}{R}
 * Legendary Creature — God // Land
 *
 * Front — Ojer Axonil, Deepest Might (4/4, Trample)
 *   If a red source you control would deal an amount of noncombat damage less than Ojer Axonil's
 *   power to an opponent, that source deals damage equal to Ojer Axonil's power instead.
 *   When Ojer Axonil dies, return it to the battlefield tapped and transformed under its owner's
 *   control.
 *
 * Back — Temple of Power (Land)
 *   {T}: Add {R}.
 *   {2}{R}, {T}: Transform this land. Activate only if red sources you controlled dealt 4 or more
 *   noncombat damage this turn and only as a sorcery.
 *
 * Implementation:
 *  - The damage floor is [SetMinimumDamage] with `dynamicMinimum = DynamicAmounts.sourcePower()`
 *    (evaluated against the replacement's source — Ojer Axonil), gated to noncombat damage from a
 *    red source you control to an opponent player. It raises the would-be amount up to Axonil's
 *    power; larger amounts are untouched.
 *  - Dies-return uses the shared [Effects.ReturnSelfFromGraveyardTransformed]`(tapped = true)`
 *    wired to [Triggers.Dies].
 *  - Back land: `{T}: Add {R}` mana ability + a `{2}{R}, {T}` sorcery-speed [TransformEffect]
 *    gated on [Conditions.YouDealtRedNoncombatDamageThisTurn]`(4)` (backed by the per-player
 *    RED_NONCOMBAT_DAMAGE_DEALT tracker).
 */

private val OjerAxonilDeepestMightFront = card("Ojer Axonil, Deepest Might") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — God"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "If a red source you control would deal an amount of noncombat damage less than Ojer " +
        "Axonil's power to an opponent, that source deals damage equal to Ojer Axonil's power " +
        "instead.\n" +
        "When Ojer Axonil dies, return it to the battlefield tapped and transformed under its " +
        "owner's control."

    keywords(Keyword.TRAMPLE)

    replacementEffect(
        SetMinimumDamage(
            dynamicMinimum = DynamicAmounts.sourcePower(),
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.Opponent,
                source = SourceFilter.Matching(GameObjectFilter.Any.withColor(Color.RED).youControl()),
                damageType = DamageType.NonCombat,
            ),
        )
    )

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.ReturnSelfFromGraveyardTransformed(tapped = true)
        description = "When Ojer Axonil dies, return it to the battlefield tapped and transformed " +
            "under its owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "158"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50f8e2b6-98c7-4f28-bb39-e1fbe841f1ee.jpg?1782694483"
    }
}

private val TempleOfPower = card("Temple of Power") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Land"
    oracleText = "(Transforms from Ojer Axonil, Deepest Might.)\n" +
        "{T}: Add {R}.\n" +
        "{2}{R}, {T}: Transform this land. Activate only if red sources you controlled dealt 4 " +
        "or more noncombat damage this turn and only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap)
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouDealtRedNoncombatDamageThisTurn(4)
            )
        )
        description = "Transform this land. Activate only if red sources you controlled dealt 4 " +
            "or more noncombat damage this turn and only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "158"
        artist = "Victor Adame Minguez"
        flavorText = "Chimil gave the Oltec passion. Ojer Axonil challenged them to harness it."
        imageUri = "https://cards.scryfall.io/normal/back/5/0/50f8e2b6-98c7-4f28-bb39-e1fbe841f1ee.jpg?1782694483"
    }
}

val OjerAxonilDeepestMight: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = OjerAxonilDeepestMightFront,
    backFace = TempleOfPower,
)
