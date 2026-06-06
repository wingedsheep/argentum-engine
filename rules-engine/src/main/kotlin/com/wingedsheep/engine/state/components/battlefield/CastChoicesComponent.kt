package com.wingedsheep.engine.state.components.battlefield

import com.wingedsheep.engine.state.Component
import kotlinx.serialization.Serializable

/**
 * The choices locked in as a spell was cast (CR 601.2b), carried durably on the same entity as it
 * resolves onto the battlefield. This is the immutable-ECS analogue of Forge's per-object SVar bag
 * and mtgish's named bindings (`ValueX` / `TheChosenColor`): a permanent can read "the X I was cast
 * with" from a triggered or activated ability long after the spell has left the stack, via
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastX].
 *
 * Lifecycle (relies on the entity id being stable across the stack→battlefield boundary):
 *  - Attached in `StackResolver.enterPermanentOnBattlefield` when the resolving spell had a chosen
 *    `{X}`, in addition to the (now removed) stack-presence marker. While on the stack the cast X
 *    still lives on `SpellOnStackComponent.xValue`, so `CastX` reads that there.
 *  - Stripped on leaving the battlefield ([com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.stripBattlefieldComponents]):
 *    a card that changes zones is a new object (CR 400.7) and no longer remembers how it was cast.
 *    The cast X is preserved as last-known information on the leave
 *    [com.wingedsheep.engine.core.ZoneChangeEvent] so dies/leaves triggers can still read it.
 *  - Not a copiable value (CR 707.2): a copy of a *permanent* (Clone) never receives it.
 *
 * Phase 1 of the cast-time-choices design (`backlog/cast-time-choices-and-inherited-x.md`) records
 * only [x]. The cast-time chosen color / creature type / mode / kicked-ness still live on their own
 * components; folding them into this bag is Phase 2.
 */
@Serializable
data class CastChoicesComponent(
    /** The value chosen for `{X}` as this object was cast. Only present for spells cast with `{X}`. */
    val x: Int
) : Component
