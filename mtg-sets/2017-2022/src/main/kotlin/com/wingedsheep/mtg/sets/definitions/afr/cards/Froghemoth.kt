package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Froghemoth
 * {3}{G}{G}
 * Creature — Frog Horror
 * 4/4
 * Trample, haste
 * Whenever this creature deals combat damage to a player, exile up to that many target cards from
 * their graveyard. Put a +1/+1 counter on this creature for each creature card exiled this way. You
 * gain 1 life for each noncreature card exiled this way.
 *
 * "That many" caps the targets at the combat damage dealt, locked in as the trigger goes on the
 * stack (`dynamicMaxCount = triggerDamageAmount()`); "their graveyard" scopes the targets to the
 * damaged player's graveyard (`ownedByTriggeringPlayer()`). "Exiled this way" reads the pile that
 * actually moved, so a target that left the graveyard before resolution counts for nothing.
 */
val Froghemoth = card("Froghemoth") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Frog Horror"
    power = 4
    toughness = 4
    oracleText = "Trample, haste\n" +
        "Whenever this creature deals combat damage to a player, exile up to that many target cards from " +
        "their graveyard. Put a +1/+1 counter on this creature for each creature card exiled this way. " +
        "You gain 1 life for each noncreature card exiled this way."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        targets(
            TargetFilter.CardInGraveyard.ownedByTriggeringPlayer(),
            optional = true,
            dynamicMaxCount = DynamicAmounts.triggerDamageAmount(),
        )
        effect = Effects.Pipeline {
            val exiled = moveTracked(gather(CardSource.ChosenTargets), CardDestination.ToZone(Zone.EXILE))
            val creatures = filter(exiled, GameObjectFilter.Creature)
            val noncreatures = filter(exiled, GameObjectFilter.Noncreature)
            run(
                Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, creatures.count, EffectTarget.Self) then
                    Effects.GainLife(noncreatures.count)
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Brent Hollowell"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/728e9567-2585-4c03-99a5-3e84fd8044d2.jpg?1783926463"
    }
}
