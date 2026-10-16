package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * The zone an object was in immediately before it was put into exile.
 *
 * Stamped by [com.wingedsheep.engine.handlers.effects.ZoneTransitionService] on every entry into the
 * exile zone that goes through it — which is every *effect-driven* exile — and cleared again by the
 * same service when the object leaves. An object exiled from exile (CR 406.7) is re-stamped with
 * `EXILE`.
 *
 * **It is not a guarantee.** `ZoneTransitionService` is the main road, not a choke point: a handful
 * of sites put a card into exile with a bare `addToZone`. Those that could matter — exile as a cost
 * (`CostPaymentService.exileSelected`, `CastSpellHandler`'s three additional-cost exile branches),
 * `ExileOpponentsGraveyardsExecutor`, and the graveyard sweep in `SbaZoneMovementHelper` — write the
 * same stamp explicitly, and the `CastSpellHandler` one is the only bypass that can populate a
 * *linked*-exile pile. What remains unstamped is deliberate or inert:
 *  - `StackResolver`'s spell-to-exile moves record nothing, but a stack origin takes the fallback
 *    anyway (see [com.wingedsheep.sdk.scripting.effects.CardDestination.ToZoneExiledFrom]), so the
 *    outcome is identical either way;
 *  - `PreparationLogic` creates its entity directly in exile — there is no previous zone;
 *  - `ScenarioBuilderService` places cards in exile as test/scenario setup.
 *
 * So a reader must treat an *absent* component as "origin unknown", not as "not exiled" — which is
 * exactly what `ToZoneExiledFrom`'s `fallback` is for. Anyone adding a new direct-`addToZone` exile
 * should write the stamp alongside it.
 *
 * Likewise the clear is best-effort: `ZoneTransitionService`, `StackResolver`'s cast-from-exile path
 * and `ReturnOneFromLinkedExileExecutor` all drop it (the last two matter because they reuse the
 * entity id, so a leftover stamp would ride onto a battlefield permanent), but the component is only
 * *meaningful* in conjunction with the object actually sitting in an exile zone. Don't read it as a
 * proxy for "is in exile".
 *
 * It exists so an exile-until clause can honour CR 610.3 — "this second one-shot effect returns the
 * object to its previous zone" — when the exile half could reach more than one zone. Read by
 * [com.wingedsheep.sdk.scripting.effects.CardDestination.ToZoneExiledFrom] in
 * `MoveCollectionExecutor`.
 *
 * Deliberately *not* stored on the exiling permanent's `LinkedExileComponent`: the origin zone is a
 * fact about the exiled object, and several exile paths (a group exile, a redirect-to-exile
 * replacement, a Tawnos's Coffin blink) would each have to remember to thread it through if it
 * lived on the linking permanent. Recorded on the object itself, the shared zone-transition path
 * covers all of them at once.
 *
 * Not last-known information: the object still exists, in exile, when this is read. It carries no
 * memory of the *object* across the zone change, only of where it came from — CR 400.7 still makes
 * the returned card a new object.
 *
 * [exiledBy] is the second fact the same stamp records: the player who controlled the spell or
 * ability that exiled the object, for "if it was exiled by an ability you controlled" (Evelyn, the
 * Covetous — read by `StatePredicate.ExiledByYou`). Only the effect-driven exile roads that know
 * their controller pass it (`MoveCollectionExecutor`, `MoveToZoneEffectExecutor`, via
 * `ZoneEntryOptions.exiledBy`); a null means "not attributable" and matches nobody.
 */
@Serializable
data class ExiledFromZoneComponent(
    val zone: Zone,
    val exiledBy: EntityId? = null
) : Component
