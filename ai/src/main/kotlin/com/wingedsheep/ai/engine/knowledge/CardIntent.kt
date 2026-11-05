package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.scripting.GameObjectFilter

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
     * it carries [IntentTag.FIGHT] to say so. A dynamic amount (X burn, Death Wind's -X/-X) also
     * leaves reach null: its size is only known on the stack, so no static reading can bound it.
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
     * A bound or context target does not itself carry its filter, so the effect's intent tag
     * cannot say whose board it touches. [AmbushWindow] needs that distinction: a bounce aimed
     * at an opposing blocker pays off before attacking, while an own-side bounce or blink can
     * wait for the opponent to act. Immediate blinks read as [IntentTag.PROTECTION]; the
     * [protectionOnlyFromBlink] field distinguishes them from combat shields.
     *
     * `false` when the card targets no permanent at all, which keeps a vanilla body — the case the
     * ambush policy most wants — from being mistaken for one that aims at us.
     */
    val targetsOnlyOurPermanents: Boolean = false,
    /** Protection comes only from an immediate blink, not a shield improving the next combat. */
    val protectionOnlyFromBlink: Boolean = false,
    /**
     * Whether the card carries an **untargeted group pump that expires** — "creatures you control
     * get +2/+1 until end of turn" (Rabbit Response), which `Patterns.Group.modifyStatsForAll`
     * lowers to a `ForEachEffect` over a group.
     *
     * [EffectWalker] treats a `ForEach` as a leaf, so such a card never reaches
     * [IntentTag.COMBAT_TRICK] and [HoldPolicy] never asks whether there is a fight to spend it in.
     * Kept as its own field rather than folded into that tag so the tag — and every profile that
     * reads it — stays exactly as it was; the one consumer is [HoldPolicy] behind
     * [com.wingedsheep.ai.engine.AiProfile.holdUnusablePumps].
     */
    val expiringGroupPump: Boolean = false,
    /**
     * Whether the card **looks at the top of its library and puts what it picks into hand** —
     * Sleight of Hand's "look at the top two, put one into your hand", which
     * `Patterns.Library.lookAtTopAndKeep` lowers to a gather from the top of the library and a move
     * to hand.
     *
     * The ability reads [IntentTag.DRAW] too (`CardIntentAnalyzer.digTags`); this card-level field
     * stays for the one consumer that needs the wish half as well — [HoldPolicy] behind
     * [com.wingedsheep.ai.engine.AiProfile.drawWithIdleMana].
     *
     * Also true for a **wish** — a card brought from outside the game into hand ([wishFilters]):
     * the same card-for-a-card trade, from the other hidden zone its owner can draw on.
     */
    val selectsIntoHand: Boolean = false,
    /**
     * The filters of every gather that brings a card its owner holds **outside the game** (the
     * private `Zone.SIDEBOARD`) into hand — Burning Wish's "a sorcery card you own from outside the
     * game", Extrapolate the Impossible's "two cards you own … from outside the game". Empty when
     * the card is not a wish.
     *
     * Read so [HoldPolicy] can tell a live wish from a dead one: the evaluator prices the trade as
     * a card spent and a card gained, so a wish into a sideboard holding nothing it can take is a
     * card thrown away, and must not collect the idle-mana nudge a live one does.
     */
    val wishFilters: List<GameObjectFilter> = emptyList(),
    /**
     * The subset of [tags] the card can cash in **the turn it arrives** — everything except what
     * sits behind a later event of the permanent's own: a trigger on it attacking, blocking, being
     * blocked, dealing damage or leaving the battlefield, and (for a creature without haste) an
     * ability that taps or untaps it.
     *
     * [AmbushWindow] asks "does this card pay off before the ambush?" and [tags] cannot answer it:
     * a tag says what the card does *anywhere*. Sphinx of False Conclusions loots when it attacks,
     * so it reads [IntentTag.DRAW], and the hold treated that as an ETB card draw and jammed the
     * Sphinx in its own main phase (2026-10-14, Reality Fracture draft) — though a no-haste body
     * cannot attack before the opponent's turn whichever window it lands in.
     *
     * Null means "same as [tags]": what an intent built by hand, or [UNKNOWN], should read.
     * Only [AmbushWindow] reads it, behind
     * [com.wingedsheep.ai.engine.AiProfile.holdInstantSpeedDeploys].
     */
    val arrivalTags: Set<IntentTag>? = null,
    /**
     * Whether the card lets its controller activate planeswalker loyalty abilities **at instant
     * speed** (Jace's Machinations). A walker it deploys or empowers can then be used at the
     * opponent's end step as well as in our main phase, so its loyalty is no reason to cast it now.
     * Read by [AmbushWindow].
     */
    val grantsInstantSpeedLoyalty: Boolean = false,
    /**
     * The generic mana a **tax counter** asks for — Divert Disaster's "counter target spell unless
     * its controller pays {2}" reads `2`. Null for a hard counter, a dynamic tax, or a card that
     * counters nothing: the field is set only when every counter the card carries is a fixed
     * "unless pays". Read by [CounterTax], behind
     * [com.wingedsheep.ai.engine.AiProfile.taxCountersReadTheirMana].
     */
    val counterTax: Int? = null,
    /**
     * What this card's non-mana activated abilities do — the payoffs a permanent still holds while
     * it sits on the battlefield (a Spellbomb's draw, a Bauble's land search). Unlike an ETB, which
     * has paid out by then, these are value the board still carries even when they can be used only
     * once; `CardIntentAnalyzer.priorLadder` prices them.
     */
    val cashableTags: Set<IntentTag> = emptySet(),
) {
    /** [arrivalTags], falling back to [tags] where the analyzer did not split them. */
    val tagsOnArrival: Set<IntentTag> get() = arrivalTags ?: tags

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
    /** A restriction on the source creature attacking or blocking; a drawback, not a payoff. */
    COMBAT_RESTRICTION,

    /** Lets a creature block more attackers in one combat. */
    EXTRA_BLOCKING,

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
     * The one-shot form reads the same: another creature losing all its abilities, or having its
     * base power and toughness set to 1/1 or less without dying (Warkite Marauder's "loses all
     * abilities and has base power and toughness 0/1 until end of turn") — still on the
     * battlefield, no longer a threat or a blocker worth the name.
     */
    NEUTRALIZE,

    /** Draws cards. */
    DRAW,

    /** Searches a library. */
    TUTOR,

    /**
     * Looks at the top of its controller's library and arranges it — scry, or surveil (scry whose
     * discards go to the graveyard). No card changes hands, so it is neither [DRAW] nor [TUTOR],
     * but it is the same card flow for timing: a card seen at the opponent's end step is the card
     * seen now, so [HoldPolicy] cashes a selection-only activation there, with mana that would
     * otherwise empty, like a draw activation.
     */
    CARD_SELECTION,

    /** Produces mana, or puts extra lands onto the battlefield. */
    RAMP,

    /**
     * Makes its controller's own spells or activated abilities cheaper — "Equipment spells you cast
     * cost {1} less" (Bladegraft Aspirant), "activated abilities you activate cost {1} less"
     * (Training Grounds, Silver-Fur Master).
     *
     * Tagged apart from [RAMP] because a discount is not a mana source: it pays for nothing on its
     * own, can't be tapped for a colour, and only matters for the spells and abilities it names —
     * folding it into [RAMP] would count it as a mana source wherever the AI asks for one (the
     * mana-source prior rung, a deck's ramp role). Only a discount on the controller's own side reads
     * this way; a symmetric one (Helm of Awakening) helps the opponent as much and earns no tag.
     */
    COST_REDUCER,

    /** Multiplies damage from our sources, without also multiplying opposing sources. */
    DAMAGE_AMPLIFIER,

    /** Deals damage to an explicitly named player; may be delayed or damage to ourselves.
     * This describes the recipient, not immediate lethal reach or creature removal. */
    DAMAGE_TO_PLAYERS,

    /** A continuous P/T bonus to a group of creatures — a lord or an anthem. */
    ANTHEM,

    /** Grants a chosen creature type, enabling tribal payoffs without making a body or a stat bonus. */
    TRIBAL_ENABLER,

    /** Mills a player who can be an opponent, advancing a library-depletion win condition. */
    MILL,

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

    /**
     * Changes the target of a spell or ability on the stack — "change the target of target spell
     * with a single target" (Bolt Bend, Misdirection), "change a target of target spell or ability
     * to this creature" (Spellskite, Hydroelectric Specimen). Like [COUNTERSPELL] it answers only
     * something already on the stack — an instant one needs a spell to target before it can be
     * cast at all. It is not [PROTECTION]: that tag releases a flash body's ambush hold before
     * combat, and a redirect on arrival is worth nothing without a spell to catch. Where the
     * spell goes is the decision's question, read off what the redirected object does to the slot
     * (`SpellRedirectChoice`).
     */
    SPELL_REDIRECT,

    /**
     * Copies a spell or ability — "copy target instant or sorcery spell; you may choose new targets
     * for the copy" (Fork, Double Major, Twincast), "copy target activated or triggered ability"
     * (Lithoform Engine, Echo, Perceptive Prodigy), "when you next cast an instant or sorcery spell
     * this turn, copy that spell" (Double Vision's kin). The copy is ours whoever controls the
     * original, and worth what that object does, so the card is a second helping of the deck's best
     * spell — it rewards instants and sorceries the way prowess does (`SynergyAxes`). Like
     * [SPELL_REDIRECT] the targeted forms answer only what is already on the stack; which spell to
     * copy is the polarity reader's two-sided slot, and where the copy's new targets go is read off
     * the copied effect (`TargetSelection.decisionPolarities`).
     */
    SPELL_COPY,

    /**
     * Takes cards out of a graveyard that isn't only ours — "exile target card from a graveyard"
     * (Scavenging Ooze, Carrion Locust, Mourner's Shield's imprint), "exile target player's
     * graveyard" (Tormod's Crypt, Bojuka Bog), "put target card from a graveyard on the bottom of
     * its owner's library" (Junktroller). It answers recursion, flashback and delirium before they
     * happen, but no permanent on the board: never [REMOVAL], and worth little in Limited on its own.
     */
    GRAVEYARD_HATE,

    /** Gains life. */
    LIFEGAIN,

    /** Makes a player discard. */
    DISCARD,

    /** Returns cards from a graveyard, or onto our battlefield from the pile a permanent exiled. */
    RECURSION,

    /** Creates tokens. */
    TOKEN_MAKER,

    /** Protects something: hexproof, indestructible, regeneration, damage prevention. */
    PROTECTION,

    /** Taps (or keeps tapped) a permanent — the Icy Manipulator shape. */
    TAPPER,

    /** Grants evasion (flying, menace, unblockable, …). */
    EVASION_GRANT,

    /**
     * Makes a creature better in a fight without adding to its power and toughness — one-shot or
     * static, and whichever creature the polarity reader says it lands on: a granted first strike,
     * double strike, deathtouch, lifelink, vigilance or haste, a
     * "must be blocked" that drags a blocker in, or a base power and toughness it takes from
     * something bigger (Exuberant Wolfbear). Not a [PUMP]: nothing here is a +N/+N a simulation
     * or a precombat plan may add to the creature's stats.
     */
    COMBAT_ENHANCEMENT,

    /**
     * Moves Equipment onto a creature without paying its equip cost — "attach target Equipment you
     * control to target creature you control" (Halvar, Weapons Vendor), "attach any number of
     * Equipment you control to it" (Beatrix, Loyal General), "attach all Equipment on the
     * battlefield to it" (Vulshok Battlemaster). Worth the Equipment it moves, so it wants
     * Equipment in the deck and creatures to wear it; which creature gets it is the polarity reader's
     * creature slot, a gift.
     */
    EQUIPMENT_RELOCATION,

    /**
     * Turns a permanent into a copy of another permanent or card — "this becomes a copy of target
     * creature" (Lazav, the Multifarious), "equipped creature becomes a copy of up to one other
     * target creature you control" (Blade of Shared Souls), "target artifact or creature becomes a
     * copy of another target artifact or creature" (True Polymorph), "each other creature you
     * control becomes a copy of it until end of turn" (Naga Fleshcrafter). Worth the best thing it
     * can copy, which is a board question: the polarity reader keeps the copy source on our side
     * when its filter does and leaves the rest to the simulation, which sees the copied
     * characteristics and abilities on the receiving permanent.
     */
    PERMANENT_COPY,

    /** An activated ability whose cost is sacrificing a permanent — a sacrifice outlet. */
    SACRIFICE_OUTLET,

    /**
     * Stops an opponent casting spells (or a kind of spell) for a stretch of turn — Silence's "your
     * opponents can't cast spells this turn", Sphinx's Decree's "can't cast instant or sorcery spells
     * during that player's next turn", Xantid Swarm's attack trigger.
     *
     * Its payoff is spells that are never cast, which no board simulation sees: the ban leaves every
     * permanent where it was. So the tag is what tells the AI the card is disruption rather than a
     * blank, and what [HoldPolicy] reads to cast an instant-speed ban where it covers the opponent's
     * whole turn. A ban that binds its own controller too (Calamity's Wake, Conduit of Worlds) is the
     * card's price, not its point, and earns no tag — see `CardIntentAnalyzer.bansAnOpponent`.
     */
    CAST_BAN,

    /**
     * Stops permanents entering (or dying) from triggering abilities — Torpor Orb's and Tocatli
     * Honor Guard's "creatures entering the battlefield don't cause abilities to trigger", Hushbringer's
     * enters-or-dies lock, Elesh Norn, Mother of Machines' opponents-only half.
     *
     * A lock moves nothing and its payoff is triggers that never happen, so no board simulation sees
     * it; without the tag the static read as "abilities not recognised" and the card as a blank. The
     * symmetric lock is a hoser — it shuts off its controller's own enter triggers too — so the draft
     * heuristic prices it low. It is not opponent-facing: a static has no target to aim, and the symmetric lock hits its own side too.
     */
    TRIGGER_LOCK,

    /**
     * Stops counters going on players or permanents — Solemnity's "Players can't get counters.
     * Counters can't be put on artifacts, creatures, enchantments, or lands", a battlefield-wide
     * "can't have +1/+1 counters put on them" (Blightbeetle's opponents-only form).
     *
     * Like [TRIGGER_LOCK], a lock moves nothing — its payoff is counters that never arrive (poison,
     * -1/-1, +1/+1, undying and persist returns) — so no board simulation sees it. The symmetric
     * form also starves its controller's own counters, so the draft heuristic prices it as a hoser.
     */
    COUNTER_LOCK,

    /**
     * Takes the tokens an opponent would create — Crafty Cutpurse's "each token that would be
     * created under an opponent's control this turn is created under your control instead".
     *
     * Tagged apart from [TOKEN_MAKER] because it makes nothing on its own: its payoff is the
     * opponent's token-making, which may never come, so counting it as a token source would feed
     * the token-synergy and card-or-token reads with tokens that don't exist. Without the tag the
     * ability read as unrecognised and the card as its bare body. It has no target to aim, so it
     * is not opponent-facing either.
     */
    TOKEN_THEFT,

    /**
     * Ends the game in its controller's favour outright — an opponent loses (Phage the Untouchable's
     * "that player loses the game", Vraska, Scheming Gorgon's −10 handing every creature of ours
     * that trigger), or its controller wins.
     *
     * Its payoff is the game, which no board simulation sees until the damage lands: the grant
     * itself moves nothing but a keyword, so the −10 read as a planeswalker spent for deathtouch.
     * The tag is what says what the card is for; [com.wingedsheep.ai.engine.evaluation.FinisherStrike]
     * is what prices a board whose attackers carry the trigger. A loss its own controller suffers
     * (Phage cast from anywhere but the hand) is the card's price and earns no tag.
     */
    GAME_WIN,

    /**
     * Its controller takes an extra turn — Time Walk, Timestream Navigator's "take an extra turn
     * after this one". Its payoff is a whole turn: an untap, a draw and another attack, none of which
     * the board simulation sees when the effect resolves, so without the tag the ability read as
     * "no intent". An extra turn handed to an opponent is never a play and earns no tag.
     */
    EXTRA_TURN,

    /**
     * Casts a spell without paying its mana cost — Wildfire Eternal's and Omnispell Adept's "cast an
     * instant or sorcery spell from your hand without paying its mana cost", a cascade-like "cast it
     * without paying its mana cost" off the top of a library or out of exile. Its payoff is the mana
     * the spell didn't cost (and, from outside the hand, the card itself), which the board
     * simulation only sees once the spell resolves, so without the tag the ability read as
     * "no intent". A cast that still pays the spell's mana cost earns no tag.
     */
    FREE_SPELL,
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
