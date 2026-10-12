package com.wingedsheep.engine.event

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The sourceless inherent radiation trigger (CR 728.1), controlled by the active player.
 * Player entity identity follows MonarchAbilities; this is an ordinary, respondable trigger.
 * Its intervening if is checked at detection and resolution, and the mill amount is read on resolution.
 * Public-zone filtering implements CR 701.17c, including graveyard-to-exile replacements.
 */
object RadiationAbilities {
    const val SOURCE_NAME = "Radiation"
    val ID = AbilityId("inherent_radiation_precombat_main")
    val ability = TriggeredAbility(
        id = ID,
        trigger = EventPattern.StepEvent(Step.PRECOMBAT_MAIN, Player.You),
        binding = TriggerBinding.ANY,
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.playerCounterCount(CounterType.RAD, Player.TriggeringPlayer), ComparisonOperator.GTE, 1
        ),
        effect = Effects.Pipeline {
            val top = gather(
                CardSource.TopOfLibrary(DynamicAmounts.playerCounterCount(CounterType.RAD, Player.TriggeringPlayer), player = Player.TriggeringPlayer, isMill = true),
                lookAudience = LookAudience.None
            )
            val moved = moveTracked(top, CardDestination.ToZone(Zone.GRAVEYARD, Player.TriggeringPlayer))
            // A replacement can move milled cards elsewhere. Hidden-zone cards cannot be found.
            val publicCards = filter(moved,
                GameObjectFilter.Any.withStatePredicate(StatePredicate.InZone(Zone.GRAVEYARD)) or
                    GameObjectFilter.Any.withStatePredicate(StatePredicate.InZone(Zone.EXILE))
                        .withStatePredicate(StatePredicate.Not(StatePredicate.IsFaceDown)) or
                    GameObjectFilter.Any.withStatePredicate(StatePredicate.InZone(Zone.BATTLEFIELD)) or
                    GameObjectFilter.Any.withStatePredicate(StatePredicate.InZone(Zone.COMMAND)) or
                    GameObjectFilter.Any.withStatePredicate(StatePredicate.InZone(Zone.STACK)))
            val nonlands = filter(publicCards, GameObjectFilter.Nonland)
            val count = storeNumber(nonlands.count)
            run(Effects.If(
                Conditions.CompareAmounts(count.amount, ComparisonOperator.GTE, 1),
                Effects.LoseLife(count.amount, EffectTarget.PlayerRef(Player.TriggeringPlayer), LifeLossOrigin.RADIATION) then
                    Effects.RemoveCounters(CounterType.RAD, count.amount, EffectTarget.PlayerRef(Player.TriggeringPlayer))
            ))
        },
        descriptionOverride = "At the beginning of a player's precombat main phase, if they have rad counters, " +
            "they mill that many cards. For each nonland card milled this way, they lose 1 life and remove a rad counter."
    )
}
