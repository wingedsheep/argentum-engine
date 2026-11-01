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
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.PhaseOutEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.effects.ScryEffect
import com.wingedsheep.sdk.scripting.effects.SurveilEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
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
 *    ([needsNoIntent]: a non-keyword counter on the source itself or one it removes from itself, any counter on a player, damage to a
 *    player, life loss, a permanent untapping, moving, transforming or animating itself, a creature
 *    shifting its own P/T with a loss on either side, an upkeep cost its controller pays or suffers
 *    on its own source, an "unless" whose suffer is life loss).
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

    /** Everything the readers said about one ability. A [Check]-free static ability has no findings. */
    data class AbilityReading(val ability: AbilityRef, val findings: List<Finding>) {
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
                intent(spell, sink, script.targetRequirements)
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
                            leaf is PreventDamageEffect
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
            intent(ability.effect, sink, ability.targetRequirements)
            combat(ability, sink)
            if (ability.timing == TimingRule.InstantSpeed) {
                timing(ability.effect, sink) { ExpiringGrantWindow.hasExpiringReading(it, creatureGrants = true) }
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
            // One that puts the cards into a hand stays flagged: that can be a payoff (Hoarding Dragon).
            if (TargetlessEntry.handsBackTheEntry(trigger) && !movesToHand(trigger.effect)) {
                sink += Finding(Check.INTENT, read = true, reason = "needs no intent: it only hands back what this permanent exiled, when it leaves")
            } else {
                intent(trigger.effect, sink, reqs)
            }
            modal(trigger.effect, sink)
            out += AbilityReading(AbilityRef(AbilityKind.TRIGGERED, i + 1, faceName, describe { trigger.description }), sink.findings)
        }

        // ── State triggers ──────────────────────────────────────────────────
        script.stateTriggeredAbilities.forEachIndexed { i, trigger ->
            val sink = Sink()
            intent(trigger.effect, sink, emptyList())
            modal(trigger.effect, sink)
            out += AbilityReading(AbilityRef(AbilityKind.STATE_TRIGGER, i + 1, faceName, describe { trigger.description }), sink.findings)
        }

        // ── Static abilities: listed, not checked ───────────────────────────
        script.staticAbilities.forEachIndexed { i, static ->
            out += AbilityReading(AbilityRef(AbilityKind.STATIC, i + 1, faceName, describe { static.description }), emptyList())
        }
        // Closest to Oracle's own order — a permanent's triggers usually precede its activated
        // abilities — with the unchecked statics last so they don't push the findings down.
        return out.sortedBy { DISPLAY_ORDER.indexOf(it.ability.kind) }
    }

    private val DISPLAY_ORDER = listOf(
        AbilityKind.SPELL, AbilityKind.TRIGGERED, AbilityKind.STATE_TRIGGER, AbilityKind.ACTIVATED, AbilityKind.STATIC,
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
    private fun intent(root: Effect, sink: Sink, reqs: List<TargetRequirement>) {
        // "Target opponent gets a poison counter": the bound slot is a player, so the counter is a clock too.
        val playerSlots = reqs.filter { it is TargetPlayer || it is TargetOpponent }.mapNotNullTo(HashSet()) { it.id }
        val noIntent = { leaf: Effect -> needsNoIntent(leaf) || countsOnAPlayerSlot(leaf, playerSlots) }
        val tags = CardIntentAnalyzer.analyzeEffect(root).tags
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
        val unreadLeaves = leaves.filterNot(noIntent)
        labels(unreadLeaves.filterNot { it is ForEachEffect }).filterNot { it.first in NO_INTENT_PIPELINES }.forEach { (label, leaf) ->
            sink += gap(Check.INTENT, unread, label, step(leaves, leaf))
        }
        for (each in unreadLeaves.filterIsInstance<ForEachEffect>()) {
            labels(fired(EffectWalker.payoffLeaves(each.body)).filterNot(noIntent)).filterNot { it.first in NO_INTENT_PIPELINES }.forEach { (label, _) ->
                sink += gap(Check.INTENT, unread, "ForEach>$label", step(leaves, each))
            }
        }
        // Every untagged step needed no tag: say so, so the ability reads as understood rather
        // than as one nobody asked about.
        if (sink.findings.size == before && tags.isNotEmpty()) {
            sink += Finding(Check.INTENT, read = true, reason = "knows what it's for: ${tags.sorted().joinToString(", ", transform = ::tagPhrase)}")
        } else if (sink.findings.size == before) {
            sink += Finding(Check.INTENT, read = true, reason = "needs no intent: it only grows, shifts the P/T of, untaps, attaches, moves, transforms or animates its own source, adds counters for its controller or removes them from itself, damages a player, makes one lose life, mills one, or is an unless-clause its payer suffers as an upkeep cost or life loss")
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
        for (leaf in leaves) {
            val expiring = if (leaf is ForEachEffect) EffectWalker.leaves(leaf.body).firstOrNull(::endsThisTurn) else leaf.takeIf(::endsThisTurn)
            expiring ?: continue
            val label = if (leaf is ForEachEffect) "ForEach>${label(expiring)}" else label(leaf)
            if (isRead(leaf)) {
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
        TargetPolarity.UNKNOWN -> "can't tell if this helps or hurts"
    }

    private fun tagPhrase(tag: IntentTag): String = when (tag) {
        IntentTag.EXILE_REMOVAL -> "exile removal"
        IntentTag.TOKEN_MAKER -> "makes tokens"
        IntentTag.COMBAT_TRICK -> "combat trick"
        IntentTag.EVASION_GRANT -> "grants evasion"
        IntentTag.SACRIFICE_OUTLET -> "sacrifice outlet"
        IntentTag.LIFEGAIN -> "life gain"
        IntentTag.TAPPER -> "taps things"
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
        return (req.id != null && "BoundVariable(name=${req.id})" in text) || "ContextTarget(index=$index)" in text
    }

    /**
     * The leaves that need no intent tag: equip / attach, a card moving, untapping, transforming or animating itself, damage to a
     * player and life loss (commented inline below), and a counter the effect puts on its own source or its own
     * controller — a creature growing itself, a charge, oil, quest or loyalty counter, `{E}` for you. There is no target to choose and nothing
     * to hold it for, and its payoff is already on the board (the evaluator reads a grown creature's
     * projected stats) or belongs to the ability that later spends it, so it needs no intent tag. A
     * keyword counter stays flagged: it grants a keyword the way [com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect]
     * does, and the reader tags the evasion and protective ones. A keyword counter is recognised by
     * its id naming a [Keyword]; that also keeps the few counters merely named after a keyword (fuse,
     * training) flagged, which errs toward flagging.
     */
    private fun fired(payoff: List<Effect>): List<Effect> = payoff.flatMap { leaf ->
        if (leaf is CreateDelayedTriggerEffect) fired(EffectWalker.firedLeaves(leaf, payoff)) else listOf(leaf)
    }

    private fun needsNoIntent(leaf: Effect): Boolean {
        // Equip / fortify / reconfigure moves the source onto a host and does nothing of its own:
        // what the Equipment is *for* is the card's reading of its "equipped creature" statics
        // (CardIntentAnalyzer tags those), and which creature gets it is the polarity check.
        if (leaf is AttachEquipmentEffect) return true
        // A card moving itself — "return this card to its owner's hand" when it dies, ninjutsu,
        // "shuffle it into its owner's library" — has no target to aim and is no spell to time:
        // whether it is worth doing is the simulation's call on the activation or trigger.
        if (leaf is MoveToZoneEffect && leaf.target == EffectTarget.Self) return true
        // A permanent untapping itself (a mana rock untapping for more, a boast's "untap it") has no
        // target and nothing to hold: the untap is worth what tapping it again buys (another mana
        // ability, or a blocker after it attacked), which the simulation already sees on the board.
        // Untapping a *chosen* target stays flagged.
        if (leaf is TapUntapEffect && !leaf.tap && leaf.target == EffectTarget.Self) return true
        // A permanent transforming itself (a werewolf, a DFC's "transform this") has no target and is
        // no spell to time: its payoff is the face it becomes, which the simulation sees on the board.
        // Transforming another permanent stays flagged.
        if (leaf is TransformEffect && leaf.target == EffectTarget.Self) return true
        // A permanent animating itself (a manland, "this artifact becomes a 3/3 until end of turn") has
        // no target and is no spell to hold: when to activate it is the timing check's question and
        // that it can block is CombatReach's; what it is worth is the creature the simulation then sees.
        // Animating another permanent stays flagged.
        if (leaf is BecomeCreatureEffect && leaf.target == EffectTarget.Self) return true
        // A creature shifting its own P/T with a loss on either side — Flowstone's "+1/-1", Morphling's
        // "+1/-1", Blind Creeper's "-1/-1" drawback — has no target and is no trick to hold: it is a
        // trade on its own body the simulation prices on the board. A pure self pump already reads
        // PUMP; a target's shift, and a self shift with no side known to be a loss, stay flagged.
        if (leaf is ModifyStatsEffect && leaf.target == EffectTarget.Self &&
            (AmountSign.of(leaf.powerModifier) == AmountSign.LOSS || AmountSign.of(leaf.toughnessModifier) == AmountSign.LOSS)
        ) return true
        // Damage to a player is a clock: the evaluator prices the life it takes on resolution, it
        // answers nothing on the board, and life lost now or one window later is the same life.
        // Which player a targeted one aims at is the polarity check's question, not intent's.
        if (leaf is DealDamageEffect && isPlayerTarget(leaf.target)) return true
        // Life loss always lands on a player, so it is the same clock — or, on "you lose N life",
        // a drawback the simulation already prices on resolution. Either way there is nothing to tag.
        if (leaf is LoseLifeEffect) return true
        // "Sacrifice it unless you pay {1}{U}" (an upkeep cost, a land's "sacrifice it unless you pay {1}") is a drawback its
        // controller chooses to pay or take: no target, nothing to hold, and whether to pay is the
        // simulation's call when the trigger resolves. A suffer that needs no intent of its own (life
        // loss, damage to a player) needs none behind an "unless" either, whoever pays. An opponent
        // paying to stop *our* payoff (Rhystic Study's draw) stays flagged.
        if (leaf is PayOrSufferEffect) {
            val paidByController = leaf.player == EffectTarget.Controller || leaf.player == EffectTarget.PlayerRef(Player.You)
            // Scry/surveil needsNoIntent only because CardIntentAnalyzer reads them, and nothing reads inside a suffer.
            return EffectWalker.payoffLeaves(leaf.suffer).all {
                (it !is ScryEffect && it !is SurveilEffect && needsNoIntent(it)) || (paidByController && harmsOnlyItsSource(it))
            }
        }
        // Scry and surveil are read (card selection); skipped here only so a sibling leaf is still asked about.
        if (leaf is ScryEffect || leaf is SurveilEffect) return true
        // A permanent removing counters from itself (a time/oil/charge tick, Clockwork Beast's
        // +1/+0 wearing off, a -1/-1 counter shed) has no target and nothing to hold: it is
        // bookkeeping or a grow the simulation sees on the board. Removing them from another stays flagged.
        if (leaf is RemoveCountersEffect && leaf.target == EffectTarget.Self) return true
        val (counter, target) = when (leaf) {
            is AddCountersEffect -> leaf.counterType to leaf.target
            is AddDynamicCountersEffect -> leaf.counterType to leaf.target
            else -> return false
        }
        // A counter on a player (poison, rad, energy) is a clock or a resource, like life loss: no
        // permanent to answer, nothing to hold, and which player gets it is the polarity check's question.
        if (isPlayerTarget(target)) return true
        if (target != EffectTarget.Self) return false
        return Keyword.entries.none { it.name == counter.name }
    }

    /** A counter placed on a bound target slot that only a player can fill (see [needsNoIntent]'s player counters). */
    private fun countsOnAPlayerSlot(leaf: Effect, playerSlots: Set<String>): Boolean {
        val target = when (leaf) {
            is AddCountersEffect -> leaf.target
            is AddDynamicCountersEffect -> leaf.target
            else -> return false
        }
        return (target as? EffectTarget.BoundVariable)?.name in playerSlots
    }

    private fun movesToHand(effect: Effect): Boolean = EffectWalker.leaves(effect).any { leaf ->
        leaf is MoveCollectionEffect && (leaf.destination as? CardDestination.ToZone)?.zone == Zone.HAND
    }

    /** A leaf that only sacrifices, taps or phases out its own source — the "suffer" of an upkeep cost. */
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
     * Whole pipelines that need no intent tag. Milling — the top of a library put into its owner's
     * graveyard — always lands on a player, like life loss: on an opponent it is a slow clock
     * (LibraryClock prices it once the library nears empty), on you it is fuel for whatever later
     * reads the graveyard. It answers nothing on the board and has no window to hold for; which
     * player a mill aims at is GraveyardSynergy's and the polarity check's question, not intent's.
     */
    private val NO_INTENT_PIPELINES = setOf("Pipeline(TopOfLibrary→graveyard)")

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
