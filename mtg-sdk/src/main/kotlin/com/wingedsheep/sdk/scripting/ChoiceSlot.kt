package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * The kinds of choice an object can lock in *as it is cast or as it enters* (CR 601.2b) and then
 * carry, durably, on the same entity for the rest of its life — the named slots of the
 * cast-choices bag (`CastChoicesComponent`, the immutable-ECS analogue of Forge's per-object SVar
 * bag and mtgish's named bindings `TheChosenColor` / `TheChosenCreatureType`).
 *
 * A later triggered or activated ability reads the value back generically via
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastChoice] (for numeric slots),
 * [com.wingedsheep.sdk.scripting.conditions.CastChoiceMade] ("was this choice made"), or
 * [com.wingedsheep.sdk.scripting.conditions.CastChoiceIs] ("does the choice equal …").
 *
 * Note `{X}` is **not** a slot — it has its own dedicated reader
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastX]; the slots here are the *other*
 * cast/entry choices folded into the same durable component.
 */
@Serializable
enum class ChoiceSlot {
    /** A color chosen as the object entered (e.g. Riptide Replicator "choose a color"). */
    COLOR,

    /** A creature type chosen as the object entered (e.g. Riptide Replicator "choose a creature type"). */
    CREATURE_TYPE,

    /** A basic land type chosen as the object entered (e.g. Phantasmal Terrain). */
    LAND_TYPE,

    /** A named card-defined mode chosen as the object entered (e.g. the Khans Sieges). */
    MODE,

    /**
     * A card name chosen as the object entered (e.g. Petrified Hamlet "choose a land card name").
     * Stored as a [com.wingedsheep.engine.state.components.battlefield.ChoiceValue.TextChoice].
     * Read back at static-projection / activation-legality time by
     * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.NameEqualsChosenComponent], which
     * keys name-matching off the *source permanent's* durable choice — unlike
     * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.NameEqualsChosen], which reads a
     * transient pipeline variable and fails closed in projection.
     */
    CARD_NAME,

    /** Another creature chosen as the object entered (e.g. Dauntless Bodyguard). */
    CREATURE,

    /** Whether the spell was kicked when cast (e.g. Skizzik). A present value means "kicked". */
    KICKED,

    /**
     * Whether the spell's sneak cost was paid when cast (CR 702.190, e.g. Leonardo, Leader
     * in Blue). A present value means "cast for its sneak cost". Read back through
     * [com.wingedsheep.sdk.scripting.conditions.SneakCostWasPaid].
     */
    SNEAK,

    /** The X declared for a `blight X` additional cost when cast (e.g. Soul Immolation). */
    BLIGHT_AMOUNT,

    /**
     * An opponent chosen as the object entered, stored as a [ChoiceValue.EntityChoice]
     * carrying the player entity id (e.g. Jihad "as this enchantment enters, choose
     * a color and an opponent"). Read back through the
     * [com.wingedsheep.sdk.scripting.references.Player.ChosenOpponent] reference.
     */
    OPPONENT,
}
