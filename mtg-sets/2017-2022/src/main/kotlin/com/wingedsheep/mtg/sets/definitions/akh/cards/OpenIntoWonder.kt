package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Open into Wonder — Amonkhet #64
 * {X}{U}{U} · Sorcery
 *
 * X target creatures can't be blocked this turn. Until end of turn, those creatures gain
 * "Whenever this creature deals combat damage to a player, draw a card."
 *
 * "X target creatures" is exactly X (Part Water / Wave of Indifference shape). [ForEachTarget]
 * applies both grants to each chosen creature on its own (Rabid Attack shape), so the granted
 * draw trigger is scoped to that creature and an illegal target simply misses out.
 */
val OpenIntoWonder = card("Open into Wonder") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "X target creatures can't be blocked this turn. Until end of turn, those creatures " +
        "gain \"Whenever this creature deals combat damage to a player, draw a card.\""

    spell {
        targets(TargetFilter.Creature, exactly = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.ContextTarget(0)),
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                    effect = Effects.DrawCards(1),
                    descriptionOverride = "Whenever this creature deals combat damage to a player, draw a card.",
                ),
                target = EffectTarget.ContextTarget(0),
                duration = Duration.EndOfTurn,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Tyler Jacobson"
        flavorText = "The one thing the Gatewatch didn't expect to find on a plane ruled by Nicol Bolas was perfection."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0df4db1e-f2a7-44e7-9b8d-fecf73d1e01c.jpg?1783936518"
    }
}
