package com.wingedsheep.ai.engine.knowledge

/**
 * What a card is *for*, derived structurally from its [com.wingedsheep.sdk.model.CardScript].
 *
 * This is Phase 6 of `backlog/engine-ai-improvement.md`. It replaces card knowledge that did not
 * scale — 19 hand-written `CardAdvisor`s covering 42 card names across 2 sets of a ~30-set catalog
 * — with a pure function of the card definition that covers every card the engine can load.
 *
 * It is a **prior**, not an evaluation. It says "this is a repeatable tapper"; it does not say how
 * much that is worth on *this* board. Board-dependent value stays in
 * [com.wingedsheep.ai.engine.evaluation.BoardPresence], which reads [staticPriorValue] as its
 * starting point and adds what it can see (see [anthemBonus]).
 *
 * Produced by [CardIntentAnalyzer] and reached through an [IntentCatalog].
 */
data class CardIntent(
    /** Everything the card does that the analyzer recognizes. Empty = uninterpretable. */
    val tags: Set<IntentTag>,
    /** When the card's payoff happens. See [Speed]. */
    val speed: Speed,
    /**
     * The largest creature this card can answer by damage or -N/-N, or null when it answers by
     * destruction/exile (which has no reach limit) or answers nothing.
     *
     * Null is therefore two different claims, and [HoldPolicy] has to tell them apart: "toughness
     * is no defence" and "we could not read this card". The one case where null means neither is a
     * fight, whose reach is the *other* creature's power and so cannot be a property of the card —
     * it carries [IntentTag.FIGHT] to say so.
     */
    val removalReach: Int?,
    /** Cards drawn on a single resolution, or null when the card draws none. */
    val cardsDrawn: Int?,
    /** Whether any recognized effect points at an opponent's stuff rather than our own. */
    val affectsOpponent: Boolean,
    /**
     * Whether the payoff can happen more than once: a non-mana activated ability that doesn't eat
     * its own source, or a triggered ability on a permanent.
     */
    val repeatable: Boolean,
    /**
     * Board value of this card as a permanent, before anything board-dependent.
     *
     * Feeds `BoardPresence.permanentValue`, whose pre-Phase-6 behaviour was a flat `0.5` for every
     * non-creature permanent regardless of text. `0.5` is still the value of a card the analyzer
     * cannot read, so an unrecognized permanent scores exactly what it always did.
     *
     * Calibrated against the cost of *casting* removal, not against sibling targets: Phase 2
     * measured all four non-creature puzzle failures as the AI declining to cast the Disenchant at
     * all. At a typical hand size that decision is worth ~4 points of `CardAdvantage`, and
     * `BoardPresence` carries weight 1.5, so a permanent worth answering has to price above ~2.7
     * here or the AI holds the removal forever. Every number is a hand-set prior; Phase 9's
     * logistic fit is what replaces guesses with fits.
     */
    val staticPriorValue: Double,
    /**
     * Total P+T this card grants to *each* creature its controller has, when it is an anthem
     * ("creatures you control get +1/+1" = 2). Zero for everything else.
     *
     * Separated from [staticPriorValue] because it is the one part of an anthem's worth that is
     * cheaply board-dependent — a lord with an empty board is a very different card from the same
     * lord behind five creatures — and `BoardPresence` already has the creature count in hand.
     */
    val anthemBonus: Int,
    /**
     * Toughness a single resolution of an *expiring* pump grants one creature — Giant Growth is 3.
     * Zero for everything else, including an Aura or a +1/+1 counter, whose bonus does not expire
     * and is therefore not the thing [IntentTag.COMBAT_TRICK] names.
     *
     * Separate from the tag because "is this a trick" and "does this trick beat three damage" are
     * different questions, and [HoldPolicy] needs the second one to decide whether a response
     * window is real. Power is deliberately absent: nothing outside combat cares.
     */
    val pumpToughness: Int = 0,
    /**
     * Whether this card **always** enters the battlefield tapped — the Shivan Oasis clause, not the
     * shock-land one.
     *
     * True only for an unconditional `EntersTapped`. A land that enters tapped *unless* you pay 2
     * life, or *unless* you control two or fewer other lands, has a drawback the AI cannot price
     * without simulating the choice it is about to be offered, and a false "this costs you a turn"
     * is worse than no answer — so those read `false` and the consumer keeps its untyped
     * behaviour. Declining is the call [CardIntentAnalyzer] makes everywhere else it cannot read a
     * card exactly.
     *
     * The one consumer is [com.wingedsheep.ai.engine.evaluation.BoardPresence]'s land-sequencing
     * term, which needs to know that the land still in hand carries a deferred cost. Nothing else
     * in the evaluator tells one land from another.
     */
    val entersTapped: Boolean = false,
    /**
     * Whether this card is a **permanent with flash** — a body you may deploy on either player's
     * turn.
     *
     * [Speed] cannot answer this on its own: it reports `INSTANT` for a Lightning Bolt and for a
     * Restoration Angel alike, and the whole of [AmbushWindow]'s argument turns on the difference.
     * An instant is spent when it resolves and its window is about what it is *answering*; a flash
     * permanent stays on the battlefield afterwards, so holding it costs the board nothing and buys
     * the option to ambush.
     *
     * Structural like [entersTapped] rather than an [IntentTag], for the reason that enum's own doc
     * gives: a tag is something the card *does*, and this is a property of when it may be cast.
     */
    val flashPermanent: Boolean = false,
    /**
     * Whether the card is **printed** with haste — the one reason a [flashPermanent] is worth
     * deploying on our own turn rather than held for the ambush window.
     *
     * Printed, not effective. A creature that would be granted haste by something already on the
     * battlefield (a Lightning Mauler pairing, an Anger in the graveyard) reads `false` here, and
     * [AmbushWindow] will hold it a turn it could have attacked. That is the conservative direction
     * and it is the one this file takes everywhere else it cannot read a card exactly: the
     * alternative is asking what keywords a card *would* have on resolution, which nothing short of
     * simulating the cast can answer.
     */
    val hasHaste: Boolean = false,
    /**
     * Whether every permanent this card targets is one **we control** — and there is at least one.
     *
     * This exists because [IntentTag.REMOVAL] cannot answer the question. `hitsAnotherPermanent`
     * decides "does this take *someone else's* permanent off the battlefield" from the
     * [com.wingedsheep.sdk.scripting.effects.EffectTarget] alone, and a bound or context target
     * carries no filter — so it falls through to `else -> true` and every "exile a creature **you
     * control**" reads as spot removal. Restoration Angel is tagged `REMOVAL, EXILE_REMOVAL` for
     * blinking our own creature.
     *
     * That fail-open is harmless where the tag is only a prior, and wrong where a consumer needs to
     * know which side of the table an effect points at. [AmbushWindow] is the second kind: "this ETB
     * clears a blocker before we attack" and "this ETB blinks our own creature" are opposite
     * answers, and the tag gives the same one for both.
     *
     * Fixing `hitsAnotherPermanent` to resolve bound targets against their requirement is the real
     * repair, and it is deliberately *not* done here: `REMOVAL` feeds `staticPriorValue`'s ladder
     * and [RemovalPatience]'s bar, so re-tagging every "target creature you control" card is an
     * evaluation change that needs its own flag and its own arena run. This field is additive, read
     * by exactly one consumer, and changes nothing for anyone else.
     *
     * `false` when the card targets no permanent at all, which keeps a vanilla body — the case the
     * ambush policy most wants — from being mistaken for one that aims at us.
     */
    val targetsOnlyOurPermanents: Boolean = false,
) {
    operator fun contains(tag: IntentTag): Boolean = tag in tags

    companion object {
        /**
         * What a card the analyzer cannot read looks like. `staticPriorValue` is the historical
         * flat `0.5`, so falling back to this changes nothing about how the AI values the board.
         */
        val UNKNOWN = CardIntent(
            tags = emptySet(),
            speed = Speed.SORCERY,
            removalReach = null,
            cardsDrawn = null,
            affectsOpponent = false,
            repeatable = false,
            staticPriorValue = 0.5,
            anthemBonus = 0,
        )
    }
}

/**
 * A recognized thing a card does.
 *
 * Tags are structural, not evaluative — `REMOVAL` means "this destroys/exiles/bounces a permanent",
 * with no claim about whether doing so is good right now. Consumers combine tags with board state.
 */
enum class IntentTag {
    /** Destroys, bounces or otherwise removes a single permanent. */
    REMOVAL,

    /** Removal that exiles — strictly better than destruction, and answers indestructible. */
    EXILE_REMOVAL,

    /** Removal aimed at a whole group rather than one target (a wrath). */
    SWEEPER,

    /**
     * Shuts one creature off without removing it — the Pacifism shape: an Aura whose static
     * ability says the *enchanted* creature can't attack or block.
     *
     * Tagged apart from [REMOVAL] rather than folded into it, because the two are only alike from
     * the caster's side of the table. A Pacifism leaves a permanent on the battlefield that other
     * cards can still read, untap, sacrifice or re-target, and — the reason it is a separate tag
     * here — [CardIntent.staticPriorValue]'s ladder prices [REMOVAL] at 3.5, so folding it in would
     * silently re-value every Aura of this shape sitting on a battlefield. This tag is deliberately
     * absent from that ladder: it changes what the AI does with the card *in hand*, and nothing
     * about what the permanent is worth.
     *
     * The scope test is what keeps it honest — see `CardIntentAnalyzer.neutralizeTags`. "This
     * creature can't block" printed on a creature is a drawback, not an answer.
     */
    NEUTRALIZE,

    /** Draws cards. */
    DRAW,

    /** Searches a library. */
    TUTOR,

    /** Produces mana, or puts extra lands onto the battlefield. */
    RAMP,

    /** A continuous P/T bonus to a group of creatures — a lord or an anthem. */
    ANTHEM,

    /** A P/T bonus to one creature: an Aura/Equipment, or a one-shot pump. */
    PUMP,

    /** An instant-speed pump — the thing you hold up for combat. */
    COMBAT_TRICK,

    /**
     * Removal by fight: two creatures deal damage equal to their power to each other.
     *
     * Tagged apart from plain [REMOVAL] because it is the one answer whose reach is not a property
     * of the card — see [CardIntent.removalReach], whose null a fight would otherwise read as
     * "destruction, toughness is no defence" when toughness is in fact the whole defence.
     */
    FIGHT,

    /** Counters a spell. */
    COUNTERSPELL,

    /** Gains life. */
    LIFEGAIN,

    /** Makes a player discard. */
    DISCARD,

    /** Returns cards from a graveyard. */
    RECURSION,

    /** Creates tokens. */
    TOKEN_MAKER,

    /** Protects something: hexproof, indestructible, regeneration, damage prevention. */
    PROTECTION,

    /** Taps (or keeps tapped) a permanent — the Icy Manipulator shape. */
    TAPPER,

    /** Grants evasion (flying, menace, unblockable, …). */
    EVASION_GRANT,

    /** An activated ability whose cost is sacrificing a permanent — a sacrifice outlet. */
    SACRIFICE_OUTLET,
}

/** When a card's payoff happens — the axis the hold policy reasons about. */
enum class Speed {
    /** Sorcery-speed: an instant/sorcery without flash, or a permanent's ETB. */
    SORCERY,

    /** Castable on the opponent's turn: an instant, or anything with flash. */
    INSTANT,

    /** A continuous effect that needs no activation. */
    STATIC,

    /** Pays a cost to do something, repeatedly. */
    ACTIVATED,
}
