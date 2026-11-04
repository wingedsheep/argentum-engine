package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.FaceUpInstead
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.targeting.*
import com.wingedsheep.engine.replacement.PendingReplacementRider
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.DamageBonusComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import kotlinx.serialization.Serializable

/**
 * The replacement/prevention boundary shared by combat and effect damage. Re-evaluates every
 * remaining replacement after each application and lets the affected player choose the next one.
 * A question returns the original transaction state: no shields, counters, cards, life or damage
 * have been committed. Answers replay the pure transaction until all its damage can be committed.
 */
object DamageReplacementChain {
    @Serializable
    data class Instance(
        val sourceId: EntityId?, val targetId: EntityId, val amount: Int,
        val isCombatDamage: Boolean = false, val cantBePrevented: Boolean = false,
        val sourceRef: ObjectRef? = null,
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
        val sourceSnapshot: EntitySnapshot? = null
    )
    data class DeferredResult(val damage: Instance, val host: EntityId, val controller: EntityId, val effect: ReplacementEffect)
    data class AppliedResults(val state: GameState, val events: List<GameEvent>)
    sealed interface Result {
        data class Ask(val state: GameState, val question: (String) -> PendingDecision,
                       val choiceKey: String, val optionKeys: List<String>,
                       val numberMax: Int? = null, val numberPrefix: String? = null, val numberMin: Int = 0,
                       val events: List<GameEvent> = emptyList()) : Result
        data class Done(val state: GameState, val instances: List<Instance>, val events: List<GameEvent>,
                        val lifeGains: Map<EntityId, Int>, val results: List<DeferredResult> = emptyList()) : Result
    }
    fun record(state: GameState, choiceKey: String, option: String): GameState {
        val index = choiceKey.toInt()
        require(index == state.damageReplacementChoices.size)
        return state.copy(damageReplacementChoices = state.damageReplacementChoices + option)
    }
    /** Resolve and apply one effect's complete simultaneous damage instruction. */
    fun deal(
        zones: ZoneTransitionService, state: GameState, instances: List<Instance>,
        effect: com.wingedsheep.sdk.scripting.effects.Effect? = null,
        context: EffectContext? = null, excessToController: Boolean = false
    ): EffectResult {
        // Determine excess from the announced amount before replacement/prevention effects.
        // The creature and its controller then receive their shares simultaneously.
        val announced = if (!excessToController) instances else instances.flatMap { damage ->
            val projected = state.projectedState
            val controller = projected.getController(damage.targetId)
            if (!projected.isCreature(damage.targetId) || controller == null) listOf(damage)
            else {
                val deathtouch = damage.sourceId?.let { DamageUtils.sourceHasDeathtouch(zones.cardRegistry, state, projected, it, sourceSnapshot = damage.sourceSnapshot) } == true
                val marked = state.getEntity(damage.targetId)?.get<DamageComponent>()?.amount ?: 0
                val lethal = if (deathtouch) 1 else (projected.lethalDamageThreshold(damage.targetId) - marked).coerceAtLeast(0)
                val retained = minOf(damage.amount, lethal)
                listOf(damage.copy(amount = retained), damage.copy(targetId = controller, amount = damage.amount - retained))
            }
        }
        return when (val result = resolve(zones, state, announced, inBatch = announced.size > 1)) {
            is Result.Ask -> {
                val paused = EffectResult.from(result.state.suspendForDecision(result.question,
                    DamageReplacementContinuation(result.choiceKey, result.optionKeys, instances, effect, context, excessToController, result.numberMax, result.numberPrefix, result.numberMin)))
                paused.copy(events = result.events + paused.events)
            }
            is Result.Done -> {
                var working = result.state
                val events = result.events.toMutableList()
                for (damage in result.instances) {
                    val dealt = DamageUtils.applyReplacedDamage(zones, working, damage.targetId, damage.amount, damage.sourceId,
                        damage.cantBePrevented, damage.isCombatDamage, false, damage.sourceRef, sourceSnapshot = damage.sourceSnapshot)
                    working = dealt.state
                    events += dealt.events
                }
                val applied = applyResults(zones, working, result.results)
                working = applied.state; events += applied.events
                for ((controller, amount) in result.lifeGains) {
                    val (gained, event) = DamageUtils.gainLife(working, controller, amount, predicateEvaluator = zones.predicateEvaluator)
                    working = gained; event?.let(events::add)
                }
                EffectResult.success(working, events)
            }
        }
    }

    fun applyResults(zones: ZoneTransitionService, state: GameState, results: List<DeferredResult>): AppliedResults {
        var working = state
        val events = mutableListOf<GameEvent>()
        for (result in results) {
            val d = result.damage
            when (val effect = result.effect) {
                is ReplaceDamageWithCounters -> {
                    val applied = DamageUtils.applyReplaceDamageWithCounters(zones, working, d.targetId, d.amount, d.sourceId,
                        d.isCombatDamage, result.host, effect, result.controller)
                    if (applied != null) { working = applied.state; events += applied.events }
                }
                is ReplaceDamageWithMill -> {
                    val applied = DamageUtils.applyReplaceDamageWithMill(zones, working, d.targetId, d.amount, d.sourceId,
                        d.cantBePrevented, result.host, effect, result.controller)
                    if (applied != null) { working = applied.state; events += applied.events }
                }
                else -> error("Not a deferred damage result")
            }
        }
        return AppliedResults(working, events)
    }

    private data class Pending(val damage: Instance, val applied: Set<String> = emptySet())
    private data class Step(val state: GameState, val damage: List<Instance>, val events: List<GameEvent> = emptyList(),
                            val gains: Map<EntityId, Int> = emptyMap(), val results: List<DeferredResult> = emptyList())
    private data class Allocation(val key: String, val capacity: Int, val verb: String,
                                  val apply: (GameState, Int) -> Step)
    private data class Batch(val key: String, val apply: (List<Instance>) -> BatchStep)
    private data class BatchStep(val step: Step, val outputs: List<List<Instance>>)
    private data class Candidate(val key: String, val label: String, val host: EntityId? = null,
                                 val commute: String? = null, val optionalController: EntityId? = null,
                                 val allocation: Allocation? = null, val batch: Batch? = null,
                                 val apply: () -> Step)

    fun resolve(zones: ZoneTransitionService, state: GameState, instances: List<Instance>, inBatch: Boolean = false): Result {
        @Suppress("NAME_SHADOWING")
        val state = DamageReplacementReveals.begin(state)
        var working = state
        val activeIndex = state.turnOrder.indexOf(state.activePlayerId).coerceAtLeast(0)
        val apnap = state.turnOrder.drop(activeIndex) + state.turnOrder.take(activeIndex)
        val pending = ArrayDeque(instances.filter { it.amount > 0 }.sortedBy {
            val player = if (it.targetId in state.turnOrder) it.targetId else state.projectedState.getController(it.targetId)
            apnap.indexOf(player).let { index -> if (index < 0) Int.MAX_VALUE else index }
        }.map(::Pending))
        val final = mutableListOf<Pending>()
        val candidateCache = java.util.IdentityHashMap<Pending, List<Candidate>>()
        val events = mutableListOf<GameEvent>()
        val gains = linkedMapOf<EntityId, Int>()
        val results = mutableListOf<DeferredResult>()
        var matchingState = state
        val batchShieldTargets = mutableSetOf<EntityId>()
        val batchRedirects = mutableSetOf<EntityId>()
        var choiceIndex = 0
        fun publish(ask: Result.Ask): Result.Ask {
            val (publicState, revealed) = DamageReplacementReveals.pause(state, working, events)
            return ask.copy(state = publicState, events = revealed)
        }
        fun ask(damage: Instance, options: List<Candidate>, decider: EntityId, prompt: String): Result.Ask {
            val sourceName = sourceName(working, damage)
            val targetName = if (damage.targetId == decider) "you" else name(working, damage.targetId)
            return publish(Result.Ask(state, { id -> ChooseOptionDecision(
                id = id, playerId = decider,
                prompt = "$sourceName would deal ${damage.amount} damage to $targetName. $prompt",
                context = DecisionContext(sourceId = options.firstOrNull()?.host, phase = DecisionPhase.RESOLUTION),
                options = options.map { it.label }
            ) }, choiceIndex.toString(), options.map { it.key }))
        }
        while (pending.isNotEmpty()) {
            if (pending.first().damage.amount <= 0) { pending.removeFirst(); continue }
            fun decider(incoming: Instance): EntityId =
                if (incoming.targetId in working.turnOrder) incoming.targetId
                else working.projectedState.getController(incoming.targetId)
                    ?: working.getEntity(incoming.targetId)?.get<OwnerComponent>()?.playerId ?: working.activePlayerId!!
            fun available(item: Pending): List<Candidate> = candidateCache.getOrPut(item) {
                if (item.damage.amount <= 0) emptyList() else candidates(zones, working, item.damage, inBatch, batchShieldTargets, batchRedirects,
                    { pending.asSequence().filter { it !== item }.map { it.damage } }, matchingState)
                    .filter { identity(it.key) !in item.applied }
            }
            val first = pending.first()
            val firstChoices = available(first)
            // No replacement can affect this instance until a reveal changes the matching state.
            // In ordinary combat this avoids scanning all the other assignments for every source.
            if (firstChoices.isEmpty()) { final += pending.removeFirst(); continue }
            // Shared resources and noncommuting replacements may span simultaneous sources.
            // Let the affected player choose across them before committing any one source.
            val samePlayer = pending.withIndex().filter { decider(it.value.damage) == decider(first.damage) }
            val allChoices = if (samePlayer.size > 1) samePlayer.map { it.index to available(it.value) } else emptyList()
            val sharedResource = allChoices.flatMap { (_, options) -> options.mapNotNull { it.allocation?.key ?: it.batch?.key } }
                .groupingBy { it }.eachCount().any { it.value > 1 }
            val sharedNoncommuting = allChoices.flatMap { (_, options) ->
                options.filter { it.commute == null }.map { it.key }
            }.groupingBy { it }.eachCount().any { it.value > 1 } &&
                allChoices.flatMap { it.second }.map { it.key }.distinct().size > 1
            val sharedShield = sharedResource || sharedNoncommuting
            val actions = if (sharedShield) {
                val seenShields = mutableSetOf<String>()
                allChoices.flatMap { (index, options) -> options.mapNotNull { option ->
                    val batchKey = option.allocation?.key ?: option.batch?.key
                    if (batchKey != null && !seenShields.add(batchKey)) null else index to option
                } }
            } else firstChoices.map { 0 to it }
            if (actions.isEmpty()) { final += pending.removeFirst(); continue }
            val picked = if (actions.size == 1 || !sharedShield && actions.first().second.commute != null &&
                actions.all { it.second.commute == actions.first().second.commute }) actions.first() else {
                val options = actions.map { (index, option) ->
                    if (!sharedShield) option else {
                        val incoming = pending[index].damage
                        val source = sourceName(working, incoming)
                        val recipient = if (incoming.targetId == decider(incoming)) "you" else name(working, incoming.targetId)
                        option.copy(key = "instance:$index:${option.key}", label = if (option.allocation != null || option.batch != null)
                            option.label else "${option.label} — $source's ${incoming.amount} damage to $recipient")
                    }
                }
                val answer = state.damageReplacementChoices.getOrNull(choiceIndex)
                    ?: return ask(first.damage, options, decider(first.damage), "Choose which damage replacement applies next")
                choiceIndex++
                val index = options.indexOfFirst { it.key == answer }
                require(index >= 0) { "The recorded damage replacement is no longer applicable" }
                actions[index]
            }
            val current = pending.removeAt(picked.first)
            val damage = current.damage
            val selected = picked.second
            val eventStart = events.size
            if (selected.batch != null) {
                val whole = listOf(current) + pending.toList()
                val eligible = whole.indices.filter { index ->
                    whole[index].damage.amount > 0 && available(whole[index]).any { it.batch?.key == selected.batch.key }
                }
                val applied = selected.batch.apply(eligible.map { whole[it].damage })
                working = applied.step.state; events += applied.step.events; results += applied.step.results
                applied.step.gains.forEach { (id, amount) -> gains.merge(id, amount, Int::plus) }
                val transformed = eligible.zip(applied.outputs).toMap()
                pending.clear()
                for ((index, item) in whole.withIndex()) {
                    val outputs = transformed[index]
                    if (outputs == null) pending.addLast(item) else outputs.forEach {
                        pending.addLast(Pending(it, item.applied + selected.key))
                    }
                }
            } else if (selected.allocation != null) {
                // Allocate a shared finite resource against the whole current event before any
                // other replacement can alter its capacities. Each recipient chooses its sources.
                val resource = selected.allocation
                val batch = listOf(current) + pending.toList()
                val declined = mutableMapOf<Int, String>()
                val eligible = batch.indices.mapNotNull { i ->
                    val item = batch[i]
                    if (item.damage.amount <= 0) null else {
                        val candidate = available(item).firstOrNull { it.allocation?.key == resource.key }
                        if (candidate?.optionalController != null) {
                            val yes = "${candidate.key}:consent:$i:yes"
                            val no = "${candidate.key}:consent:$i:no"
                            val answer = state.damageReplacementChoices.getOrNull(choiceIndex)
                            if (answer == null) return publish(Result.Ask(state, { id -> YesNoDecision(
                                id = id, playerId = candidate.optionalController,
                                prompt = "${sourceName(working, item.damage)} would deal ${item.damage.amount} damage to ${name(working, item.damage.targetId)}. ${candidate.label}?",
                                context = DecisionContext(sourceId = candidate.host, phase = DecisionPhase.RESOLUTION),
                                yesText = "Redirect it", noText = "Let it through"
                            ) }, choiceIndex.toString(), listOf(yes, no)))
                            choiceIndex++
                            require(answer == yes || answer == no)
                            if (answer == no) { declined[i] = candidate.key; null } else i to candidate
                        } else candidate?.let { i to it }
                    }
                }.toMap()
                val slots = eligible.keys.toList()
                val capacity = slots.sumOf { batch[it].damage.amount.toLong() }
                val total = minOf(resource.capacity.toLong(), capacity).toInt()
                var remaining = total
                val allocations = mutableMapOf<Int, Int>()
                for ((position, slot) in slots.withIndex()) {
                    val incoming = batch[slot].damage
                    val laterCapacity = slots.drop(position + 1).sumOf { batch[it].damage.amount.toLong() }
                    val minimum = (remaining.toLong() - laterCapacity).coerceAtLeast(0).toInt()
                    val maximum = minOf(remaining, incoming.amount)
                    val allocation = if (minimum == maximum) minimum else {
                        val prefix = selected.key + ":allocation:" + slot + ":"
                        val answer = state.damageReplacementChoices.getOrNull(choiceIndex)
                        if (answer == null) {
                            val decider = decider(damage)
                            val sourceName = sourceName(working, incoming)
                            val recipientName = if (incoming.targetId == decider) "you" else name(working, incoming.targetId)
                            return publish(Result.Ask(state, { id -> ChooseNumberDecision(
                                id = id, playerId = decider,
                                prompt = "${resource.verb} how much of $sourceName's ${incoming.amount} damage to $recipientName? $remaining remains to assign.",
                                context = DecisionContext(sourceId = selected.host, phase = DecisionPhase.RESOLUTION),
                                minValue = minimum, maxValue = maximum
                            ) }, choiceIndex.toString(), emptyList(), maximum, prefix, minimum))
                        }
                        choiceIndex++
                        val value = answer.removePrefix(prefix).toInt()
                        require(answer.startsWith(prefix) && value in minimum..maximum)
                        value
                    }
                    allocations[slot] = allocation
                    remaining -= allocation
                }
                check(remaining == 0)
                pending.clear()
                for ((index, item) in batch.withIndex()) {
                    val allocated = allocations[index]
                    if (allocated == null) {
                        pending.addLast(if (index in declined) item.copy(applied = item.applied + declined.getValue(index)) else item)
                        continue
                    }
                    val candidate = eligible.getValue(index)
                    val result = candidate.allocation!!.apply(working, allocated)
                    working = result.state; events += result.events; results += result.results
                    result.gains.forEach { (id, amount) -> gains.merge(id, amount, Int::plus) }
                    result.damage.forEach { pending.addLast(Pending(it, item.applied + candidate.key)) }
                }
            } else if (selected.optionalController != null) {
                val yes = selected.copy(key = selected.key + ":yes", optionalController = null)
                val no = selected.copy(key = selected.key + ":no", label = "Do not redirect", optionalController = null,
                    apply = { Step(working, listOf(damage)) })
                val answer = state.damageReplacementChoices.getOrNull(choiceIndex)
                if (answer == null) return publish(Result.Ask(state, { id -> YesNoDecision(
                    id = id, playerId = selected.optionalController,
                    prompt = "${sourceName(working, damage)} would deal ${damage.amount} damage to ${name(working, damage.targetId)}. ${selected.label}?",
                    context = DecisionContext(sourceId = selected.host, phase = DecisionPhase.RESOLUTION),
                    yesText = "Redirect it", noText = "Let it through"
                ) }, choiceIndex.toString(), listOf(yes.key, no.key)))
                choiceIndex++
                val result = if (answer == yes.key) yes.apply() else no.apply()
                working = result.state; events += result.events; results += result.results
                result.gains.forEach { (id, amount) -> gains.merge(id, amount, Int::plus) }
                result.damage.asReversed().forEach { pending.addFirst(Pending(it, current.applied + identity(selected.key))) }
            } else {
                val result = selected.apply()
                working = result.state; events += result.events; results += result.results
                result.gains.forEach { (id, amount) -> gains.merge(id, amount, Int::plus) }
                result.damage.asReversed().forEach { pending.addFirst(Pending(it, current.applied + identity(selected.key))) }
            }
            val revealed = events.subList(eventStart, events.size).filterIsInstance<TurnFaceUpEvent>()
            if (revealed.isNotEmpty()) {
                matchingState = FaceUpInstead.turnFaceUpAll(matchingState, revealed.map { it.entityId }).first
                // All surviving damage is still one pending event. A newly visible global effect
                // must see earlier instances too, without reapplying their previous replacements.
                final.asReversed().forEach(pending::addFirst)
                final.clear()
            }
            // Candidate closures capture the resource state and amounts for this application.
            candidateCache.clear()
        }
        if (batchRedirects.isNotEmpty()) working = working.copy(floatingEffects = working.floatingEffects.filterNot { it.id in batchRedirects })
        val (finished, finalEvents) = DamageReplacementReveals.finish(working.copy(damageReplacementChoices = emptyList()), events)
        return Result.Done(finished, final.map { it.damage }, finalEvents, gains, results)
    }

    private fun identity(key: String): String = key.substringBefore(":amount:")

    private fun name(state: GameState, id: EntityId): String =
        state.getEntity(id)?.get<CardComponent>()?.name?.let { nameVisibleToAll(state, id, it) } ?: "Permanent"

    private fun sourceName(state: GameState, damage: Instance): String =
        if (damage.sourceSnapshot != null) damage.sourceSnapshot.name.takeUnless { damage.sourceSnapshot.wasFaceDown } ?: "A source"
        else damage.sourceId?.let { name(state, it) } ?: "A source"

    private fun candidates(zones: ZoneTransitionService, state: GameState, d: Instance, inBatch: Boolean,
                           batchShields: MutableSet<EntityId>, batchRedirects: MutableSet<EntityId>, later: () -> Sequence<Instance>, viewState: GameState): List<Candidate> {
        val out = mutableListOf<Candidate>()
        val projected = viewState.projectedState
        val evaluator = zones.predicateEvaluator
        val disabled = d.cantBePrevented || DamageUtils.isDamagePreventionDisabled(viewState, d.targetId, d.sourceId, d.isCombatDamage, evaluator, sourceSnapshot = d.sourceSnapshot)
        val redirectDisabled = state.getEntity(d.targetId)?.has<DamageUnpreventableThisTurnComponent>() == true
        fun matches(pattern: EventPattern.DamageEvent, host: EntityId, controller: EntityId): Boolean =
            (pattern.damageType == DamageType.Any || (pattern.damageType == DamageType.Combat) == d.isCombatDamage) &&
                pattern.amount.matches(d.amount) &&
                DamageUtils.damageSourceMatches(viewState, projected, pattern.source, d.sourceId, host, controller, d.targetId, evaluator, sourceSnapshot = d.sourceSnapshot) &&
                DamageUtils.damageRecipientMatches(viewState, projected, pattern.recipient, d.targetId, host, controller, evaluator)
        fun amount(key: String, label: String, host: EntityId?, next: Int, commute: String? = null) {
            out += Candidate(key, label, host, commute = commute) { Step(state, listOf(d.copy(amount = next.coerceAtLeast(0)))) }
        }
        fun prevent(key: String, label: String, host: EntityId?) { if (!disabled) amount(key, label, host, 0, "prevent-all") }

        for (id in listOfNotNull(d.sourceId.takeIf { d.sourceSnapshot == null }, d.targetId).distinct()) {
            if (FaceUpInstead.applies(viewState, id)) out += Candidate("face-up:$id", "Turn face-down permanent face up", id) {
                val turned = FaceUpInstead.turnFaceUp(state, id)
                Step(turned.first, listOf(d), listOfNotNull(turned.second))
            }
        }

        if (!disabled && d.sourceId != null) {
            val source = d.sourceId
            val colors = d.sourceSnapshot?.colors ?: if (d.sourceSnapshot == null) projected.colorsOf(source) else emptySet()
            val types = d.sourceSnapshot?.typeLine?.cardTypes?.map { it.name } ?: if (d.sourceSnapshot == null) projected.getTypes(source) else emptySet()
            val color = (d.sourceSnapshot != null || state.getEntity(source)?.get<CardComponent>() != null || colors.isNotEmpty()) && ColorProtection.isProtected(projected, d.targetId, colors)
            val quality = (d.sourceSnapshot?.let { TypeQualityProtection.matchedQuality(projected, d.targetId, it.subtypes, it.supertypes, types) }
                ?: if (d.sourceSnapshot == null) TypeQualityProtection.matchedQuality(projected, d.targetId, source) else null) != null
            val type = types.any { projected.hasKeyword(d.targetId, "PROTECTION_FROM_CARDTYPE_${it.uppercase()}") }
            val opponent = projected.hasKeyword(d.targetId, "PROTECTION_FROM_EACH_OPPONENT") &&
                DamageUtils.damageSourceController(state, projected, source, sourceSnapshot = d.sourceSnapshot)?.let { from ->
                    projected.getController(d.targetId)?.let { state.isOpponentOf(from, it) }
                } == true
            if (color || quality || type || opponent || SourceKindProtection.isProtectedFromObject(viewState, d.targetId, source, d.sourceSnapshot) ||
                PlayerProtectionRules.isProtectedFromSource(viewState, d.targetId, source, casterId = null, predicateEvaluator = evaluator, sourceSnapshot = d.sourceSnapshot)) {
                prevent("protection:${d.targetId}", "Protection — prevent all damage", d.targetId)
            }
        }
        val shieldCount = state.getEntity(d.targetId)?.get<CountersComponent>()?.getCount(CounterType.SHIELD) ?: 0
        if (d.targetId !in state.turnOrder && (shieldCount > 0 || inBatch && d.targetId in batchShields)) {
            out += Candidate("shield:${d.targetId}", "Shield counter — prevent damage and remove a shield counter", d.targetId) {
                if (inBatch && d.targetId in batchShields) Step(state, if (disabled) listOf(d) else emptyList())
                else {
                    val shield = applyShieldCounterToDamage(state, d.targetId, disabled)!!
                    if (inBatch) batchShields += d.targetId
                    Step(shield.state, if (disabled) listOf(d) else emptyList(), listOf(shield.event))
                }
            }
        }
        data class Host(val id: EntityId, val controller: EntityId, val effects: List<ReplacementEffect>, val identity: String)
        val hosts = (viewState.getBattlefield() + viewState.emblemReplacementHolders).mapNotNull { host ->
            val source = viewState.activeReplacementSource(host) ?: return@mapNotNull null
            val controller = projected.getController(host) ?: state.getEntity(host)?.get<ControllerComponent>()?.playerId ?: return@mapNotNull null
            Host(host, controller, source.replacementEffects, "static:$host")
        } + state.grantedReplacementEffects.mapIndexed { index, grant ->
            Host(grant.entityId, grant.controllerId, listOf(grant.replacement), "grant:$index")
        }
        for (entry in hosts) {
            val host = entry.id
            val controller = entry.controller
            val hostName = name(state, host)
            for ((index, effect) in entry.effects.withIndex()) {
                val pattern = effect.appliesTo as? EventPattern.DamageEvent ?: continue
                if (!matches(pattern, host, controller)) continue
                val context = EffectContext(sourceId = host, controllerId = controller)
                if (effect.restrictions.any { !evaluator.conditions.evaluate(viewState, it, context) }) continue
                val key = "${entry.identity}:$index"
                when (effect) {
                    is DoubleDamage -> amount(key, "$hostName — ${if (effect.multiplier == 2) "double" else "multiply"} damage (${d.amount * effect.multiplier})", host, d.amount * effect.multiplier, if (pattern.amount == com.wingedsheep.sdk.scripting.events.AmountFilter.Any) "multiply" else null)
                    is HalveDamage -> amount(key, "$hostName — halve damage (${d.amount / 2})", host, d.amount / 2)
                    is ModifyDamageAmount -> {
                        val bonus = effect.dynamicModifier?.let { evaluator.amounts.evaluate(viewState, it, context, projected) } ?: effect.modifier
                        amount(key, "$hostName — modify damage (${(d.amount + bonus).coerceAtLeast(0)})", host, d.amount + bonus, if (bonus >= 0 && pattern.amount == com.wingedsheep.sdk.scripting.events.AmountFilter.Any) "add" else null)
                    }
                    is CapDamage -> if (d.amount > effect.maxAmount) amount(key, "$hostName — limit damage to ${effect.maxAmount}", host, effect.maxAmount)
                    is SetMinimumDamage -> {
                        val minimum = effect.dynamicMinimum?.let { evaluator.amounts.evaluate(viewState, it, context, projected) } ?: effect.minAmount
                        if (d.amount < minimum) amount(key, "$hostName — raise damage to $minimum", host, minimum)
                    }
                    is PreventDamage -> if (!disabled || effect.onPrevented != null) {
                        val prevented = when {
                            disabled -> 0
                            effect.allBut != null -> (d.amount - effect.allBut!!).coerceAtLeast(0)
                            effect.half -> if (effect.roundUp) (d.amount + 1) / 2 else d.amount / 2
                            effect.amount == null -> d.amount
                            else -> minOf(d.amount, effect.amount!!)
                        }
                        out += Candidate(key, "$hostName — prevent $prevented damage", host,
                            commute = if (effect.amount == null && !effect.half && effect.allBut == null && effect.onPrevented == null) "prevent-all" else null) {
                            val next = if (effect.onPrevented == null || prevented == 0 && !disabled) state else state.copy(pendingReplacementRiders = state.pendingReplacementRiders +
                                PendingReplacementRider(effect.onPrevented!!, host, controller, d.targetId, amount = prevented))
                            Step(next, listOf(d.copy(amount = d.amount - prevented)))
                        }
                    }
                    is RedirectDamage -> if (!redirectDisabled && d.sourceId != null &&
                        (effect.condition == null || evaluator.conditions.evaluate(viewState, effect.condition!!, context))) {
                        val target = DamageUtils.resolveRedirectTarget(state, effect.redirectTo, d.sourceId, host, d.targetId)
                        if (target != null && target != d.targetId && (target in state.turnOrder || target in state.getBattlefield())) {
                            out += Candidate(key, "$hostName — redirect to ${name(state, target)}", host) { Step(state, listOf(d.copy(targetId = target))) }
                        }
                    }
                    is ReplaceDamageWithCounters -> if (effect.counterRecipient != DamageCounterRecipient.DamagedPermanent || d.targetId !in state.turnOrder) {
                        out += Candidate(key, "$hostName — replace damage with counters", host) {
                            Step(state, emptyList(), results = listOf(DeferredResult(d, host, controller, effect)))
                        }
                    }
                    is ReplaceDamageWithMill -> out += Candidate(key, "$hostName — prevent damage and mill", host) {
                        Step(state, if (disabled) listOf(d) else emptyList(), results = listOf(DeferredResult(d.copy(cantBePrevented = disabled), host, controller, effect)))
                    }
                    is PreventDamageByRemovingCounter -> {
                        val present = state.getEntity(d.targetId)?.get<CountersComponent>()?.getCount(effect.counterType) ?: 0
                        if (present > 0 || !effect.requiresCounter) out += Candidate(key, "$hostName — prevent damage and remove counters", host,
                            batch = Batch("$key:${d.targetId}") { incoming ->
                                val count = when (effect.removalAmount) {
                                    CounterRemovalAmount.One -> minOf(present, 1)
                                    CounterRemovalAmount.EqualToDamage -> minOf(present.toLong(), incoming.sumOf { it.amount.toLong() }).toInt()
                                }
                                var next = state
                                val removedEvents = mutableListOf<GameEvent>()
                                if (count > 0) {
                                    next = next.updateEntity(d.targetId) { c -> c.with(c.get<CountersComponent>()!!.withRemoved(effect.counterType, count)) }
                                    removedEvents += CountersRemovedEvent(d.targetId, effect.counterType, count, name(state, d.targetId), remainingCount = present - count, byDamagePrevention = true)
                                    if (effect.onRemoved != null) next = next.copy(pendingReplacementRiders = next.pendingReplacementRiders + PendingReplacementRider(effect.onRemoved!!, host, controller, d.targetId, countersRemoved = count))
                                }
                                BatchStep(Step(next, emptyList(), removedEvents), incoming.map { item ->
                                    if (item.cantBePrevented || DamageUtils.isDamagePreventionDisabled(viewState, item.targetId, item.sourceId,
                                            item.isCombatDamage, evaluator, sourceSnapshot = item.sourceSnapshot)) listOf(item) else emptyList()
                                })
                            }) { error("Counter-removal prevention applies to the simultaneous event") }
                    }
                    is PreventDamagePerCounter -> {
                        val present = state.getEntity(d.targetId)?.get<CountersComponent>()?.getCount(effect.counterType) ?: 0
                        if (present > 0) out += Candidate(key, "$hostName — remove counters to prevent damage", host,
                            allocation = Allocation("$key:${d.targetId}", present, "Remove counters for") { current, removed ->
                                val counters = current.getEntity(d.targetId)!!.get<CountersComponent>()!!
                                val next = if (removed == 0) current else current.updateEntity(d.targetId) {
                                    it.with(counters.withRemoved(effect.counterType, removed))
                                }
                                Step(next, listOf(d.copy(amount = if (disabled) d.amount else d.amount - removed)),
                                    if (removed == 0) emptyList() else listOf(CountersRemovedEvent(d.targetId, effect.counterType, removed,
                                        name(current, d.targetId), remainingCount = counters.getCount(effect.counterType) - removed, byDamagePrevention = true)))
                            }) { error("Counter prevention is allocated over the whole damage batch") }
                    }
                    is HealOtherDamage -> if (state.getEntity(d.targetId)?.get<DamageComponent>() != null) {
                        out += Candidate(key, "$hostName — remove damage already marked", host) { Step(DamageUtils.healMarkedDamage(state, d.targetId), listOf(d)) }
                    }
                    else -> {}
                }
            }
        }
        floatingCandidates(zones, state, d, disabled, redirectDisabled, inBatch, batchRedirects, out, later, viewState, ::matches)
        val thresholdSensitive = hosts.any { host -> host.effects.any {
            (it.appliesTo as? EventPattern.DamageEvent)?.amount?.let { amount -> amount != com.wingedsheep.sdk.scripting.events.AmountFilter.Any } == true
        } } || state.floatingEffects.any {
            (it.effect.modification as? SerializableModification.AmplifyDamage)?.appliesTo?.amount?.let { amount -> amount != com.wingedsheep.sdk.scripting.events.AmountFilter.Any } == true
        }
        return if (thresholdSensitive) out.map { if (it.commute in setOf("multiply", "add")) it.copy(commute = null) else it } else out
    }

    private fun floatingCandidates(zones: ZoneTransitionService, state: GameState, d: Instance, disabled: Boolean,
                                   redirectDisabled: Boolean, inBatch: Boolean, batchRedirects: MutableSet<EntityId>,
                                   out: MutableList<Candidate>, later: () -> Sequence<Instance>, viewState: GameState, matches: (EventPattern.DamageEvent, EntityId, EntityId) -> Boolean) {
        val evaluator = zones.predicateEvaluator
        val projected = viewState.projectedState
        fun sourceMatches(floating: ActiveFloatingEffect, filter: GameObjectFilter): Boolean =
            d.sourceId != null && (d.sourceSnapshot?.let {
                evaluator.matchesSnapshot(viewState, it, filter, PredicateContext(controllerId = floating.controllerId, sourceId = floating.sourceId), onBattlefield = true)
            } ?: evaluator.matches(viewState, projected, d.sourceId, filter,
                PredicateContext(controllerId = floating.controllerId, sourceId = floating.sourceId)))
        fun replace(floating: ActiveFloatingEffect, replacement: SerializableModification?): GameState = state.copy(
            floatingEffects = state.floatingEffects.mapNotNull { old ->
                if (old.id != floating.id) old else replacement?.let { old.copy(effect = old.effect.copy(modification = it)) }
            })
        for (floating in state.floatingEffects) {
            val mod = floating.effect.modification
            val key = "floating:${floating.id}"
            val hostName = floating.sourceName ?: "Damage prevention"
            val targetMatches = d.targetId in floating.effect.affectedEntities
            fun amount(label: String, next: Int, commute: String? = null) {
                out += Candidate(key, "$hostName — $label", floating.sourceId, commute) { Step(state, listOf(d.copy(amount = next.coerceAtLeast(0)))) }
            }
            fun preventAll(label: String = "prevent all damage", gainTo: EntityId? = null, consume: Boolean = false) {
                if (disabled) return
                out += Candidate(key, "$hostName — $label", floating.sourceId,
                    commute = if (gainTo == null && !consume) "prevent-all" else null) {
                    Step(if (consume) replace(floating, null) else state, emptyList(), gains = if (gainTo == null) emptyMap() else mapOf(gainTo to d.amount))
                }
            }
            when (mod) {
                is SerializableModification.AmplifyDamage -> if (matches(mod.appliesTo, floating.sourceId ?: floating.controllerId, floating.controllerId)) {
                    amount("add ${mod.bonus} damage (${d.amount + mod.bonus})", d.amount + mod.bonus, "add")
                }
                is SerializableModification.DoubleDamageToPlayer -> if (d.targetId == mod.playerId || projected.getController(d.targetId) == mod.playerId) {
                    amount("double damage (${d.amount * 2})", d.amount * 2, "multiply")
                }
                is SerializableModification.PreventNextDamage -> if (!disabled && targetMatches &&
                    (mod.onlyFromSource == null || mod.onlyFromSource == d.sourceId) && (!mod.combatOnly || d.isCombatDamage) && mod.remainingAmount > 0) {
                    val maximum = minOf(mod.remainingAmount, d.amount)
                    val shared = later().any { it.amount > 0 && it.targetId in floating.effect.affectedEntities &&
                        (mod.onlyFromSource == null || mod.onlyFromSource == it.sourceId) && (!mod.combatOnly || it.isCombatDamage) }
                    out += Candidate(key, "$hostName — prevent ${if (shared) "up to ${mod.remainingAmount}" else maximum.toString()} damage", floating.sourceId,
                        allocation = Allocation(key, mod.remainingAmount, "Prevent") { current, prevented ->
                            val active = current.floatingEffects.firstOrNull { it.id == floating.id }
                            val remaining = (active?.effect?.modification as? SerializableModification.PreventNextDamage)?.remainingAmount ?: 0
                            val left = remaining - prevented
                            val next = current.copy(floatingEffects = current.floatingEffects.mapNotNull {
                                if (it.id != floating.id) it else if (left == 0) null else it.copy(effect = it.effect.copy(modification = mod.copy(remainingAmount = left)))
                            })
                            Step(next, listOf(d.copy(amount = d.amount - prevented)),
                                gains = if (mod.controllerGainsLife && prevented > 0) mapOf(floating.controllerId to prevented) else emptyMap())
                        }) { error("Finite prevention is allocated over the whole damage batch") }

                }
                is SerializableModification.PreventAllDamageTo -> if (targetMatches && (!mod.combatOnly || d.isCombatDamage) &&
                    (!mod.onlyCausedByObjectTargetingIt || DamageUtils.resolvingObjectTargets(state, d.targetId))) preventAll()
                is SerializableModification.PreventAllDamageToGroup -> if ((!mod.combatOnly || d.isCombatDamage) &&
                    (mod.includesController && d.targetId == floating.controllerId || mod.filter != null && d.targetId in state.getBattlefield() &&
                        evaluator.matches(viewState, projected, d.targetId, mod.filter!!, PredicateContext(controllerId = floating.controllerId))) &&
                    (mod.sourceFilter == null || sourceMatches(floating, mod.sourceFilter!!))) preventAll()
                is SerializableModification.PreventNextDamageFromMatching -> if (targetMatches && sourceMatches(floating, mod.filter)) preventAll(consume = true)
                is SerializableModification.PreventAllDamageFromSource -> if (targetMatches && d.sourceId == mod.damageSourceId) {
                    val gains = d.sourceId != null && (d.sourceSnapshot?.colors ?: if (d.sourceSnapshot == null) projected.colorsOf(d.sourceId) else emptySet()).any { it in mod.gainLifeFromColors }
                    preventAll(gainTo = if (gains) d.targetId else null)
                }
                is SerializableModification.PreventAllDamageFromGroup -> if ((!mod.combatOnly || d.isCombatDamage) && sourceMatches(floating, mod.filter)) {
                    preventAll(gainTo = if (mod.controllerGainsLife) floating.controllerId else null)
                }
                is SerializableModification.PreventCombatDamageFromGroup -> if (d.isCombatDamage && sourceMatches(floating, mod.filter)) preventAll()
                SerializableModification.PreventAllCombatDamage -> if (d.isCombatDamage) preventAll()
                SerializableModification.PreventCombatDamageToAndBy -> if (d.isCombatDamage && (targetMatches || d.sourceId in floating.effect.affectedEntities)) preventAll()
                SerializableModification.PreventAllDamageDealtBy -> if (d.sourceId in floating.effect.affectedEntities) preventAll()
                is SerializableModification.PreventNextDamageInstanceFromSource -> if (!disabled && targetMatches && d.sourceId == mod.damageSourceId) {
                    val prevented = when { mod.halveRoundedDown -> d.amount / 2; mod.maxAmount != null -> minOf(d.amount, mod.maxAmount!!); else -> d.amount }
                    out += Candidate(key, "$hostName — prevent $prevented damage", floating.sourceId) {
                        Step(replace(floating, null), listOf(d.copy(amount = d.amount - prevented)))
                    }
                }
                is SerializableModification.PreventNextDamageLeavingAmount -> if (!disabled && targetMatches && d.sourceId == mod.damageSourceId &&
                    (!mod.combatOnly || d.isCombatDamage) && floating.referencedObjects.all(state::isCurrentObject) &&
                    sourceMatches(floating, mod.eligibleSource) && d.amount > mod.amountToLeave) {
                    out += Candidate(key, "$hostName — prevent all but ${mod.amountToLeave} damage", floating.sourceId) {
                        Step(replace(floating, null), listOf(d.copy(amount = mod.amountToLeave)))
                    }
                }
                is SerializableModification.PreventNextDamageFromSourceShield -> if (d.sourceId != null &&
                    (!mod.combatOnly || d.isCombatDamage) && (!mod.playersOnly || d.targetId in state.turnOrder) &&
                    (floating.effect.affectedEntities.isEmpty() || targetMatches) &&
                    (d.sourceId == mod.damageSourceId || originOf(state, d.sourceId) == mod.damageSourceId)) {
                    // The rider still happens when prevention is prohibited; the ordinary damage continues.
                    fun preventInstance(incoming: Instance): Pair<List<Instance>, List<GameEvent>> {
                        val prohibited = incoming.cantBePrevented || DamageUtils.isDamagePreventionDisabled(
                            viewState, incoming.targetId, incoming.sourceId, incoming.isCombatDamage, evaluator,
                            sourceSnapshot = incoming.sourceSnapshot)
                        val prevented = if (prohibited && mod.preventDamage) 0 else incoming.amount
                        val event = DamagePreventedEvent(requireNotNull(incoming.sourceId), incoming.targetId, prevented, mod.linkId,
                            sourceName = sourceName(state, incoming), sourceControllerId = DamageUtils.damageSourceController(
                                state, projected, requireNotNull(incoming.sourceId), sourceSnapshot = incoming.sourceSnapshot))
                        return (if (mod.preventDamage && !prohibited) emptyList() else listOf(incoming)) to
                            (if (prevented > 0) listOf(event) else emptyList())
                    }
                    out += Candidate(key, "$hostName — ${if (mod.preventDamage) "prevent" else "reflect"} damage", floating.sourceId,
                        batch = Batch(key) { incoming ->
                            val prevented = incoming.map(::preventInstance)
                            BatchStep(Step(replace(floating, null), emptyList(), prevented.flatMap { it.second }),
                                prevented.map { it.first })
                        }) {
                        val (remaining, events) = preventInstance(d)
                        Step(replace(floating, null), remaining, events)
                    }
                }
                is SerializableModification.RedirectNextDamage -> if (!redirectDisabled &&
                    (mod.chosenSource == null || mod.chosenSource!!.reference == (d.sourceRef ?: d.sourceId?.let(state::objectRef))) &&
                    OptionalDamageRedirect.redirectShieldCovers(viewState, floating, mod, d.targetId, evaluator)) {
                    val redirected = minOf(mod.amount ?: d.amount, d.amount)
                    if (redirected > 0) out += Candidate(key, "$hostName — redirect $redirected damage to ${name(state, mod.redirectToId)}", floating.sourceId,
                        optionalController = if (mod.optional) floating.controllerId else null,
                        batch = if (mod.optional || mod.amount != null || mod.scope == RedirectScope.NEXT_INSTANCE) null else Batch(key) { incoming ->
                            val next = if (mod.scope == RedirectScope.NEXT_BATCH) replace(floating, null) else state
                            BatchStep(Step(next, emptyList()), incoming.map { listOf(it.copy(targetId = mod.redirectToId)) })
                        },
                        allocation = if (mod.amount == null) null else Allocation(key, mod.amount!!, "Redirect") { current, assigned ->
                            val active = current.floatingEffects.firstOrNull { it.id == floating.id }
                            val remaining = (active?.effect?.modification as? SerializableModification.RedirectNextDamage)?.amount ?: 0
                            val left = remaining - assigned
                            val next = current.copy(floatingEffects = current.floatingEffects.mapNotNull {
                                if (it.id != floating.id) it else if (left == 0) null else it.copy(effect = it.effect.copy(modification = mod.copy(amount = left)))
                            })
                            Step(next, (if (assigned > 0) listOf(d.copy(targetId = mod.redirectToId, amount = assigned)) else emptyList()) +
                                (if (assigned < d.amount) listOf(d.copy(amount = d.amount - assigned)) else emptyList()))
                        }) {
                        val nextState = when {
                            mod.amount != null -> replace(floating, (mod.amount!! - redirected).takeIf { it > 0 }?.let { mod.copy(amount = it) })
                            mod.scope == RedirectScope.NEXT_INSTANCE -> replace(floating, null)
                            mod.scope == RedirectScope.NEXT_BATCH -> if (inBatch) { batchRedirects += floating.id; state } else replace(floating, null)
                            else -> state
                        }
                        Step(nextState, listOf(d.copy(targetId = mod.redirectToId, amount = redirected)) +
                            if (redirected < d.amount) listOf(d.copy(amount = d.amount - redirected)) else emptyList())
                    }
                }
                SerializableModification.RedirectCombatDamageToController -> if (!redirectDisabled && d.isCombatDamage && d.sourceId in floating.effect.affectedEntities) {
                    val controller = d.sourceId?.let { DamageUtils.damageSourceController(state, projected, it, sourceSnapshot = d.sourceSnapshot) }
                    if (controller != null && controller != d.targetId) out += Candidate(key, "$hostName — redirect damage to its controller", floating.sourceId) {
                        if (inBatch) batchRedirects += floating.id
                        Step(if (inBatch) state else replace(floating, null), listOf(d.copy(targetId = controller)))
                    }
                }
                else -> {}
            }
        }
        // Legacy additive sources still participate as individual replacements.
        if (d.sourceId != null) {
            val controller = DamageUtils.damageSourceController(state, projected, d.sourceId, sourceSnapshot = d.sourceSnapshot)
            if (controller != null) {
                state.getEntity(controller)?.get<DamageBonusComponent>()?.let { bonus ->
                    if (d.sourceSnapshot?.let { evaluator.matchesSnapshot(viewState, it, bonus.sourceFilter, PredicateContext(controllerId = controller), onBattlefield = true) }
                        ?: evaluator.matches(viewState, projected, d.sourceId, bonus.sourceFilter, PredicateContext(controllerId = controller))) {
                        out += Candidate("player-bonus:$controller", "Damage bonus — add ${bonus.bonusAmount} damage", commute = "add") {
                            Step(state, listOf(d.copy(amount = (d.amount + bonus.bonusAmount).coerceAtLeast(0))))
                        }
                    }
                }
                if (!d.isCombatDamage) for (host in state.controlledBattlefield(controller)) {
                    if (projected.hasLostAllAbilities(host) || projected.isFaceDown(host)) continue
                    val container = state.getEntity(host) ?: continue
                    val definition = container.faceUpCardDefinition(zones.cardRegistry) ?: continue
                    val level = container.get<ClassLevelComponent>()?.currentLevel
                    for ((index, ability) in definition.script.effectiveStaticAbilities(level).withIndex()) {
                        if (ability !is NoncombatDamageBonus) continue
                        val recipientController = if (d.targetId in state.turnOrder) d.targetId else projected.getController(d.targetId)
                        if (recipientController == null || !state.isOpponentOf(recipientController, controller)) continue
                        out += Candidate("static-bonus:$host:$index", "${name(state, host)} — add ${ability.bonusAmount} damage", host, "add") {
                            Step(state, listOf(d.copy(amount = (d.amount + ability.bonusAmount).coerceAtLeast(0))))
                        }
                    }
                }
            }
        }
    }

    private fun originOf(state: GameState, sourceId: EntityId): EntityId? {
        val source = state.getEntity(sourceId) ?: return null
        return source.get<com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent>()?.sourceId
            ?: source.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()?.sourceId
            ?: source.get<com.wingedsheep.engine.state.components.stack.AbilityOnStackComponent>()?.sourceId
    }
}
