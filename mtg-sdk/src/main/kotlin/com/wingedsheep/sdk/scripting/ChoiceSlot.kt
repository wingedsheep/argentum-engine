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

    /**
     * A card type chosen as the object entered (e.g. Arachne, Psionic Weaver "choose a card type
     * other than creature"). Stored as a
     * [com.wingedsheep.engine.state.components.battlefield.ChoiceValue.TextChoice] holding the
     * card-type name ("Artifact", "Instant", …). Read back at static-projection / cost-calculation
     * time by [com.wingedsheep.sdk.scripting.predicates.CardPredicate.CardTypeEqualsChosenComponent],
     * the card-type analogue of [CARD_NAME]'s
     * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.NameEqualsChosenComponent].
     */
    CARD_TYPE,

    /** Another creature chosen as the object entered (e.g. Dauntless Bodyguard). */
    CREATURE,

    /** Whether the spell was kicked when cast (e.g. Skizzik). A present value means "kicked". */
    KICKED,

    /**
     * Whether the spell's **bargain** additional cost was declared when cast (CR 702.166b, Wilds of
     * Eldraine — "you may sacrifice an artifact, enchantment, or token as you cast this spell"). A
     * present value means the spell was *bargained*. Read back through
     * [com.wingedsheep.sdk.dsl.Conditions.WasBargained].
     *
     * Deliberately distinct from [KICKED] even though both ride the same optional-additional-cost
     * rail ([KeywordAbility.OptionalAdditionalCost.declaredSlot]): CR 702.166c links a card's
     * "if it was bargained" abilities to *its own* bargain ability, so a bargained spell must not
     * read as kicked to unrelated "whenever you cast a kicked spell" payoffs.
     */
    BARGAINED,

    /**
     * Whether the spell's optional **collect evidence** additional cost was declared when cast
     * (CR 701.59c, Murders at Karlov Manor — "as an additional cost to cast this spell, you may
     * collect evidence 6"). A present value means evidence *was* collected. Read back through
     * [com.wingedsheep.sdk.dsl.Conditions.WasEvidenceCollected].
     *
     * Deliberately distinct from [KICKED] and [BARGAINED] even though all three ride the same
     * optional-additional-cost rail ([KeywordAbility.OptionalAdditionalCost.declaredSlot]):
     * CR 701.59c links a card's "if evidence was collected" ability to *its own* collect-evidence
     * ability (CR 607), so a spell cast with evidence collected must not read as kicked to an
     * unrelated "whenever you cast a kicked spell" payoff, and vice versa.
     *
     * Only the *linked* shape stamps this slot. The many unlinked collect-evidence costs — every
     * activated-ability cost, Axebane Ferox's ward, and the resolution-time
     * [com.wingedsheep.sdk.scripting.effects.CollectEvidenceEffect] — pay the same
     * [com.wingedsheep.sdk.scripting.costs.CostAtom.CollectEvidence] atom without recording
     * anything here, because nothing on those cards asks the question.
     */
    EVIDENCE_COLLECTED,

    /**
     * Whether the spell was cast **using teamwork** (CR 702.194b, Marvel Super Heroes — "as an
     * additional cost to cast this spell, you may tap any number of creatures you control with
     * total power N or more"). A present value means the teamwork cost was declared and paid.
     * Read back through [com.wingedsheep.sdk.dsl.Conditions.TeamworkWasPaid].
     *
     * Deliberately distinct from [KICKED] and [BARGAINED] even though all three ride the same
     * optional-additional-cost rail ([KeywordAbility.OptionalAdditionalCost.declaredSlot]):
     * CR 702.194b scopes "cast using teamwork" to the spell's *own* teamwork ability, so a
     * teamwork spell must not read as kicked to unrelated "whenever you cast a kicked spell"
     * payoffs.
     */
    TEAMWORK,

    /**
     * Whether the spell's sneak cost was paid when cast (CR 702.190, e.g. Leonardo, Leader
     * in Blue). A present value means "cast for its sneak cost". Read back through
     * [com.wingedsheep.sdk.scripting.conditions.SneakCostWasPaid].
     */
    SNEAK,

    /**
     * Whether the spell's web-slinging cost was paid when cast (CR 702.188, Marvel's Spider-Man —
     * e.g. Spiders-Man, Heroic Horde). A present value means "cast for its web-slinging cost". Read
     * back through [com.wingedsheep.sdk.scripting.conditions.WebSlungCostWasPaid]. Pairs with
     * [WEB_SLUNG_RETURNED_MV], which carries the returned creature's mana value. Deliberately
     * distinct from [SNEAK] even though both are "alt cost + return a creature you control": CR
     * 702.188 links a card's "if it was cast using web-slinging" abilities to its own web-slinging
     * ability, so a web-slung spell must not read as sneaked to unrelated payoffs.
     */
    WEB_SLUNG,

    /**
     * The mana value of the creature returned to pay a web-slinging cost (CR 702.188 / 118.9c —
     * the returned creature's own mana value, not the spell's). Stored as a
     * [com.wingedsheep.engine.state.components.battlefield.ChoiceValue.NumberChoice] and read back
     * through [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastChoice] — e.g. Scarlet Spider,
     * Ben Reilly enters with this many +1/+1 counters. Present only when [WEB_SLUNG] is.
     */
    WEB_SLUNG_RETURNED_MV,

    /**
     * Whether the spell's Mayhem cost was paid when cast (CR 702.187, Marvel's Spider-Man — e.g.
     * Sandman's Quicksand). A present value means "cast from the graveyard for its Mayhem cost".
     * Read back through [com.wingedsheep.sdk.scripting.conditions.MayhemCostWasPaid]. Distinct from
     * the other graveyard-cast keywords: CR 702.187 links a card's "if this spell's mayhem cost was
     * paid" abilities to its own Mayhem ability.
     */
    MAYHEM_CAST,

    /** The X declared for a `blight X` additional cost when cast (e.g. Soul Immolation). */
    BLIGHT_AMOUNT,

    /**
     * Whether the spell's optional **waterbend** additional cost was paid when cast (Avatar: The
     * Last Airbender, e.g. Ruinous Waterbending / Spirit Water Revival). A present value means
     * "you may waterbend {N}" was paid. Read back through
     * [com.wingedsheep.sdk.scripting.conditions.WaterbendWasPaid]. Pairs with
     * [com.wingedsheep.sdk.scripting.SpellWaterbendCost] (`optional = true`).
     */
    WATERBEND_PAID,

    /**
     * Whether the spell's **gift** additional cost was paid when cast (CR 702.174a, Bloomburrow —
     * "as an additional cost to cast this spell, you may choose an opponent"). A present value
     * means the gift was promised; the promised opponent rides along in [OPPONENT]. Read back
     * through [com.wingedsheep.sdk.dsl.Conditions.GiftWasPromised] (and its negation for the
     * "if the gift wasn't promised" riders). Pairs with
     * [com.wingedsheep.sdk.scripting.KeywordAbility.Gift].
     */
    GIFT_PROMISED,

    /**
     * An opponent chosen as the object entered, stored as a [ChoiceValue.EntityChoice]
     * carrying the player entity id (e.g. Jihad "as this enchantment enters, choose
     * a color and an opponent"). Read back through the
     * [com.wingedsheep.sdk.scripting.references.Player.ChosenOpponent] reference.
     */
    OPPONENT,

    /**
     * A general-purpose chosen number stored durably on the permanent and re-settable over its
     * lifetime (e.g. Shapeshifter "as this enters and at each upkeep, choose a number between 0
     * and 7; its power is that number and its toughness is 7 minus it"). Written at entry by the
     * [EntersWithChoice]`(ChoiceType.NUMBER)` replacement (CR 614.1c — before the permanent is on
     * the battlefield) and re-chosen later by an upkeep trigger running
     * [com.wingedsheep.sdk.scripting.effects.ChooseNumberForSourceEffect]; both write this slot.
     * Read back through
     * [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastChoice], typically feeding a
     * [com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic] CDA. Distinct from
     * [BLIGHT_AMOUNT] (a cast-time additional-cost X) — this is a free-standing repeatable choice
     * that is not tied to casting.
     */
    CHOSEN_NUMBER,
}
