package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.knowledge.AmountSign
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.CardLookup
import com.wingedsheep.ai.engine.knowledge.CombatReach
import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.engine.knowledge.TargetlessEntry
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LegendRuleDoesNotApplyTo
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.DamageCantBePreventedThisTurnEffect
import com.wingedsheep.sdk.scripting.effects.MarkSpellExileWithCountersEffect
import com.wingedsheep.sdk.scripting.effects.AdaptEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import com.wingedsheep.sdk.scripting.effects.DoubleCountersEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DealDamagePerEntityInZoneEffect
import com.wingedsheep.sdk.scripting.effects.CantAttackEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ExileAndReturnTransformedEffect
import com.wingedsheep.sdk.scripting.effects.ExploreEffect
import com.wingedsheep.sdk.scripting.effects.LoseGameEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect
import com.wingedsheep.sdk.scripting.effects.GrantLoyaltyActivationPermissionEffect
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MustAttackGroupEffect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.ProliferateEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersOfIteratedKindEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersOfChosenKindEffect
import com.wingedsheep.sdk.scripting.effects.CounterKindSource
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import com.wingedsheep.sdk.scripting.effects.MoveAllCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveAllLastKnownCountersEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.RemoveAnyNumberOfCountersEffect
import com.wingedsheep.sdk.scripting.effects.RemoveAllCountersOfTypeEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersFromAmongEffect
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.effects.PhaseOutEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.ForagedEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.effects.SetBaseStatsEffect
import com.wingedsheep.sdk.scripting.effects.SetLifeTotalEffect
import com.wingedsheep.sdk.scripting.effects.SkipUntapEffect
import com.wingedsheep.sdk.scripting.effects.ScryEffect
import com.wingedsheep.sdk.scripting.effects.SurveilEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.TheRingTemptsYouEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.effects.VentureIntoTheDungeonEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import java.util.Locale

/**
 * The AI card-reading audit: runs the AI's static card readers over a card and reports, per ability,
 * what they understood and what they could not read. A gap is named by the SDK **effect type** that
 * defeated the reader — so one classifier entry, not one card, is the unit of work (see
 * `docs/ai/reading-audit.md`).
 *
 * Two consumers read the same walk:
 *
 *  - The **corpus audit** (`just ai-reading-audit`, `AiReadingRatchetTest`) wants only the per-card
 *    [Flag] keys — [audit] and [cardFlags]. Those strings are what the checked-in baseline records,
 *    so they are a stable contract: changing one re-blesses every card that carries it.
 *  - The **Argentum Assay explorer** (mounted by `game-server`) shows a developer *where* each gap
 *    is — [read]: which ability, which step of its effect, which target slot, which reader, and a
 *    plain-English reason — next to what each reader *did* understand.
 *
 * Every check asks a reader that the AI really consults, through the entry point the AI uses:
 *
 *  - [Check.POLARITY] — [TargetPolarityAnalyzer], per target slot of a spell (or each mode of a
 *    modal one), an activated ability and a triggered ability. A slot reading
 *    [TargetPolarity.UNKNOWN] is attributed to the leaves that name it and that read UNKNOWN on
 *    their own; a slot whose leaves each read but disagree is `mixed(A+B)`.
 *  - [Check.INTENT] — [CardIntentAnalyzer.analyzeEffect] per effect slot (the spell, each trigger,
 *    each non-mana ability): a slot that earns no [IntentTag] at all is invisible to every intent
 *    consumer, and is attributed to each of its leaves — except the leaves that need no tag
 *    ([needsNoIntent]: a counter on the source itself or one it removes from itself, any counter on a player, damage to a
 *    player, life loss, a permanent untapping, moving, transforming, animating or sacrificing itself, a creature
 *    shifting its own P/T with a loss on either side, its controller venturing into the dungeon or being tempted by the Ring, counters of any kind moved between permanents or removed from a target, an upkeep cost its controller pays or suffers
 *    on its own source, an "unless" whose suffer is life loss; and [noIntentPipeline]: a mill, or a return
 *    to hand chosen among its controller's own permanents).
 *  - [Check.COMBAT] — [CombatReach] over each leaf of a non-mana activated ability that changes
 *    power/toughness, keywords, P/T counters or blocking, and that combat simulation cannot see.
 *  - [Check.TIMING] — an until-end-of-turn leaf on an instant-speed activation or an instant-speed
 *    spell with no reading in [ExpiringGrantWindow] (abilities) or in the combat-trick / group-pump
 *    reading [com.wingedsheep.ai.engine.knowledge.HoldPolicy] gets from [CardIntentAnalyzer]
 *    (spells; a combat trick is held whole, so each of its expiring leaves reads, and a prevention
 *    spell's shield is timed by simulation through the damage it stops).
 *  - [Check.MODAL] — a "choose more than one" modal whose per-extra-mode cost has no picker in
 *    `TargetSelection.payEscalateCost`, so the AI can only ever choose one mode.
 *
 * Choose-N modals in general and crew/saddle are counted as information, not gaps: both are read
 * today (`TargetSelection.modeSelections`, `Strategist.tapForPowerSelection`).
 *
 * Static abilities are listed by [read] but not checked: no static reader is asked about them. The
 * evaluator sees their effect on the board through projected state, and an Aura's statics are read
 * through its cast target's polarity, which [Check.POLARITY] does check.
 *
 * The readers memoize by card name, so reading a definition that shares a name with a different one
 * (an Assay-compiled preview of a card) shares the cache entry. Every caller hands one definition per
 * name, so that never mixes two answers.
 */
object AiReadingAudit {

    enum class Check(val label: String, val reader: String) {
        POLARITY("polarity", "Target polarity"),
        INTENT("intent", "Card intent"),
        COMBAT("combat", "Combat reach"),
        TIMING("timing", "End-of-turn timing"),
        MODAL("modal", "Escalate picker"),
    }

    /** One thing a reader did not understand on one card — the ratchet's unit. */
    data class Flag(val check: Check, val effectType: String) {
        val key: String get() = "${check.label}: $effectType"
    }

    /** Where on a card an ability lives. */
    enum class AbilityKind(val label: String) {
        SPELL("Spell"),
        ACTIVATED("Activated ability"),
        TRIGGERED("Triggered ability"),
        CHAPTER("Chapter"),
        STATE_TRIGGER("State trigger"),
        STATIC("Static ability"),
    }

    /**
     * One ability of a card: [kind] and [number] (1-based within the kind on its face, counting mana
     * abilities so the number matches the script's order), the [face] it is on when the card has more
     * than one, and its rules [text] as the SDK describes it.
     */
    data class AbilityRef(val kind: AbilityKind, val number: Int, val face: String?, val text: String) {
        /** "Activated ability 2", "Spell". */
        val title: String get() = if (kind == AbilityKind.SPELL) kind.label else "${kind.label} $number"
    }

    /**
     * One reader's answer about one part of an ability.
     *
     * @property read whether the reader understood this part.
     * @property reason the answer in plain English: "target 1: can't tell if this helps or hurts".
     * @property effectType the SDK effect type the answer is about, for a developer — null when the
     *   finding is about the ability as a whole.
     * @property step 1-based index of that effect among the ability's effect steps (its leaves, in
     *   order), or null.
     * @property stepText that step's own description.
     * @property target 1-based target slot, for a polarity finding.
     * @property mode 1-based mode of a modal ability, when the finding is inside one.
     * @property flag the audit [Flag] this gap produces; null on an understood part.
     */
    data class Finding(
        val check: Check,
        val read: Boolean,
        val reason: String,
        val effectType: String? = null,
        val step: Int? = null,
        val stepText: String? = null,
        val target: Int? = null,
        val mode: Int? = null,
        val flag: Flag? = null,
    )

    /**
     * Everything the readers said about one ability. A [Check]-free static ability has no findings.
     *
     * [body] marks a static that only grants its own creature a keyword ("this creature has
     * vigilance as long as you control another Dinosaur") — part of the creature's body, which the
     * evaluator prices from its projected keywords the way it prices a printed one, so nothing an
     * intent reader is meant to place — or that only changes the card's own casting cost.
     */
    data class AbilityReading(val ability: AbilityRef, val findings: List<Finding>, val body: Boolean = false) {
        /** Whether any reader was asked about this ability. Static abilities are not. */
        val checked: Boolean get() = findings.isNotEmpty()

        /** Asked, and every answer understood. */
        val readFully: Boolean get() = checked && findings.all { it.read }
    }

    /** The per-ability reading of a whole card: per face, spell, triggers, activated, then static. */
    data class CardReading(val card: String, val abilities: List<AbilityReading>) {
        /** Every distinct [Flag], sorted by key — exactly what [audit] returns. */
        val flags: List<Flag> get() = abilities.flatMap { a -> a.findings.mapNotNull { it.flag } }.distinct().sortedBy { it.key }

        val readFully: Boolean get() = abilities.all { a -> a.findings.all { it.read } }
    }

    /** Every [Flag] for [card], deduplicated, in a stable order. */
    fun audit(card: CardDefinition, lookup: CardLookup = TargetPolarityAnalyzer.NO_DEFINITIONS): List<Flag> =
        read(card, lookup).flags

    /** What the AI's readers make of each ability of [card]. */
    fun read(card: CardDefinition, lookup: CardLookup = TargetPolarityAnalyzer.NO_DEFINITIONS): CardReading {
        val faces = facesOf(card)
        val named = faces.size > 1
        return CardReading(card.name, faces.flatMap { readFace(card, it, lookup, named) })
    }

    /**
     * A static whose whole effect is a keyword on the creature that has it — printed plainly or
     * gated on a condition (ascend's "has flying as long as you have the city's blessing").
     */
    private fun isBodyStatic(static: StaticAbility): Boolean = when (static) {
        is GrantKeyword -> static.filter.scope == Scope.Self
        // A creature's own printed penalty ("this creature gets -2/-2 as long as …") is part of its
        // body, read by the evaluator through projected P/T — not an ability a reader missed.
        is ModifyStats -> static.filter.scope == Scope.Self && static.powerBonus <= 0 && static.toughnessBonus <= 0
        // "This spell costs {2} less to cast for each card you've cycled or discarded this turn"
        // (Hollow One, affinity's Tangle Golem): a change to the card's own cost, which the legal
        // actions the AI is offered already price — part of the card, like its mana cost.
        is ModifySpellCost -> static.target == SpellCostTarget.SelfCast
        is ConditionalStaticAbility -> isBodyStatic(static.ability)
        is CompositeStaticAbility -> static.abilities.isNotEmpty() && static.abilities.all(::isBodyStatic)
        else -> false
    }

    /** Whether [card] is a choose-more-than-one modal anywhere — informational. */
    fun isChooseN(card: CardDefinition): Boolean = facesOf(card).any { face ->
        roots(face.script).any { root ->
            EffectWalker.leaves(root).any { it is ModalEffect && (it.chooseCount > 1 || it.dynamicChooseCount != null) }
        }
    }

    /** Whether [card] has crew or saddle — informational. */
    fun hasCrewOrSaddle(card: CardDefinition): Boolean = facesOf(card).any { face ->
        face.keywordAbilityNames.any { "keyword=CREW" in it || "keyword=SADDLE" in it }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** A script the card can put into play, with what makes it a spell at instant speed. */
    private class Face(
        val name: String,
        val script: CardScript,
        val instantSpeed: Boolean,
        val keywordAbilityNames: List<String>,
    )

    private fun facesOf(card: CardDefinition): List<Face> {
        fun face(def: CardDefinition) = Face(
            def.name,
            def.script,
            def.typeLine.isInstant || Keyword.FLASH in def.keywords,
            def.keywordAbilities.map { it.toString() },
        )
        return buildList {
            add(face(card))
            card.cardFaces.forEach { f ->
                add(Face(f.name, f.script, f.typeLine.isInstant || Keyword.FLASH in f.keywords, emptyList()))
            }
            card.backFace?.let { add(face(it)) }
        }
    }

    /** Every effect root a script can resolve: spell, triggers, state triggers, non-mana abilities. */
    private fun roots(script: CardScript): List<Effect> =
        EffectWalker.slots(script).map { it.effect } + script.stateTriggeredAbilities.map { it.effect }

    /** Collects one ability's findings. */
    private class Sink {
        val findings = mutableListOf<Finding>()
        operator fun plusAssign(finding: Finding) { findings += finding }
    }

    private fun readFace(card: CardDefinition, face: Face, lookup: CardLookup, named: Boolean): List<AbilityReading> {
        val script = face.script
        val faceName = face.name.takeIf { named }
        val intent = CardIntentAnalyzer.analyze(card)
        // A pile only ever filled with our own permanents comes back to us (Endless Sands).
        val ownPile = CardIntentAnalyzer.stashesOwnPermanents(listOf(card.script) + card.cardFaces.map { it.script })
        // A pile it may cast from (Maralen, Theater of Horrors) makes the exile into it a draw.
        val pileCast = CardIntentAnalyzer.pileCastTags(listOf(card.script, script) + card.cardFaces.map { it.script })
        val out = mutableListOf<AbilityReading>()

        // ── The spell ───────────────────────────────────────────────────────
        val spell = script.spellEffect
        val isAura = script.auraTarget != null && script.targetRequirements.isEmpty()
        // A bare target list with no effect is still asked about: it reads "(no effect)".
        if (spell != null || isAura || script.targetRequirements.isNotEmpty()) {
            val sink = Sink()
            when {
                isAura -> {
                    if (script === card.script && card.cardFaces.isEmpty()) {
                        when (val reading = TargetPolarityAnalyzer.forSpell(card, lookup).singleOrNull()) {
                            TargetPolarity.UNKNOWN -> auraFlags(script).forEach { label ->
                                sink += Finding(
                                    Check.POLARITY, read = false,
                                    reason = "enchant target: can't tell if this Aura helps or hurts what it enchants",
                                    effectType = label, target = 1,
                                    flag = Flag(Check.POLARITY, label),
                                )
                            }
                            null -> Unit
                            else -> sink += Finding(
                                Check.POLARITY, read = true,
                                reason = "enchant target: ${polarityPhrase(reading)}", target = 1,
                            )
                        }
                    }
                }
                spell is ModalEffect && script.targetRequirements.isEmpty() ->
                    spell.modes.forEachIndexed { m, mode ->
                        polarity(mode.effect, mode.targetRequirements, TargetPolarityAnalyzer.forMode(mode, lookup), lookup, sink, m + 1)
                    }
                script.targetRequirements.isNotEmpty() -> {
                    val reqs = script.targetRequirements
                    polarity(spell, reqs, TargetPolarityAnalyzer.forRequirements(spell, reqs, lookup), lookup, sink, null)
                }
            }
            if (spell != null) {
                intent(spell, sink, script.targetRequirements, ownPile, pileCast = pileCast)
                if (face.instantSpeed) {
                    // HoldPolicy holds a COMBAT_TRICK spell whole, so every expiring leaf on it (the
                    // keyword beside the pump, too) waits for the same fight or response. Not a
                    // modal one: its tags are the union of its modes, the hold reads the chosen ones.
                    val heldWhole = IntentTag.COMBAT_TRICK in intent.tags &&
                        EffectWalker.leaves(spell).none { it is ModalEffect }
                    timing(spell, sink) { leaf ->
                        ExpiringGrantWindow.hasExpiringReading(leaf, creatureGrants = false) ||
                            (heldWhole || (leaf is ModifyStatsEffect && IntentTag.COMBAT_TRICK in intent.tags)) ||
                            (leaf is ForEachEffect && intent.expiringGroupPump) ||
                            // A prevention shield on a spell needs no hold: cast before any damage it
                            // could stop, the shield leaves the simulated board unchanged, and cast into
                            // the damage, simulation through that damage prices it. Its other leaves
                            // (a counter, an untap) keep their own timing findings.
                            leaf is PreventDamageEffect ||
                            // A death-return grant (Verdant Rebirth, Feign Death): HoldPolicy holds
                            // it for the removal it answers or the fight its target is in.
                            (ExpiringGrantWindow.returnsFromDeathThisTurn(leaf) && IntentTag.PROTECTION in intent.tags) ||
                            // Indestructible until end of turn (Renegade's Getaway): the same hold,
                            // whatever permanent rider (a Servo) rides beside it.
                            (ExpiringGrantWindow.shieldsFromDestructionThisTurn(leaf) && IntentTag.PROTECTION in intent.tags) ||
                            // An instant-speed cast ban on an opponent: HoldPolicy rewards casting it
                            // before their main phase, where it covers their whole turn. Not a face
                            // (Revel in Silence): CastBanWindow reads the card's own spell only.
                            (leaf is CantCastSpellsEffect && IntentTag.CAST_BAN in intent.tags &&
                                script === card.script && card.cardFaces.isEmpty()) ||
                            // A forced attack on an opponent's creatures: HoldPolicy rewards casting
                            // it on their turn before they attack ([ForcedAttackWindow]), the only
                            // window "this turn" binds an attack of theirs. Own spell only, as above.
                            // "Until end of turn, you may activate loyalty abilities of Jace planeswalkers
                            // you control … any time you could cast an instant" (Jace's Machinations):
                            // HoldPolicy holds it for the opponent's turn, where the walker gets a second
                            // activation ([CardIntent.grantsInstantSpeedLoyalty], AmbushWindow).
                            (leaf is GrantLoyaltyActivationPermissionEffect && leaf.instantSpeed &&
                                intent.grantsInstantSpeedLoyalty) ||
                            (leaf is MustAttackGroupEffect && IntentTag.FORCED_ATTACK in intent.tags &&
                                script === card.script && card.cardFaces.isEmpty()) ||
                            // A spell that only grants keywords until end of turn is floored where no
                            // fight is left; the floor asks it of the whole spell, not one leaf. Not
                            // an Adventure or prepare face: HoldPolicy reads no face's effects.
                            (script === card.script && card.cardFaces.isEmpty() &&
                                ExpiringGrantWindow.readsSpellKeywordGrant(spell)) ||
                            // An expiring protective keyword beside a payoff that outlives the turn
                            // (Snakeskin Veil's counter, Moogles' Valor's tokens): the lasting part pays
                            // for the card whenever it is cast, and simulation prices it in every window,
                            // so it is no trick to hold — CardIntentAnalyzer withholds COMBAT_TRICK from
                            // it on purpose. Faces too (Assure // Assemble): the argument needs no hold.
                            // Stricter than the analyzer: a shield or player hexproof that ends at
                            // cleanup is not lasting here (Blinding Fog stays flagged).
                            protectionBesideLastingPayoff(spell, leaf)
                    }
                }
                modal(spell, sink)
            }
            val text = when {
                spell != null -> describe { spell.description }
                isAura -> "Enchant ${describe { script.auraTarget?.description ?: "permanent" }}"
                else -> ""
            }
            out += AbilityReading(AbilityRef(AbilityKind.SPELL, 1, faceName, text), sink.findings)
        }

        // ── Activated abilities ─────────────────────────────────────────────
        script.activatedAbilities.forEachIndexed { i, ability ->
            if (ability.isManaAbility) return@forEachIndexed
            val sink = Sink()
            abilityPolarity(ability.effect, ability.targetRequirements, lookup, sink) {
                TargetPolarityAnalyzer.forAbility(card, ability, lookup)
            }
            intent(ability.effect, sink, ability.targetRequirements, ownPile, pileCast = pileCast)
            combat(ability, sink)
            if (ability.timing == TimingRule.InstantSpeed) {
                // A pure can't-attack / can't-block activation is judged whole by the restriction
                // window (ExpiringGrantWindow.restrictionHasNoWindow), not leaf by leaf.
                val restriction = ExpiringGrantWindow.readsRestriction(ability.effect)
                timing(ability.effect, sink) {
                    restriction || ExpiringGrantWindow.hasExpiringReading(it, creatureGrants = true)
                }
            }
            modal(ability.effect, sink)
            out += AbilityReading(AbilityRef(AbilityKind.ACTIVATED, i + 1, faceName, describe { ability.description }), sink.findings)
        }

        // ── Triggered abilities ─────────────────────────────────────────────
        script.triggeredAbilities.forEachIndexed { i, trigger ->
            val sink = Sink()
            val reqs = listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements
            abilityPolarity(trigger.effect, reqs, lookup, sink) {
                TargetPolarityAnalyzer.forRequirements(trigger.effect, reqs, lookup, source = card)
            }
            // "When this leaves the battlefield, return the exiled card" (O-Ring, Fiend Hunter, champion)
            // needs no intent: it never stands alone — it is the price of the entry's exile, with no
            // target to aim and no window to choose, and the entry trigger already carries the reading.
            // One that puts the cards into a hand stays flagged: that can be a payoff (Hoarding Dragon) —
            // unless what it hands back is what casting this card exiled as a cost (behold-and-exile,
            // "exile a creature you control"): that is a refund of the card's own price, not a payoff.
            if (TargetlessEntry.handsBackTheEntry(trigger) &&
                (!movesToHand(trigger.effect) || script.additionalCosts.any(::exilesAsCost))
            ) {
                sink += Finding(Check.INTENT, read = true, reason = "needs no intent: it only hands back what this permanent exiled, when it leaves")
            } else {
                intent(trigger.effect, sink, reqs, ownPile = ownPile, turnAhead = EffectWalker.firesWithItsTurnAhead(trigger), pileCast = pileCast)
            }
            modal(trigger.effect, sink)
            out += AbilityReading(AbilityRef(AbilityKind.TRIGGERED, i + 1, faceName, describe { trigger.description }), sink.findings)
        }

        // ── Saga chapters: triggered abilities (CR 714.2b), numbered by chapter ──
        script.sagaChapters.sortedBy { it.chapter }.forEach { chapter ->
            val sink = Sink()
            val reqs = EffectWalker.chapterTargets(chapter)
            abilityPolarity(chapter.effect, reqs, lookup, sink) {
                TargetPolarityAnalyzer.forRequirements(chapter.effect, reqs, lookup, source = card)
            }
            intent(chapter.effect, sink, reqs, ownPile = ownPile, turnAhead = true)
            modal(chapter.effect, sink)
            out += AbilityReading(AbilityRef(AbilityKind.CHAPTER, chapter.chapter, faceName, describe { chapter.effect.description }), sink.findings)
        }

        // ── State triggers ──────────────────────────────────────────────────
        script.stateTriggeredAbilities.forEachIndexed { i, trigger ->
            val sink = Sink()
            intent(trigger.effect, sink, emptyList(), ownPile, pileCast = pileCast)
            modal(trigger.effect, sink)
            out += AbilityReading(AbilityRef(AbilityKind.STATE_TRIGGER, i + 1, faceName, describe { trigger.description }), sink.findings)
        }

        // ── Static abilities: listed, not checked ───────────────────────────
        script.staticAbilities.forEachIndexed { i, static ->
            out += AbilityReading(
                AbilityRef(AbilityKind.STATIC, i + 1, faceName, describe { static.description }), emptyList(),
                body = isBodyStatic(static),
            )
        }
        // Closest to Oracle's own order — a permanent's triggers usually precede its activated
        // abilities — with the unchecked statics last so they don't push the findings down.
        return out.sortedBy { DISPLAY_ORDER.indexOf(it.ability.kind) }
    }

    private val DISPLAY_ORDER = listOf(
        AbilityKind.SPELL, AbilityKind.CHAPTER, AbilityKind.TRIGGERED, AbilityKind.STATE_TRIGGER, AbilityKind.ACTIVATED, AbilityKind.STATIC,
    )

    /** Polarity of an ability's slots, descending into a modal body's modes the way the AI does. */
    private fun abilityPolarity(
        effect: Effect,
        reqs: List<TargetRequirement>,
        lookup: CardLookup,
        sink: Sink,
        read: () -> List<TargetPolarity>,
    ) {
        if (effect is ModalEffect && reqs.isEmpty()) {
            effect.modes.forEachIndexed { m, mode ->
                polarity(mode.effect, mode.targetRequirements, TargetPolarityAnalyzer.forMode(mode, lookup), lookup, sink, m + 1)
            }
        } else if (reqs.isNotEmpty()) {
            polarity(effect, reqs, read(), lookup, sink, null)
        }
    }

    /** Report each slot in [readings]; attribute each UNKNOWN one to the effect types that left it unread. */
    private fun polarity(
        effect: Effect?,
        reqs: List<TargetRequirement>,
        readings: List<TargetPolarity>,
        lookup: CardLookup,
        sink: Sink,
        mode: Int?,
    ) {
        for ((slot, reading) in readings.withIndex()) {
            val target = slot + 1
            if (reading != TargetPolarity.UNKNOWN) {
                sink += Finding(Check.POLARITY, read = true, reason = "target $target: ${polarityPhrase(reading)}", target = target, mode = mode)
                continue
            }
            val unread = "target $target: can't tell if this helps or hurts"
            if (effect == null) {
                sink += gap(Check.POLARITY, "target $target: no effect acts on this target", "(no effect)", null, target, mode)
                continue
            }
            val leaves = EffectWalker.payoffLeaves(effect)
            val probes = leaves.map { leaf ->
                leaf to (TargetPolarityAnalyzer.forRequirements(leaf, reqs, lookup).getOrNull(slot) ?: TargetPolarity.UNKNOWN)
            }
            val unknown = probes.filter { it.second == TargetPolarity.UNKNOWN }.map { it.first }
            val naming = unknown.filter { names(it, reqs[slot], slot) }
            val read = probes.filter { it.second != TargetPolarity.UNKNOWN }
            when {
                naming.isNotEmpty() -> labels(naming, pipelineOf = leaves).forEach { (label, leaf) ->
                    sink += gap(Check.POLARITY, unread, label, step(leaves, leaf), target, mode)
                }
                read.map { it.second }.distinct().size > 1 -> {
                    val label = "mixed(${read.map { label(it.first) }.distinct().sorted().joinToString("+")})"
                    sink += gap(Check.POLARITY, "target $target: its steps disagree — one helps, another hurts", label, null, target, mode)
                }
                unknown.isNotEmpty() -> labels(unknown).forEach { (label, leaf) ->
                    sink += gap(Check.POLARITY, unread, label, step(leaves, leaf), target, mode)
                }
                else -> sink += gap(Check.POLARITY, "target $target: no step of the effect names this target", "(slot named by no leaf)", null, target, mode)
            }
        }
    }

    /**
     * Intent per ability, not per leaf: a pipeline's plumbing ("shuffle", "select from the gathered
     * cards") earns no tag of its own and needs none when a sibling leaf reads the ability.
     */
    private fun intent(
        root: Effect,
        sink: Sink,
        reqs: List<TargetRequirement>,
        ownPile: Boolean = false,
        turnAhead: Boolean = false,
        pileCast: Set<IntentTag> = emptySet(),
    ) {
        // "Target opponent gets a poison counter": the bound slot is a player, so the counter is a clock too.
        val playerSlots = reqs.filter { it is TargetPlayer || it is TargetOpponent }.mapNotNullTo(HashSet()) { it.id }
        val noIntent = { leaf: Effect ->
            needsNoIntent(leaf) || countsOnAPlayerSlot(leaf, playerSlots) ||
                // Damage confined to our creatures is a drawback, not offensive removal.
                // Polarity still reads the harm; simulation prices the damaged board.
                (leaf is DealDamageEffect && TargetPolarityAnalyzer.targetsOwnSide(leaf.target, reqs)) ||
                // So are harmful counters confined to our creatures (Defiant Greatmaw's two -1/-1).
                (leaf is AddCountersEffect && CardIntentAnalyzer.harmsOwnTarget(leaf.counterType, leaf.target, reqs)) ||
                (leaf is AddDynamicCountersEffect && CardIntentAnalyzer.harmsOwnTarget(leaf.counterType, leaf.target, reqs)) ||
                untapsOwnTarget(leaf, reqs) ||
                // A card exiled from our own graveyard is the price or fuel of what follows
                // (Patchwork Crawler, Trove Warden), not an answer.
                (leaf is MoveToZoneEffect &&
                    CardIntentAnalyzer.graveyardMoveOf(leaf, reqs) == CardIntentAnalyzer.GraveyardMove.OWN_EXILE) ||
                // Exiling a permanent we control (Identity Echo's polymorph) is the price of what
                // follows, like a sacrifice of our own choosing — no answer to anything.
                (leaf is MoveToZoneEffect && CardIntentAnalyzer.removesOurOwnPermanent(leaf, reqs))
        }
        val tags = CardIntentAnalyzer.analyzeEffect(root, reqs, turnAhead = turnAhead, ownPile = ownPile, pileCast = pileCast).tags
        // Card selection alone doesn't read an ability: scry is a rider on half the cards that
        // carry it, so a scry beside an unread payoff must not hide that payoff from the audit.
        if ((tags - IntentTag.CARD_SELECTION).isNotEmpty()) {
            sink += Finding(Check.INTENT, read = true, reason = "knows what it's for: ${tags.sorted().joinToString(", ", transform = ::tagPhrase)}")
            return
        }
        val unread = "no intent: the AI doesn't know what this effect is for"
        val before = sink.findings.size
        // A delayed trigger is read as the effect it fires (CardIntentAnalyzer), so an unread one is
        // labelled by that effect — the step it schedules, not the scheduling. Its price ("sacrifice
        // it at end step") needs no intent, like a gate's cost: EffectWalker.firedLeaves drops it.
        val leaves = fired(EffectWalker.payoffLeaves(root))
        val blight = ownBlightStages(leaves)
        val unreadLeaves = leaves.filterNot { noIntent(it) || blight.any { stage -> stage === it } }
        labels(unreadLeaves.filterNot { it is ForEachEffect }).filterNot { noIntentPipeline(it.first, unreadLeaves) }.forEach { (label, leaf) ->
            sink += gap(Check.INTENT, unread, label, step(leaves, leaf))
        }
        for (each in unreadLeaves.filterIsInstance<ForEachEffect>()) {
            // A loop can rebind target slots; the outer own-side filter proves nothing here.
            // Over the targets, though, `ContextTarget(0)` is each declared target in turn, so an
            // untap of it is our own permanent when every declaration is on our side.
            val ownTargets = each.space == IterationSpace.Targets && reqs.isNotEmpty() && reqs.all(TargetPolarityAnalyzer::isOwnSide)
            // "Put a loyalty counter on each planeswalker you control" (Way of the Mentor, Mila) is the
            // own-source loyalty counter spread over our side: no target, nothing to hold, and the
            // loyalty it adds is on the board the simulation prices when it next activates them.
            val ownGroup = (each.space as? IterationSpace.Group)?.filter?.baseFilter?.controllerPredicate ==
                ControllerPredicate.ControlledByYou
            // Over one declared slot, `ContextTarget(0)` is each pick of that slot, so the body reads
            // against it as the ability's own leaves do (Steward of the Harvest exiling our own land cards).
            val oneSlot = each.space == IterationSpace.Targets && reqs.size == 1
            val body = fired(EffectWalker.payoffLeaves(each.body)).filterNot {
                (oneSlot && noIntent(it)) || needsNoIntent(it) || countsOnAPlayerSlot(it, playerSlots) ||
                    (ownTargets && it is TapUntapEffect && !it.tap && it.target is EffectTarget.ContextTarget) ||
                    (ownGroup && it is AddCountersEffect && it.counterType == CounterType.LOYALTY &&
                        it.target == EffectTarget.IterationEntity) ||
                    // "Untap each other Merfolk you control" (Deepway Navigator), "untap all nonland
                    // permanents you control" (Dramatic Reversal): [untapsOwnTarget] over a group of
                    // ours — worth what tapping them again buys, which the simulation sees on the board.
                    (ownGroup && it is TapUntapEffect && !it.tap && it.target == EffectTarget.IterationEntity)
            }
            // Over each player, "you" in the body is the player iterated (ControllerOfTriggeringEntity's
            // land for Confounding Conundrum), so an own-permanents return is only ours over Players(You).
            val players = (each.space as? IterationSpace.Players)?.players
            val ownSide = players == null || players == Player.You
            // Over the owners of the source's exile pile, a pipeline out of that pile hands each owner
            // their own cards back (Induced Amnesia's "return the exiled cards to their owner's hand"):
            // the release half of an exile already read where it was made — no target, nothing to time.
            val releasesThePile = players == Player.OwnersOfLinkedExile
            labels(body).filterNot {
                it.first in NO_INTENT_PIPELINES || (ownSide && noIntentPipeline(it.first, body)) ||
                    (releasesThePile && it.first.startsWith(LINKED_EXILE_PIPELINE))
            }.forEach { (label, _) ->
                sink += gap(Check.INTENT, unread, "ForEach>$label", step(leaves, each))
            }
        }
        // Every untagged step needed no tag: say so, so the ability reads as understood rather
        // than as one nobody asked about.
        if (sink.findings.size == before && tags.isNotEmpty()) {
            sink += Finding(Check.INTENT, read = true, reason = "knows what it's for: ${tags.sorted().joinToString(", ", transform = ::tagPhrase)}")
        } else if (sink.findings.size == before) {
            sink += Finding(Check.INTENT, read = true, reason = "needs no intent: it only grows or puts counters on, shifts the P/T of, untaps, attaches, moves, transforms or animates or sacrifices its own source or permanents of its choosing, adds counters for its controller or removes them from itself, moves counters of any kind or removes them from a target, damages a player, makes one lose life or sets their life total, mills one, returns its controller's own permanents to hand, damages only its controller's permanents, exiles cards from its controller's own graveyard, is an unless-clause its payer suffers as an upkeep cost or life loss, or hands its exile pile back to the cards' owners")
        }
    }

    private fun combat(ability: ActivatedAbility, sink: Sink) {
        val leaves = EffectWalker.leaves(ability.effect)
        for (leaf in leaves) {
            val parts = if (leaf is ForEachEffect) EffectWalker.leaves(leaf.body).map { "ForEach>" to it } else listOf("" to leaf)
            for ((prefix, part) in parts) {
                if (!combatShaped(part)) continue
                if (CombatReach.of(part).isEmpty()) {
                    sink += gap(Check.COMBAT, "combat can't see this ${combatNoun(part)}", prefix + label(part), step(leaves, leaf))
                } else {
                    sink += Finding(
                        Check.COMBAT, read = true, reason = "combat sees this ${combatNoun(part)}",
                        effectType = prefix + label(part), step = step(leaves, leaf)?.index?.plus(1), stepText = stepText(leaf),
                    )
                }
            }
        }
    }

    /** Report each top-level until-end-of-turn leaf of [effect], read when [isRead] reads it. */
    private fun timing(effect: Effect, sink: Sink, isRead: (Effect) -> Boolean) {
        val leaves = EffectWalker.leaves(effect)
        // "If that creature would die this turn, exile it instead" rides on removal (Essence Burn,
        // Scorchmark, Bleed Dry): the creature dies as the effect resolves, so the rider's turn is
        // spent the moment it is cast and has no window of its own to wait for. When to cast it is
        // the removal's question, which the removal readers answer.
        val removal by lazy { IntentTag.REMOVAL in CardIntentAnalyzer.analyzeEffect(effect).tags }
        for (leaf in leaves) {
            val expiring = if (leaf is ForEachEffect) EffectWalker.leaves(leaf.body).firstOrNull(::endsThisTurn) else leaf.takeIf(::endsThisTurn)
            expiring ?: continue
            val label = if (leaf is ForEachEffect) "ForEach>${label(expiring)}" else label(leaf)
            // "Return target creature to its owner's hand. You may have Shapeshifters you control
            // become copies of that creature until end of turn" (Absorb Identity): the copies ride
            // on the bounce in the same way, so when to cast it is the removal's question too.
            // "A creature dealt damage this way can't block this turn" (Ballista Wielder, Blood
            // Aspirant) rides on the damage the same way: when to ping is the removal's question.
            // "… loses all abilities until end of turn. Target creature you control deals damage equal
            // to its power to that permanent" (Flourishing Grapple): the strip clears the way for the
            // damage as the spell resolves, so it too is spent on the removal's timing.
            val ridesOnRemoval = (leaf is MarkExileOnDeathEffect || expiring is EachPermanentBecomesCopyOfTargetEffect ||
                expiring is CantBlockEffect || expiring is CantAttackEffect || expiring is RemoveAllAbilitiesEffect) && removal
            // "This land becomes a copy of target creature you control until end of turn. The legend
            // rule doesn't apply to permanents you control this turn" (Hall of Echoes): the waiver only
            // keeps a copied legend alive beside its original, so it lasts exactly as long as the copy
            // it rides on, and when to spend it is the copy's question, which the copy's reading answers.
            val ridesOnCopy = expiring is GrantStaticAbilityEffect && expiring.ability is LegendRuleDoesNotApplyTo &&
                leaves.any { it is EachPermanentBecomesCopyOfTargetEffect && isRead(it) }
            // "Until end of turn, whenever you tap a Mountain for mana, add an additional {R}" (High
            // Tide, Molten Tide) granted to its caster: the mana is spent in the window it is cast in,
            // on the spell it makes affordable, and the cast's simulation sees that spell become
            // castable. Cast with nothing to spend it on, it changes nothing and is passed. No window
            // to wait for.
            val spentWithCast = expiring is GrantStaticAbilityEffect && expiring.ability is AdditionalManaOnSourceTap &&
                expiring.target == EffectTarget.Controller
            if (isRead(leaf) || ridesOnRemoval || ridesOnCopy || spentWithCast) {
                sink += Finding(
                    Check.TIMING, read = true, reason = "knows when to spend this until-end-of-turn effect",
                    effectType = label, step = step(leaves, leaf)?.index?.plus(1), stepText = stepText(leaf),
                )
            } else {
                sink += gap(Check.TIMING, "no timing for this until-end-of-turn effect", label, step(leaves, leaf))
            }
        }
    }

    private fun modal(root: Effect, sink: Sink) {
        for (modal in EffectWalker.leaves(root).filterIsInstance<ModalEffect>()) {
            val extra = modal.additionalCostPerExtraMode ?: continue
            if (modal.chooseCount <= 1 && modal.dynamicChooseCount == null) continue
            if (escalateHasPicker(extra)) {
                sink += Finding(Check.MODAL, read = true, reason = "can pay for extra modes and choose more than one", effectType = label(extra))
            } else {
                sink += Finding(
                    Check.MODAL, read = false,
                    reason = "only ever chooses one mode: no way to pay the extra-mode cost",
                    effectType = label(extra), flag = Flag(Check.MODAL, "escalate>${label(extra)}"),
                )
            }
        }
    }

    // ── Findings ────────────────────────────────────────────────────────────

    /** An unread part, carrying the [Flag] the corpus audit counts. */
    private fun gap(check: Check, reason: String, effectType: String, step: IndexedValue<Effect>?, target: Int? = null, mode: Int? = null) =
        Finding(
            check = check, read = false, reason = reason, effectType = effectType,
            step = step?.index?.plus(1), stepText = step?.value?.let(::stepText),
            target = target, mode = mode, flag = Flag(check, effectType),
        )

    /** [leaf]'s position among [leaves], by identity — two equal leaves are still two steps. */
    private fun step(leaves: List<Effect>, leaf: Effect?): IndexedValue<Effect>? {
        leaf ?: return null
        val i = leaves.indexOfFirst { it === leaf }.takeIf { it >= 0 } ?: leaves.indexOf(leaf).takeIf { it >= 0 } ?: return null
        return IndexedValue(i, leaves[i])
    }

    private fun stepText(leaf: Effect): String? = describe { leaf.description }.takeIf { it.isNotBlank() }

    /** A description that cannot take the reading down with it — some render from runtime state. */
    private inline fun describe(text: () -> String): String = runCatching(text).getOrDefault("").trim()

    private fun polarityPhrase(polarity: TargetPolarity): String = when (polarity) {
        TargetPolarity.HARMFUL -> "reads as harmful — aimed at an opponent's side"
        TargetPolarity.BENEFICIAL -> "reads as helpful — aimed at your own side"
        TargetPolarity.FLEXIBLE -> "can help either side; compare targets by simulation"
        TargetPolarity.UNKNOWN -> "can't tell if this helps or hurts"
    }

    private fun tagPhrase(tag: IntentTag): String = when (tag) {
        IntentTag.EXILE_REMOVAL -> "exile removal"
        IntentTag.TOKEN_MAKER -> "makes tokens"
        IntentTag.COMBAT_TRICK -> "combat trick"
        IntentTag.EVASION_GRANT -> "grants evasion"
        IntentTag.COMBAT_ENHANCEMENT -> "makes a creature better in combat"
        IntentTag.EQUIPMENT_RELOCATION -> "moves Equipment onto a creature"
        IntentTag.PERMANENT_COPY -> "turns a permanent into a copy of another"
        IntentTag.SPELL_REDIRECT -> "changes the target of a spell or ability"
        IntentTag.SPELL_COPY -> "copies a spell or ability"
        IntentTag.GRAVEYARD_HATE -> "exiles cards from a graveyard"
        IntentTag.SACRIFICE_OUTLET -> "sacrifice outlet"
        IntentTag.LIFEGAIN -> "life gain"
        IntentTag.TAPPER -> "taps things"
        IntentTag.CAST_BAN -> "stops opponents casting spells"
        IntentTag.SPELL_TAX -> "makes opponents' spells cost more"
        IntentTag.FORCED_ATTACK -> "makes an opponent's creatures attack"
        IntentTag.MILL -> "mills opponents toward running out of cards"
        IntentTag.TRIBAL_ENABLER -> "grants a chosen creature type to enable tribal payoffs"
        IntentTag.DAMAGE_AMPLIFIER -> "amplifies damage from our sources"
        IntentTag.DAMAGE_TO_PLAYERS -> "damages or drains players"
        IntentTag.TRIGGER_LOCK -> "stops permanents entering or dying from triggering abilities"
        IntentTag.COUNTER_LOCK -> "stops counters being put on players or permanents"
        IntentTag.TOKEN_THEFT -> "takes the tokens opponents would create"
        IntentTag.GAME_WIN -> "makes an opponent lose the game outright"
        IntentTag.EXTRA_TURN -> "takes an extra turn"
        IntentTag.FREE_SPELL -> "casts a spell without paying its mana cost"
        IntentTag.COST_REDUCER -> "makes your spells or abilities cheaper"
        else -> tag.name.lowercase(Locale.ROOT).replace('_', ' ')
    }

    private fun combatNoun(leaf: Effect): String {
        val name = label(leaf)
        return when {
            name.contains("Counters") || name.contains("Stats") || name.contains("PowerToughness") -> "pump"
            name.contains("Block") -> "blocking change"
            name.contains("Animate") || name.contains("BecomeCreature") -> "animation"
            else -> "keyword grant"
        }
    }

    // ── Shapes ──────────────────────────────────────────────────────────────

    /** What an unread Aura does to its host: its static abilities and its triggers' leaves. */
    private fun auraFlags(script: CardScript): List<String> {
        val parts = script.staticAbilities.map { "Aura>${it::class.simpleName}" } +
            script.triggeredAbilities.flatMap { t -> EffectWalker.leaves(t.effect).map { "Aura>${label(it)}" } } +
            script.activatedAbilities.filterNot { it.isManaAbility }
                .flatMap { a -> EffectWalker.leaves(a.effect).map { "Aura>${label(it)}" } }
        return parts.distinct().ifEmpty { listOf("Aura>(nothing on the host)") }
    }

    /** Whether [leaf] names target slot [index] — by the requirement's bound name or its position. */
    private fun names(leaf: Effect, req: TargetRequirement, index: Int): Boolean {
        val text = leaf.toString()
        // A count-N requirement's picks are `id[0]`, `id[1]`, …
        return (req.id != null && ("BoundVariable(name=${req.id})" in text || "BoundVariable(name=${req.id}[" in text)) ||
            "ContextTarget(index=$index)" in text
    }

    /**
     * The leaves that need no intent tag: equip / attach, a card moving, untapping, transforming, animating, sacrificing or setting the base P/T of itself, untargeted proliferate, damage to a
     * player and life loss (commented inline below), and a counter the effect puts on its own source or its own
     * controller — a creature growing itself, a charge, oil, quest or loyalty counter, `{E}` for you. There is no target to choose and nothing
     * to hold it for, and its payoff is already on the board (the evaluator reads a grown creature's
     * projected stats) or belongs to the ability that later spends it, so it needs no intent tag. That
     * holds for a keyword counter on its own source too: unlike an until-end-of-turn
     * [com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect] it lasts, so it has no window, and the
     * simulation spends a protective one against removal on the stack. A keyword counter on another
     * permanent stays flagged unless the reader tags it.
     */
    private fun fired(payoff: List<Effect>): List<Effect> = payoff.flatMap { leaf ->
        when (leaf) {
            is CreateDelayedTriggerEffect -> fired(EffectWalker.firedLeaves(leaf, payoff))
            // A reflexive trigger is read as the effect it fires, as CardIntentAnalyzer reads it: an
            // unread one is labelled by its payoff, and a payoff needing no intent reads it. The
            // action before "when you do" is left out, as the analyzer leaves it out: an optional one
            // is the payoff's price, like a gate's. A mandatory one (Etched Slith's +1/+1 counter, a
            // connive) is a real effect neither reads yet; reading it in the analyzer would let its tag
            // stand for the whole ability and hide an unread payoff (Obscura Interceptor's bounce).
            is ReflexiveTriggerEffect -> fired(EffectWalker.payoffLeaves(leaf.reflexiveEffect))
            else -> listOf(leaf)
        }
    }

    private fun needsNoIntent(leaf: Effect): Boolean {
        // Equip / fortify / reconfigure moves the source onto a host and does nothing of its own:
        // what the Equipment is *for* is the card's reading of its "equipped creature" statics
        // (CardIntentAnalyzer tags those), and which creature gets it is the polarity check.
        if (leaf is AttachEquipmentEffect) return true
        // "Target creature gains all creature types" (Oko, Lorwyn Liege's +2, Amoeboid Changeling) —
        // Changeling granted: its payoff is whatever typal effects then see the creature, which the
        // simulation prices on the board; which creature gets it is the polarity check's (a gift).
        if (leaf is GrantKeywordEffect && leaf.keyword == Keyword.CHANGELING.name) return true
        // "Each opponent may search their library for a basic land card …" (Old-Growth Dryads): an
        // opponents-only iteration whose body is only their gain is the card's drawback, with
        // nothing to aim or hold; the rater prices it as one.
        if (leaf is ForEachEffect && CardIntentAnalyzer.giftsOnlyToOpponents(leaf)) return true
        // A card moving itself — "return this card to its owner's hand" when it dies, ninjutsu,
        // "shuffle it into its owner's library" — has no target to aim and is no spell to time:
        // whether it is worth doing is the simulation's call on the activation or trigger.
        if (leaf is MoveToZoneEffect && leaf.target == EffectTarget.Self) return true
        // A permanent untapping itself (a mana rock untapping for more, a boast's "untap it") has no
        // target and nothing to hold: the untap is worth what tapping it again buys (another mana
        // ability, or a blocker after it attacked), which the simulation already sees on the board.
        // Untapping a *chosen* target stays flagged unless our side can fill it ([untapsOwnTarget]) or it is a land (ramp).
        if (leaf is TapUntapEffect && !leaf.tap && leaf.target == EffectTarget.Self) return true
        // A permanent transforming itself (a werewolf, a DFC's "transform this") has no target and is
        // no spell to time: its payoff is the face it becomes, which the simulation sees on the board.
        // Transforming another permanent stays flagged.
        if (leaf is TransformEffect && leaf.target == EffectTarget.Self) return true
        if (leaf is ReturnSelfFromExileTransformedEffect) return true
        // "Exile this, then return it transformed" (the FIN eikons, Conqueror's Galleon's end-of-combat
        // flip into a land) is the same self-transform as a new object: no target, nothing to time.
        if (leaf is ExileAndReturnTransformedEffect && leaf.target == EffectTarget.Self) return true
        // A permanent animating itself (a manland, "this artifact becomes a 3/3 until end of turn") has
        // no target and is no spell to hold: when to activate it is the timing check's question and
        // that it can block is CombatReach's; what it is worth is the creature the simulation then sees.
        // Animating another permanent stays flagged.
        if (leaf is BecomeCreatureEffect && leaf.target == EffectTarget.Self) return true
        // A permanent sacrificing itself has no target: on a trigger it is a drawback its controller
        // doesn't choose (Ball Lightning's end step, Glimmervoid with no artifacts, Skittering Horror);
        // on an activation its payoff is the permanent's own leaves trigger (Hopeful Vigil's scry) or
        // the board without its statics (Festival of Embers). Whether and when to activate it, in
        // response to removal too, is the simulation's call; no intent tag would change it.
        // Sacrificing another stays flagged.
        if (leaf == SacrificeSelfEffect) return true
        // Its controller sacrificing permanents of its own choosing — Lotus Field's two lands, Korvold's
        // "sacrifice another permanent", forage's Food — is the same price with the pick left to
        // resolution: no target, nothing to time, and what is lost is the board the simulation sees.
        // An edict aimed at another player is a ForceSacrificeEffect and reads as removal.
        if (leaf is SacrificeEffect) return true
        // "Forage" happened: the marker a forage's payment emits for "whenever you forage" triggers.
        // Bookkeeping, never a payoff of its own; the "if you do" rider beside it is read on its own leaf.
        if (leaf == ForagedEffect) return true
        // "Exile that card with a dream counter on it instead of putting it into your graveyard"
        // (Goliath Daydreamer, All Hallow's Eve): where our own resolving spell goes, marked for another
        // ability of the card that later spends the pile. No target, nothing to time; that ability is read on its own.
        if (leaf is MarkSpellExileWithCountersEffect) return true
        // Its controller losing the game (Phage the Untouchable entering from anywhere but the hand)
        // is the card's price, never a play: no target to aim, nothing to time, and no tag would
        // make the AI choose it. An opponent losing is GAME_WIN and never reaches here.
        if (leaf is LoseGameEffect && (leaf.target == EffectTarget.Controller || leaf.target == EffectTarget.PlayerRef(Player.You))) return true
        // A creature shifting its own P/T with a loss on either side — Flowstone's "+1/-1", Morphling's
        // "+1/-1", Blind Creeper's "-1/-1" drawback — has no target and is no trick to hold: it is a
        // trade on its own body the simulation prices on the board. A pure self pump already reads
        // PUMP; a target's shift, and a self shift with no side known to be a loss, stay flagged.
        if (leaf is ModifyStatsEffect && leaf.target == EffectTarget.Self &&
            (AmountSign.of(leaf.powerModifier) == AmountSign.LOSS || AmountSign.of(leaf.toughnessModifier) == AmountSign.LOSS)
        ) return true
        // A permanent setting its own base P/T (Bramblefort Fink's 10/10, Master of Winds' 4/1 or
        // 1/4, Surge Engine's 5/4, Ascendant Spirit's levels) has no target and is no trick to hold:
        // when to activate it is the timing check's question, and what it is worth is the body the
        // simulation then sees. Setting a target's base stays flagged (a gift or a curse by creature).
        if (leaf is SetBaseStatsEffect && leaf.target == EffectTarget.Self) return true
        // A permanent stripping its own abilities — Retched Wretch's "return it to the battlefield
        // … and it loses all abilities" — has no target and nothing to hold: the price of its own
        // return, and what is left is the vanilla body the simulation sees. Stripping another stays flagged.
        if (leaf is RemoveAllAbilitiesEffect && leaf.target == EffectTarget.Self) return true
        // Damage to a player is a clock: the evaluator prices the life it takes on resolution, it
        // answers nothing on the board, and life lost now or one window later is the same life.
        // Which player a targeted one aims at is the polarity check's question, not intent's.
        if (leaf is DealDamageEffect && isPlayerTarget(leaf.target)) return true
        // The same clock counted per card left in a zone (Dragonhawk's "2 damage to each opponent
        // for each of those cards that are still exiled"): damage to a player, priced on resolution.
        if (leaf is DealDamagePerEntityInZoneEffect && isPlayerTarget(leaf.target)) return true
        // Life loss always lands on a player, so it is the same clock — or, on "you lose N life",
        // a drawback the simulation already prices on resolution. Either way there is nothing to tag.
        if (leaf is LoseLifeEffect) return true
        // "Your life total becomes 15" (Form of the Dinosaur), "becomes half your starting life
        // total", "the defending player's life total becomes 1" (Master of Cruelties): a life total
        // set is a gain or a loss by the total it replaces (rule 119.5), so it is the same clock or
        // drawback as the two above, and the evaluator prices the new total on resolution. Whether
        // a targeted one aims at the right player is the polarity check's question, not intent's.
        if (leaf is SetLifeTotalEffect) return true
        // "Damage can't be prevented this turn" (Insult, Skullcrack's rider) has no target and is a
        // rider on the damage beside it: what it buys is the damage the simulation already sees land.
        if (leaf is DamageCantBePreventedThisTurnEffect) return true
        // "Lands you control don't untap during your next untap step" (Hour of Devastation's god
        // spells: Bontu's Last Reckoning, Oketra's Last Mercy) binds its own controller: the price of
        // the card's payoff, with no target to aim and nothing to hold. Skipping an opponent's untap
        // (Exhaustion, Mana Vapors) is a tempo payoff and stays flagged.
        if (leaf is SkipUntapEffect && (leaf.target == EffectTarget.Controller || leaf.target == EffectTarget.PlayerRef(Player.You))) return true
        // A cast ban that binds its controller too — "you can't cast spells this turn" (Conduit of
        // Worlds), "each player can't cast noncreature spells this turn" (Calamity's Wake) — is the
        // price of the card's payoff, not its point. A ban on an opponent reads CAST_BAN.
        if (leaf is CantCastSpellsEffect && !CardIntentAnalyzer.bansAnOpponent(leaf.target)) return true
        // "Sacrifice it unless you pay {1}{U}" (an upkeep cost, a land's "sacrifice it unless you pay {1}") is a drawback its
        // controller chooses to pay or take: no target, nothing to hold, and whether to pay is the
        // simulation's call when the trigger resolves. A suffer that needs no intent of its own (life
        // loss, damage to a player) needs none behind an "unless" either, whoever pays. An opponent
        // paying to stop *our* payoff (Rhystic Study's draw) stays flagged.
        if (leaf is PayOrSufferEffect) {
            val paidByController = leaf.player == EffectTarget.Controller || leaf.player == EffectTarget.PlayerRef(Player.You)
            // Scry/surveil needsNoIntent only because CardIntentAnalyzer reads them, and nothing reads inside a suffer.
            val suffer = EffectWalker.payoffLeaves(leaf.suffer)
            val blight = if (paidByController) ownBlightStages(suffer) else emptyList()
            return suffer.all {
                (it !is ScryEffect && it !is SurveilEffect && needsNoIntent(it)) || (paidByController && harmsOnlyItsSource(it)) ||
                    blight.any { stage -> stage === it }
            }
        }
        // Untargeted proliferate has no target to aim and no consistent window to hold it for (+1/+1
        // counters want precombat, poison or loyalty don't care): which permanents and players get another
        // counter is chosen at resolution, and the counters it adds are on the board the simulation prices.
        // A targeted one ("each kind of counter on target permanent") stays flagged.
        if (leaf is ProliferateEffect && leaf.target == null) return true
        // Its controller venturing into the dungeon (Planar Ally's attack, Clattering Skeletons' ETB, Dungeon
        // Map) has no target to aim and no window to hold it for: which dungeon and room is chosen at
        // resolution, and the room's own trigger (a scry, a Treasure, a draw) is what the simulation prices.
        // A venture by another player stays flagged.
        if (leaf is VentureIntoTheDungeonEffect && (leaf.target == EffectTarget.Controller || leaf.target == EffectTarget.PlayerRef(Player.You))) return true
        // The Ring tempting its controller (Took Reaper's death, Call of the Ring's upkeep) has no target to aim
        // and no window to hold it for: the Ring-bearer is chosen at resolution, and what the Ring then does is
        // read where it acts (CombatMath's evasion), not by an intent tag. The Ring tempting another player stays flagged.
        if (leaf is TheRingTemptsYouEffect && (leaf.target == EffectTarget.Controller || leaf.target == EffectTarget.PlayerRef(Player.You))) return true
        // Scry and surveil are read (card selection); skipped here only so a sibling leaf is still asked about.
        if (leaf is ScryEffect || leaf is SurveilEffect) return true
        // A permanent exploring itself (Jadelight Ranger) is read as card selection, and its +1/+1
        // counter is self-growth, which needs no tag: skipped so a sibling leaf is still asked about.
        // A target creature exploring reads as a pump too and never gets here.
        if (leaf is ExploreEffect && leaf.target == EffectTarget.Self) return true
        // A permanent removing counters from itself (a time/oil/charge tick, Clockwork Beast's
        // +1/+0 wearing off, a -1/-1 counter shed) has no target and nothing to hold: it is
        // bookkeeping or a grow the simulation sees on the board. Removing them from another stays flagged.
        if (leaf is RemoveCountersEffect && leaf.target == EffectTarget.Self) return true
        // "Remove all -1/-1 counters from this creature" (Ammit Eternal) is the same shed, every
        // counter of the kind at once: a recovery the simulation sees as the restored P/T.
        if (leaf is RemoveAllCountersOfTypeEffect && leaf.target == EffectTarget.Self) return true
        // Counters moved between permanents or taken off one, whatever their kind — The Ozolith
        // storing a leaving creature's counters and moving them on, Essence Channeler's "put its
        // counters on target creature you control", Etched Slith's "remove a counter from another
        // target permanent or opponent". No counter is made: what the move or removal is worth is the
        // counters' kinds on the board (+1/+1 and loyalty help their holder, -1/-1 and stun hurt it),
        // which no fixed tag can say. The polarity check reads the slot FLEXIBLE so the simulation
        // weighs each candidate's counters, and whether to do it is the simulation's call too.
        // Unlike a RemoveCountersEffect of a named kind on another target (read by its kind: a -1/-1
        // counter removed is a pump; any other kind stays flagged), these name no kind, so a target
        // does not make them taggable.
        // So do the steps of a loop over the kinds already on a permanent — "for each kind of counter
        // on target permanent, put another counter of that kind on it or remove one from it"
        // (Dramatist's Puppet, Quarry Hauler): the kind is whatever the iteration lands on.
        if (leaf is MoveAllCountersEffect || leaf is MoveAllLastKnownCountersEffect ||
            leaf is RemoveAnyNumberOfCountersEffect || leaf is RemoveCountersOfIteratedKindEffect ||
            (leaf is AddCountersOfChosenKindEffect && leaf.kindFrom == CounterKindSource.ITERATION)
        ) return true
        // "Remove any number of counters from among creatures you control" (Galloping Lizrog) takes
        // counters off its controller's own permanents to pay for a payoff beside it (the doubled
        // counters, read on their own leaf): no target, nothing to hold, and how many to move is the
        // DistributeDecision answered at resolution (the responders fill every share, i.e. move every
        // eligible counter, which suits a doubling payoff). From among others' stays flagged.
        if (leaf is RemoveCountersFromAmongEffect &&
            leaf.filter.controllerPredicate == ControllerPredicate.ControlledByYou
        ) return true
        val target = when (leaf) {
            is AddCountersEffect -> leaf.target
            is AddDynamicCountersEffect -> leaf.target
            // "Double the +1/+1 counters on this creature" (Dragonsguard Elite) grows its own source.
            is DoubleCountersEffect -> leaf.target
            // Adapt (Sharktocrab, Pteramander, Incubation Druid) is a +1/+1 counter on its own source
            // when it has none: the same self-growth as a plain counter on Self, with no target to aim;
            // when to activate it, mid-combat too, is the simulation's call.
            is AdaptEffect -> leaf.target
            else -> return false
        }
        // A counter on a player (poison, rad, energy) is a clock or a resource, like life loss: no
        // permanent to answer, nothing to hold, and which player gets it is the polarity check's question.
        if (isPlayerTarget(target)) return true
        // A keyword counter on its own source (a Dominus's indestructible counter, Mai's double strike,
        // Acrobatic Cheerleader's flying) grants a keyword that lasts, so there is no window to hold it
        // for and no target to aim: answering removal with it is the simulation's call, which it makes.
        return target == EffectTarget.Self
    }

    /**
     * Untapping a target our own side can fill ("untap target creature you control", Dauntless
     * Aven; "untap another target creature", Samut, Voice of Dissent) needs no intent for the reason
     * untapping the source itself doesn't (see [needsNoIntent]): the untap is worth what tapping that
     * permanent again buys — another attack's blocker, another activation — which the simulation
     * sees on the board. An untap reads as a gift (TargetPolarityAnalyzer), so a slot open to either
     * side is aimed at our own permanents, never handed to the opponent. Untapping a target land
     * reads as ramp (CardIntentAnalyzer); an untap confined to an opponent's permanents stays flagged.
     */
    private fun untapsOwnTarget(leaf: Effect, reqs: List<TargetRequirement>): Boolean =
        leaf is TapUntapEffect && !leaf.tap && TargetPolarityAnalyzer.ownSideCanFill(leaf.target, reqs)

    /** A counter placed on a bound target slot that only a player can fill (see [needsNoIntent]'s player counters). */
    private fun countsOnAPlayerSlot(leaf: Effect, playerSlots: Set<String>): Boolean {
        val target = when (leaf) {
            is AddCountersEffect -> leaf.target
            is AddDynamicCountersEffect -> leaf.target
            else -> return false
        }
        return (target as? EffectTarget.BoundVariable)?.name in playerSlots
    }

    /** An additional cost that exiles cards as it is paid — what a leaves trigger can later hand back. */
    private fun exilesAsCost(cost: AdditionalCost): Boolean = when (cost) {
        is AdditionalCost.Composite -> cost.steps.any(::exilesAsCost)
        is AdditionalCost.ExileFromStorage -> cost.linkToSource
        is AdditionalCost.Atom -> cost.atom is CostAtom.ExileFrom
        else -> false
    }

    private fun movesToHand(effect: Effect): Boolean = EffectWalker.leaves(effect).any { leaf ->
        leaf is MoveCollectionEffect && (leaf.destination as? CardDestination.ToZone)?.zone == Zone.HAND
    }

    /** A leaf that only sacrifices, taps or phases out its own source — the "suffer" of an upkeep cost. */
    /**
     * The stages of a blight of our own — "blight 1: put a -1/-1 counter on a creature you control"
     * (Shadow Urchin's attack trigger, Chaos Spewer's unpaid ETB): a gather of our creatures, our
     * pick of one, and -1/-1 counters on it. Like a sacrifice of our own choosing ([SacrificeEffect]
     * in [needsNoIntent]) it is a price with no target and nothing to time, and which creature
     * wears the counters is the simulation's pick at resolution. An opponent's blight (they choose,
     * among their creatures) is an answer and stays read as one.
     */
    private fun ownBlightStages(leaves: List<Effect>): List<Effect> {
        val ours = leaves.filterIsInstance<GatherCardsEffect>().filter { gather ->
            (gather.source as? CardSource.ControlledPermanents)?.player == Player.You
        }
        val picks = leaves.filterIsInstance<SelectFromCollectionEffect>().filter { select ->
            select.chooser == Chooser.Controller && ours.any { it.storeAs == select.from }
        }
        val counters = leaves.filterIsInstance<AddCountersToCollectionEffect>().filter { add ->
            add.counterType == CounterType.MINUS_ONE_MINUS_ONE && picks.any { it.storeSelected == add.collectionName }
        }
        if (counters.isEmpty()) return emptyList()
        val usedPicks = picks.filter { pick -> counters.any { it.collectionName == pick.storeSelected } }
        val usedGathers = ours.filter { gather -> usedPicks.any { it.from == gather.storeAs } }
        return usedGathers + usedPicks + counters
    }

    private fun harmsOnlyItsSource(leaf: Effect): Boolean = when (leaf) {
        SacrificeSelfEffect -> true
        is SacrificeTargetEffect -> leaf.target == EffectTarget.Self
        is TapUntapEffect -> leaf.target == EffectTarget.Self
        is PhaseOutEffect -> leaf.target == EffectTarget.Self
        else -> false
    }

    /** The targets [CardIntentAnalyzer] reads as a player rather than a permanent (its `hitsAnotherPermanent`). */
    private fun isPlayerTarget(target: EffectTarget): Boolean = when (target) {
        is EffectTarget.PlayerRef, EffectTarget.Controller, EffectTarget.TargetController,
        EffectTarget.ControllerOfTriggeringEntity, EffectTarget.ControllerOfDamageSource -> true
        else -> false
    }

    /**
     * Whether [leaf] of [spell] is an until-end-of-turn protective keyword grant (one creature's, or
     * the team's through a `ForEach` whose expiring leaves are all such grants) on a spell that also
     * does something lasting — a leaf with an intent reading that does not end this turn.
     */
    private fun protectionBesideLastingPayoff(spell: Effect, leaf: Effect): Boolean {
        fun protective(l: Effect) = l is GrantKeywordEffect && endsThisTurn(l) &&
            IntentTag.PROTECTION in CardIntentAnalyzer.analyzeEffect(l).tags
        val grant = if (leaf is ForEachEffect) {
            EffectWalker.leaves(leaf.body).filter(::endsThisTurn).let { it.isNotEmpty() && it.all(::protective) }
        } else {
            protective(leaf)
        }
        if (!grant) return false
        val all = EffectWalker.leaves(spell).flatMap { if (it is ForEachEffect) EffectWalker.leaves(it.body) else listOf(it) }
        return all.any { !endsThisTurn(it) && CardIntentAnalyzer.analyzeEffect(it).tags.isNotEmpty() }
    }

    /** Whether [effect] carries a `duration` of [Duration.EndOfTurn]. Effects share no interface for it. */
    private fun endsThisTurn(effect: Effect): Boolean = property(effect, "getDuration") == Duration.EndOfTurn

    /** P/T, keyword, P/T-counter or blocking changes — the shapes a combat simulation must see. */
    private fun combatShaped(leaf: Effect): Boolean {
        val name = label(leaf)
        if (name.contains("Counters")) return (property(leaf, "getCounterType") as? CounterType)?.ptDelta != null
        return COMBAT_SHAPED.containsMatchIn(name)
    }

    private val COMBAT_SHAPED = Regex("Stats|PowerToughness|Keyword|Evasion|Protection|Hexproof|Indestructib|Block|Animate|^BecomeCreatureEffect$")

    /** The escalate cost shapes `TargetSelection.payEscalateCost` can pay (its `costType` branches). */
    private fun escalateHasPicker(atom: CostAtom): Boolean = when (atom) {
        is CostAtom.Discard, is CostAtom.TapPermanents, is CostAtom.Sacrifice, is CostAtom.ReturnToHand -> true
        is CostAtom.ExileFrom -> atom.zone == Zone.GRAVEYARD
        else -> false
    }

    private fun property(target: Any, getter: String): Any? =
        target.javaClass.methods.firstOrNull { it.name == getter && it.parameterCount == 0 }
            ?.let { runCatching { it.invoke(target) }.getOrNull() }

    private fun label(value: Any): String = value::class.simpleName ?: value.javaClass.name

    /**
     * Effect-type labels for [leaves], each with the leaf it stands for, and with each
     * library/graveyard/hand **pipeline** read as one type. A pipeline ("gather the cards in your
     * graveyard, choose one, move it to your hand") is lowered to a gather, a selection, a move and
     * some bookkeeping; none of the stages means anything alone, so they are named together as
     * `Pipeline(<from>→<to>)` — read off [pipelineOf], the whole effect, when [leaves] is only part
     * of it — and stand for the first stage.
     */
    private fun labels(leaves: List<Effect>, pipelineOf: List<Effect> = leaves): List<Pair<String, Effect>> {
        val stages = leaves.filter { label(it) in PIPELINE_STAGES }
        val plain = leaves.filterNot { label(it) in PIPELINE_STAGES }.map { label(it) to it }
        val all = if (stages.isEmpty()) plain else plain + (pipelineLabel(pipelineOf) to stages.first())
        return all.distinctBy { it.first }
    }

    private fun pipelineLabel(effects: List<Effect>): String {
        val from = effects.filterIsInstance<GatherCardsEffect>().map { gather ->
            when (val source = gather.source) {
                is CardSource.FromZone -> source.zone.name.lowercase()
                else -> label(source)
            }
        }.distinct().sorted().ifEmpty { listOf("?") }
        val to = effects.filterIsInstance<MoveCollectionEffect>().map { move ->
            when (val destination = move.destination) {
                is CardDestination.ToZone -> destination.zone.name.lowercase()
                else -> label(destination)
            }
        }.distinct().sorted().ifEmpty { listOf("?") }
        return "Pipeline(${from.joinToString("|")}→${to.joinToString("|")})"
    }

    /**
     * Library-to-graveyard pipelines without an offensive mill reading remain fuel, not a
     * win-condition claim: self-mill and incidental discarded library cards need no intent.
     * Explicit opponent-capable mills now read MILL before this exemption is consulted.
     */
    private val NO_INTENT_PIPELINES = setOf("Pipeline(TopOfLibrary→graveyard)")

    /** The label prefix of a pipeline whose cards come out of the source's linked-exile pile. */
    private const val LINKED_EXILE_PIPELINE = "Pipeline(FromLinkedExile→"

    /**
     * Whether the pipeline labelled [label], made of [stages], needs no intent tag: one of
     * [NO_INTENT_PIPELINES], or a return of its controller's own permanents to hand or their exile. An own-side exile
     * is a drawback (Shaun takes back its tokens), not offensive removal. "Return a land
     * you control" (a karoo), "return a creature you control" (Whitemane Lion, Kazandu Stomper),
     * "return each other creature you control" (Denizen of the Deep) choose on resolution among
     * their own permanents: no target, a drawback or a re-buy of an enters trigger the simulation
     * prices on the board, and a flash rescue in response to removal is the simulation's call on
     * their spell (AmbushWindow leaves that window alone) — like a Restoration Angel blinking our
     * own creature. A bounce that can reach another player's permanents stays flagged.
     */
    private fun noIntentPipeline(label: String, stages: List<Effect>): Boolean {
        if (label in NO_INTENT_PIPELINES) return true
        // A look at our own library top that only puts cards back or into our graveyard (Sage Owl,
        // Lluwen) reads as card selection: skipped like a scry leaf, so a sibling leaf is still asked about.
        if (label.startsWith("Pipeline(TopOfLibrary→") && CardIntentAnalyzer.selectsOwnLibraryTop(stages)) return true
        // Cards exiled out of our own graveyard — forage's three, delve-like fuel (Specter of
        // Mortality) — are the price or fuel of what follows, as the single-card move is (see
        // [intent]'s OWN_EXILE). Playing them afterwards is a grant leaf, read and flagged on its own.
        if (label == "Pipeline(graveyard→exile)") {
            val sources = stages.filterIsInstance<GatherCardsEffect>().map { it.source }
            return sources.isNotEmpty() && sources.all { it is CardSource.FromZone && it.zone == Zone.GRAVEYARD && it.player == Player.You }
        }
        if (label != "Pipeline(BattlefieldMatching→hand)" && label != "Pipeline(BattlefieldMatching→exile)") return false
        val sources = stages.filterIsInstance<GatherCardsEffect>().map { it.source }
        return sources.isNotEmpty() && sources.all { source ->
            source is CardSource.BattlefieldMatching &&
                (source.player == Player.You || source.filter.controllerPredicate == ControllerPredicate.ControlledByYou)
        }
    }

    private val PIPELINE_STAGES = setOf(
        "GatherCardsEffect", "MoveCollectionEffect", "SelectFromCollectionEffect", "FilterCollectionEffect",
        "RevealCollectionEffect", "ShuffleLibraryEffect", "EmitLibrarySearchedEventEffect",
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Report
    // ─────────────────────────────────────────────────────────────────────────

    data class Group(val key: String, val cards: List<String>)

    data class Report(
        val totalCards: Int,
        val readFully: Int,
        /** Sorted by card count, descending, then key. */
        val groups: List<Group>,
        val chooseNModals: Int,
        val crewOrSaddle: Int,
        /** Every flagged card's flag keys, by card name — the per-card drill-down. */
        val flagsByCard: Map<String, List<String>>,
    ) {
        val fraction: Double get() = if (totalCards == 0) 1.0 else readFully.toDouble() / totalCards

        /**
         * The baseline-comparable form: one `key<TAB>value` line per number, groups sorted by key so
         * two runs diff line by line.
         */
        fun summaryTsv(): String = buildString {
            appendLine("# AI card-reading audit. Compare runs line by line; see docs/ai/reading-audit.md.")
            appendLine("cards\t$totalCards")
            appendLine("read-fully\t$readFully")
            appendLine("fraction\t${String.format(Locale.ROOT, "%.4f", fraction)}")
            appendLine("info.choose-n-modal\t$chooseNModals")
            appendLine("info.crew-or-saddle\t$crewOrSaddle")
            for (g in groups.sortedBy { it.key }) appendLine("group\t${g.key}\t${g.cards.size}")
        }

        fun markdown(examples: Int = 5): String = buildString {
            appendLine("# AI card-reading audit")
            appendLine()
            appendLine("Cards audited: $totalCards. Read fully: $readFully (${String.format(Locale.ROOT, "%.1f", fraction * 100)}%).")
            appendLine("Choose-N modal cards (read, informational): $chooseNModals. Crew/saddle cards (filled, informational): $crewOrSaddle.")
            appendLine()
            appendLine("A card lands in every group it has a flag in. Groups sorted by card count.")
            appendLine()
            appendLine("| # | Group | Cards | Examples |")
            appendLine("|---|-------|------:|----------|")
            groups.forEachIndexed { i, g ->
                appendLine("| ${i + 1} | `${g.key}` | ${g.cards.size} | ${g.cards.take(examples).joinToString(", ")} |")
            }
        }

        /** One line per flagged card: `name<TAB>key; key; …`, sorted by name. */
        fun cardsTsv(): String = buildString {
            for ((name, keys) in flagsByCard) appendLine("$name\t${keys.joinToString("; ")}")
        }

        fun headline(top: Int = 10): String = buildString {
            appendLine("AI reading audit: $readFully / $totalCards cards read fully (${String.format(Locale.ROOT, "%.1f", fraction * 100)}%), ${groups.size} groups")
            groups.take(top).forEach { appendLine("  %5d  %s".format(it.cards.size, it.key)) }
        }
    }

    /**
     * Every card in [cards] (deduplicated by name), sorted by name, mapped to its distinct flag keys —
     * empty for a card read fully. [report] and `AiReadingRatchetTest` both build on this.
     */
    fun cardFlags(cards: List<CardDefinition>, lookup: CardLookup): Map<String, List<String>> =
        cards.distinctBy { it.name }.sortedBy { it.name }
            .associateTo(linkedMapOf()) { card -> card.name to audit(card, lookup).map { it.key }.distinct() }

    /** Audit every card in [cards] (deduplicated by name) and group the flags by effect type. */
    fun report(cards: List<CardDefinition>, lookup: CardLookup): Report {
        val unique = cards.distinctBy { it.name }.sortedBy { it.name }
        val byKey = sortedMapOf<String, MutableList<String>>()
        val flagsByCard = sortedMapOf<String, List<String>>()
        var readFully = 0
        for ((name, keys) in cardFlags(unique, lookup)) {
            if (keys.isEmpty()) readFully++ else flagsByCard[name] = keys
            keys.forEach { byKey.getOrPut(it) { mutableListOf() } += name }
        }
        val groups = byKey.map { (k, v) -> Group(k, v) }.sortedWith(compareByDescending<Group> { it.cards.size }.thenBy { it.key })
        return Report(
            totalCards = unique.size,
            readFully = readFully,
            groups = groups,
            chooseNModals = unique.count(::isChooseN),
            crewOrSaddle = unique.count(::hasCrewOrSaddle),
            flagsByCard = flagsByCard,
        )
    }
}
