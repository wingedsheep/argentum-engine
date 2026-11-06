package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Reads a standing damage-prevention shield over its controller's side: "if a source an opponent
 * controls would deal damage to you, prevent 1 of that damage" (Protection of the Hekma), "prevent
 * all damage that would be dealt to you and permanents you control by sources with the chosen name"
 * (Gideon's Intervention).
 *
 * Read by whom the damage would land on, never by card name: every recipient the shield covers must
 * be its controller or a permanent they control. A prevention that also (or only) covers the
 * opponent's side — Fog-shaped "prevent all combat damage", "prevent damage to creatures your
 * opponents control" — shields them as much as us, and is not read as ours. A creature's shield
 * over itself alone ("prevent all damage that would be dealt to this creature") is part of its body,
 * which the combat simulation already sees, and is left out.
 */
internal object DamageShields {
    fun reads(replacement: ReplacementEffect): Boolean {
        val prevention = replacement as? PreventDamage ?: return false
        val event = prevention.appliesTo as? EventPattern.DamageEvent ?: return false
        return oursOnly(event.recipient)
    }

    private fun oursOnly(recipient: Recipient): Boolean = when (recipient) {
        is Recipient.Player -> recipient.player == Player.You
        is Recipient.Object -> recipient.filter.controllerPredicate == ControllerPredicate.ControlledByYou
        is Recipient.AnyOf -> recipient.options.all(::oursOnly)
        Recipient.AnotherPlayer -> false
    }
}
