package com.wingedsheep.engine.event

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * The Ring emblem's cumulative triggered abilities (CR 701.54c).
 *
 * The emblem is a per-player object, but its triggered abilities reference "your Ring-bearer", so
 * they are modeled as SELF-bound triggered abilities living on the Ring-bearer creature.
 * [TriggerAbilityResolver] appends the subset unlocked by the bearer's owner's tempt count, so
 * they automatically follow the current bearer and grow with the tempt count without any rebuild.
 *
 * Stable [AbilityId]s keep the abilities equal across projections (important for de-duplication and
 * for the trigger system, which keys pending triggers by ability identity).
 */
object TheRingAbilities {

    /** Tempted 2+ times: "Whenever your Ring-bearer attacks, draw a card, then discard a card." */
    val attackLoot: TriggeredAbility = TriggeredAbility(
        id = AbilityId("the_ring_attack_loot"),
        trigger = Triggers.self.attacks().event,
        binding = TriggerBinding.SELF,
        effect = Effects.DrawCards(1) then Effects.Discard(1, EffectTarget.Controller),
        descriptionOverride = "Whenever your Ring-bearer attacks, draw a card, then discard a card."
    )

    /**
     * Tempted 3+ times: "Whenever your Ring-bearer becomes blocked by a creature, the blocking
     * creature's controller sacrifices it at end of combat."
     *
     * The creature filter makes this a per-blocker trigger (CR fires once for each creature that
     * blocks it), exposing each blocker as [EffectTarget.TriggeringEntity]. The delayed trigger
     * bakes that blocker into a concrete entity and sacrifices it during the End of Combat step.
     */
    val blockedSacrifice: TriggeredAbility = TriggeredAbility(
        id = AbilityId("the_ring_blocked_sacrifice"),
        trigger = Triggers.self.becomesBlocked(by = Filters.Creature).event,
        binding = TriggerBinding.SELF,
        effect = CreateDelayedTriggerEffect(
            step = Step.END_COMBAT,
            effect = SacrificeTargetEffect(EffectTarget.TriggeringEntity, sacrificedByItsController = true)
        ),
        descriptionOverride = "Whenever your Ring-bearer becomes blocked by a creature, the blocking creature's controller sacrifices it at end of combat."
    )

    /** Tempted 4+ times: "Whenever your Ring-bearer deals combat damage to a player, each opponent loses 3 life." */
    val combatDamageDrain: TriggeredAbility = TriggeredAbility(
        id = AbilityId("the_ring_combat_damage"),
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer).event,
        binding = TriggerBinding.SELF,
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent)),
        descriptionOverride = "Whenever your Ring-bearer deals combat damage to a player, each opponent loses 3 life."
    )

    /** The triggered abilities a Ring-bearer has, given how many times its owner has been tempted. */
    fun abilitiesFor(temptCount: Int): List<TriggeredAbility> = buildList {
        if (temptCount >= 2) add(attackLoot)
        if (temptCount >= 3) add(blockedSacrifice)
        if (temptCount >= 4) add(combatDamageDrain)
    }
}
