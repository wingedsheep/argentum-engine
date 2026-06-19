package com.wingedsheep.engine.state.components.battlefield

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One value locked into a [ChoiceSlot] as a spell was cast / as a permanent entered. A heterogeneous
 * bag needs a small sealed value type so color / type / mode / creature / number / flag choices can
 * all live in one map.
 */
@Serializable
sealed interface ChoiceValue {
    /** A chosen color (e.g. Riptide Replicator's chosen color). */
    @SerialName("ChoiceValue.Color")
    @Serializable
    data class ColorChoice(val color: Color) : ChoiceValue

    /** A chosen string — creature type, basic land type, or named mode id. */
    @SerialName("ChoiceValue.Text")
    @Serializable
    data class TextChoice(val text: String) : ChoiceValue

    /** A chosen entity (e.g. Dauntless Bodyguard's chosen creature). */
    @SerialName("ChoiceValue.Entity")
    @Serializable
    data class EntityChoice(val entityId: EntityId) : ChoiceValue

    /** A chosen number (e.g. the X declared for a `blight X` additional cost). */
    @SerialName("ChoiceValue.Number")
    @Serializable
    data class NumberChoice(val amount: Int) : ChoiceValue

    /** A boolean choice that is simply present or absent (e.g. "this spell was kicked"). */
    @SerialName("ChoiceValue.Flag")
    @Serializable
    data object Flag : ChoiceValue
}

/**
 * The choices locked in as a spell was cast / as a permanent entered (CR 601.2b), carried durably on
 * the same entity as it resolves onto the battlefield. This is the immutable-ECS analogue of Forge's
 * per-object SVar bag and mtgish's named bindings (`ValueX` / `TheChosenColor`): a permanent can read
 * "the X I was cast with" or "the color I was made with" from a triggered or activated ability long
 * after the spell has left the stack.
 *
 * Phase 2 of the cast-time-choices design (`backlog/cast-time-choices-and-inherited-x.md`) folds the
 * former one-off components (`ChosenColorComponent`, `ChosenCreatureTypeComponent`,
 * `ChosenLandTypeComponent`, `ChosenModeComponent`, `ChosenCreatureComponent`, `WasKickedComponent`)
 * into this single bag, plus the blight additional-cost amount. The reads happen through the
 * accessor helpers below ([chosenColor], [chosenCreatureType], …) and the SDK readers
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastX] /
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastChoice] /
 * [com.wingedsheep.sdk.scripting.conditions.CastChoiceMade] /
 * [com.wingedsheep.sdk.scripting.conditions.CastChoiceIs].
 *
 * Lifecycle (relies on the entity id being stable across the stack→battlefield boundary):
 *  - The cast-determined choices ([x], kicked, blight) are stamped in
 *    `StackResolver.enterPermanentOnBattlefield`; the as-it-enters choices ([chosen] color / type /
 *    mode / …) are merged in by the `EntersWithChoice` continuation resumers. While on the stack the
 *    cast X still lives on `SpellOnStackComponent.xValue`, so `CastX` reads that there.
 *  - Stripped on leaving the battlefield ([com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.stripBattlefieldComponents]):
 *    a card that changes zones is a new object (CR 400.7) and no longer remembers how it was cast.
 *    The cast X is preserved as last-known information on the leave
 *    [com.wingedsheep.engine.core.ZoneChangeEvent] so dies/leaves triggers can still read it.
 *  - Not a copiable value (CR 707.2): a copy of a *permanent* (Clone) never receives it.
 */
@Serializable
data class CastChoicesComponent(
    /** The value chosen for `{X}` as this object was cast. Only present for spells cast with `{X}`. */
    val x: Int? = null,
    /** The per-[ChoiceSlot] values locked in for this object. */
    val chosen: Map<ChoiceSlot, ChoiceValue> = emptyMap()
) : Component {
    /** Return a copy with [slot] set to [value], replacing any prior value for that slot. */
    fun withChoice(slot: ChoiceSlot, value: ChoiceValue): CastChoicesComponent =
        copy(chosen = chosen + (slot to value))
}

// =============================================================================
// Accessor helpers — read the unified bag off an entity's component container.
// These replace the former direct `.get<Chosen*Component>()` / `.has<WasKickedComponent>()` reads.
// =============================================================================

/** The color chosen as this object entered, or null. */
fun ComponentContainer.chosenColor(): Color? =
    (get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.COLOR) as? ChoiceValue.ColorChoice)?.color

/** The creature type chosen as this object entered, or null. */
fun ComponentContainer.chosenCreatureType(): String? =
    (get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.CREATURE_TYPE) as? ChoiceValue.TextChoice)?.text

/** The basic land type chosen as this object entered, or null. */
fun ComponentContainer.chosenLandType(): String? =
    (get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.LAND_TYPE) as? ChoiceValue.TextChoice)?.text

/** The named mode id chosen as this object entered, or null. */
fun ComponentContainer.chosenModeId(): String? =
    (get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.MODE) as? ChoiceValue.TextChoice)?.text

/** The card name chosen as this object entered (e.g. Petrified Hamlet), or null. */
fun ComponentContainer.chosenCardName(): String? =
    (get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.CARD_NAME) as? ChoiceValue.TextChoice)?.text

/** The creature chosen as this object entered, or null. */
fun ComponentContainer.chosenCreatureRef(): EntityId? =
    (get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.CREATURE) as? ChoiceValue.EntityChoice)?.entityId

/** The opponent chosen as this object entered (a player entity id), or null. */
fun ComponentContainer.chosenOpponent(): EntityId? =
    (get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.OPPONENT) as? ChoiceValue.EntityChoice)?.entityId

/** Whether this object was cast kicked. */
fun ComponentContainer.wasKickedChoice(): Boolean =
    get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.KICKED) == true

/** The X declared for a `blight X` additional cost when cast, or null. */
fun ComponentContainer.blightAmountChoice(): Int? =
    (get<CastChoicesComponent>()?.chosen?.get(ChoiceSlot.BLIGHT_AMOUNT) as? ChoiceValue.NumberChoice)?.amount

/** Return a container with [slot] set to [value] in its (possibly new) [CastChoicesComponent]. */
fun ComponentContainer.withCastChoice(slot: ChoiceSlot, value: ChoiceValue): ComponentContainer {
    val bag = get<CastChoicesComponent>() ?: CastChoicesComponent()
    return with(bag.withChoice(slot, value))
}
