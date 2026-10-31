package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.CardLookup
import com.wingedsheep.ai.engine.knowledge.CombatReach
import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import java.util.Locale

/**
 * The AI card-reading audit: runs the AI's static card readers over a card and reports what they
 * could not read, named by the SDK **effect type** that defeated them — so one classifier entry,
 * not one card, is the unit of work (see `docs/ai/reading-audit.md`).
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
 *    ([needsNoIntent]: a non-keyword counter on the source itself or on its controller).
 *  - [Check.COMBAT] — [CombatReach] over each leaf of a non-mana activated ability that changes
 *    power/toughness, keywords, P/T counters or blocking, and that combat simulation cannot see.
 *  - [Check.TIMING] — an until-end-of-turn leaf on an instant-speed activation or an instant-speed
 *    spell with no reading in [ExpiringGrantWindow] (abilities) or in the combat-trick / group-pump
 *    reading [com.wingedsheep.ai.engine.knowledge.HoldPolicy] gets from [CardIntentAnalyzer]
 *    (spells).
 *  - [Check.MODAL] — a "choose more than one" modal whose per-extra-mode cost has no picker in
 *    `TargetSelection.payEscalateCost`, so the AI can only ever choose one mode.
 *
 * Choose-N modals in general and crew/saddle are counted as information, not gaps: both are read
 * today (`TargetSelection.modeSelections`, `Strategist.tapForPowerSelection`).
 */
object AiReadingAudit {

    enum class Check(val label: String) {
        POLARITY("polarity"),
        INTENT("intent"),
        COMBAT("combat"),
        TIMING("timing"),
        MODAL("modal"),
    }

    /** One thing a reader did not understand on one card. */
    data class Flag(val check: Check, val effectType: String) {
        val key: String get() = "${check.label}: $effectType"
    }

    /** Every [Flag] for [card], deduplicated, in a stable order. */
    fun audit(card: CardDefinition, lookup: CardLookup = TargetPolarityAnalyzer.NO_DEFINITIONS): List<Flag> {
        val flags = linkedSetOf<Flag>()
        for (face in facesOf(card)) auditFace(card, face, lookup, flags)
        return flags.sortedBy { it.key }
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
    private class Face(val script: CardScript, val instantSpeed: Boolean, val keywordAbilityNames: List<String>)

    private fun facesOf(card: CardDefinition): List<Face> {
        fun face(def: CardDefinition) = Face(
            def.script,
            def.typeLine.isInstant || Keyword.FLASH in def.keywords,
            def.keywordAbilities.map { it.toString() },
        )
        return buildList {
            add(face(card))
            card.cardFaces.forEach { f ->
                add(Face(f.script, f.typeLine.isInstant || Keyword.FLASH in f.keywords, emptyList()))
            }
            card.backFace?.let { add(face(it)) }
        }
    }

    /** Every effect root a script can resolve: spell, triggers, state triggers, non-mana abilities. */
    private fun roots(script: CardScript): List<Effect> =
        EffectWalker.slots(script).map { it.effect } + script.stateTriggeredAbilities.map { it.effect }

    private fun auditFace(card: CardDefinition, face: Face, lookup: CardLookup, flags: MutableSet<Flag>) {
        val script = face.script
        val topLevel = script === card.script
        val intent = CardIntentAnalyzer.analyze(card)

        // ── Polarity ────────────────────────────────────────────────────────
        val spell = script.spellEffect
        when {
            script.auraTarget != null && script.targetRequirements.isEmpty() -> {
                if (topLevel && card.cardFaces.isEmpty()) {
                    val reading = TargetPolarityAnalyzer.forSpell(card, lookup).singleOrNull()
                    if (reading == TargetPolarity.UNKNOWN) auraFlags(script).forEach { flags += Flag(Check.POLARITY, it) }
                }
            }
            spell is ModalEffect && script.targetRequirements.isEmpty() ->
                spell.modes.forEach { mode ->
                    polarity(mode.effect, mode.targetRequirements, TargetPolarityAnalyzer.forMode(mode, lookup), lookup, flags)
                }
            script.targetRequirements.isNotEmpty() -> {
                val reqs = script.targetRequirements
                polarity(spell, reqs, TargetPolarityAnalyzer.forRequirements(spell, reqs, lookup), lookup, flags)
            }
        }
        for (ability in script.activatedAbilities.filterNot { it.isManaAbility }) {
            abilityPolarity(ability.effect, ability.targetRequirements, lookup, flags) {
                TargetPolarityAnalyzer.forAbility(card, ability, lookup)
            }
        }
        for (trigger in script.triggeredAbilities) {
            val reqs = listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements
            abilityPolarity(trigger.effect, reqs, lookup, flags) {
                TargetPolarityAnalyzer.forRequirements(trigger.effect, reqs, lookup)
            }
        }

        // ── Intent ──────────────────────────────────────────────────────────
        // Per ability, not per leaf: a pipeline's plumbing ("shuffle", "select from the gathered
        // cards") earns no tag of its own and needs none when a sibling leaf reads the ability.
        for (root in roots(script)) {
            if (CardIntentAnalyzer.analyzeEffect(root).tags.isNotEmpty()) continue
            val leaves = EffectWalker.leaves(root).filterNot(::needsNoIntent)
            labels(leaves.filterNot { it is ForEachEffect }).forEach { flags += Flag(Check.INTENT, it) }
            for (each in leaves.filterIsInstance<ForEachEffect>()) {
                labels(EffectWalker.leaves(each.body).filterNot(::needsNoIntent)).forEach { flags += Flag(Check.INTENT, "ForEach>$it") }
            }
        }

        // ── Combat ──────────────────────────────────────────────────────────
        for (ability in script.activatedAbilities.filterNot { it.isManaAbility }) {
            for (leaf in EffectWalker.leaves(ability.effect)) {
                val parts = if (leaf is ForEachEffect) EffectWalker.leaves(leaf.body).map { "ForEach>" to it } else listOf("" to leaf)
                for ((prefix, part) in parts) {
                    if (combatShaped(part) && CombatReach.of(part).isEmpty()) {
                        flags += Flag(Check.COMBAT, prefix + label(part))
                    }
                }
            }
        }

        // ── Timing ──────────────────────────────────────────────────────────
        for (ability in script.activatedAbilities) {
            if (ability.isManaAbility || ability.timing != TimingRule.InstantSpeed) continue
            timing(ability.effect, flags) { ExpiringGrantWindow.hasExpiringReading(it, creatureGrants = true) }
        }
        if (spell != null && face.instantSpeed) {
            timing(spell, flags) { leaf ->
                ExpiringGrantWindow.hasExpiringReading(leaf, creatureGrants = false) ||
                    (leaf is ModifyStatsEffect && IntentTag.COMBAT_TRICK in intent.tags) ||
                    (leaf is ForEachEffect && intent.expiringGroupPump)
            }
        }

        // ── Modal ───────────────────────────────────────────────────────────
        for (root in roots(script)) {
            for (modal in EffectWalker.leaves(root).filterIsInstance<ModalEffect>()) {
                val extra = modal.additionalCostPerExtraMode ?: continue
                if (modal.chooseCount <= 1 && modal.dynamicChooseCount == null) continue
                if (!escalateHasPicker(extra)) flags += Flag(Check.MODAL, "escalate>${label(extra)}")
            }
        }
    }

    /** Polarity of an ability's slots, descending into a modal body's modes the way the AI does. */
    private fun abilityPolarity(
        effect: Effect,
        reqs: List<TargetRequirement>,
        lookup: CardLookup,
        flags: MutableSet<Flag>,
        read: () -> List<TargetPolarity>,
    ) {
        if (effect is ModalEffect && reqs.isEmpty()) {
            effect.modes.forEach { mode ->
                polarity(mode.effect, mode.targetRequirements, TargetPolarityAnalyzer.forMode(mode, lookup), lookup, flags)
            }
        } else if (reqs.isNotEmpty()) {
            polarity(effect, reqs, read(), lookup, flags)
        }
    }

    /** Attribute each UNKNOWN slot in [readings] to the effect types that left it unread. */
    private fun polarity(
        effect: Effect?,
        reqs: List<TargetRequirement>,
        readings: List<TargetPolarity>,
        lookup: CardLookup,
        flags: MutableSet<Flag>,
    ) {
        for ((slot, reading) in readings.withIndex()) {
            if (reading != TargetPolarity.UNKNOWN) continue
            if (effect == null) {
                flags += Flag(Check.POLARITY, "(no effect)")
                continue
            }
            val probes = EffectWalker.leaves(effect).map { leaf ->
                leaf to (TargetPolarityAnalyzer.forRequirements(leaf, reqs, lookup).getOrNull(slot) ?: TargetPolarity.UNKNOWN)
            }
            val unknown = probes.filter { it.second == TargetPolarity.UNKNOWN }.map { it.first }
            val naming = unknown.filter { names(it, reqs[slot], slot) }
            val read = probes.filter { it.second != TargetPolarity.UNKNOWN }
            when {
                naming.isNotEmpty() -> labels(naming, pipelineOf = probes.map { it.first }).forEach { flags += Flag(Check.POLARITY, it) }
                read.map { it.second }.distinct().size > 1 ->
                    flags += Flag(Check.POLARITY, "mixed(${read.map { label(it.first) }.distinct().sorted().joinToString("+")})")
                unknown.isNotEmpty() -> labels(unknown).forEach { flags += Flag(Check.POLARITY, it) }
                else -> flags += Flag(Check.POLARITY, "(slot named by no leaf)")
            }
        }
    }

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

    /** Flag each top-level until-end-of-turn leaf of [effect] that [isRead] does not read. */
    private fun timing(effect: Effect, flags: MutableSet<Flag>, isRead: (Effect) -> Boolean) {
        for (leaf in EffectWalker.leaves(effect)) {
            val expiring = if (leaf is ForEachEffect) EffectWalker.leaves(leaf.body).firstOrNull(::endsThisTurn) else leaf.takeIf(::endsThisTurn)
            expiring ?: continue
            if (isRead(leaf)) continue
            flags += Flag(Check.TIMING, if (leaf is ForEachEffect) "ForEach>${label(expiring)}" else label(leaf))
        }
    }

    /**
     * A counter the effect puts on its own source or its own controller — a creature growing itself,
     * a charge, oil, quest or loyalty counter, `{E}` for you. There is no target to choose and nothing
     * to hold it for, and its payoff is already on the board (the evaluator reads a grown creature's
     * projected stats) or belongs to the ability that later spends it, so it needs no intent tag. A
     * keyword counter stays flagged: it grants a keyword the way [com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect]
     * does, and the reader tags the evasion and protective ones. A keyword counter is recognised by
     * its id naming a [Keyword]; that also keeps the few counters merely named after a keyword (fuse,
     * training) flagged, which errs toward flagging.
     */
    private fun needsNoIntent(leaf: Effect): Boolean {
        val (counter, target) = when (leaf) {
            is AddCountersEffect -> leaf.counterType to leaf.target
            is AddDynamicCountersEffect -> leaf.counterType to leaf.target
            else -> return false
        }
        if (target != EffectTarget.Self && target != EffectTarget.Controller) return false
        return Keyword.entries.none { it.name == counter.name }
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
     * Effect-type labels for [leaves], with each library/graveyard/hand **pipeline** read as one
     * type. A pipeline ("gather the cards in your graveyard, choose one, move it to your hand") is
     * lowered to a gather, a selection, a move and some bookkeeping; none of the stages means
     * anything alone, so they are named together as `Pipeline(<from>→<to>)` — read off
     * [pipelineOf], the whole effect, when [leaves] is only part of it — and the stages dropped.
     */
    private fun labels(leaves: List<Effect>, pipelineOf: List<Effect> = leaves): List<String> {
        val stages = leaves.filter { label(it) in PIPELINE_STAGES }
        if (stages.isEmpty()) return leaves.map(::label).distinct()
        val pipeline = pipelineLabel(pipelineOf)
        return (leaves.filterNot { label(it) in PIPELINE_STAGES }.map(::label) + pipeline).distinct()
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
         * two runs diff line by line. Item A2's ratchet reads this.
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
