package com.wingedsheep.ai.jev

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.view.*
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*

internal class JevActions(
    private val q: JevChoices,
    private val state: ClientGameState,
    private val label: (EntityId) -> String,
) {
    private fun cards(prompt: String, ids: List<EntityId>, min: Int = 0, max: Int = ids.size) =
        q.select(prompt, ids, min, max, label)

    fun complete(info: LegalActionInfo): GameAction {
        val action = info.action
        if (action is DeclareAttackers) {
            val assignments = linkedMapOf<EntityId, EntityId>()
            for (attacker in info.validAttackers.orEmpty()) {
                val legal = info.attackTargetsByAttacker?.get(attacker) ?: info.validAttackTargets.orEmpty()
                val defenders: List<EntityId?> = legal +
                    if (attacker in info.mandatoryAttackers.orEmpty()) emptyList() else listOf(null)
                val defender = q.pick("Attack with ${label(attacker)}?", defenders) {
                    if (it == null) "Do not attack" else "Attack ${label(it)}"
                }
                if (defender != null) assignments[attacker] = defender
            }
            return action.copy(attackers = assignments)
        }
        if (action is DeclareBlockers) {
            val attackers = state.combat?.attackers.orEmpty().map { it.creatureId }
            return action.copy(blockers = info.validBlockers.orEmpty().associateWith { blocker ->
                cards("${label(blocker)} blocks which attackers? Obey evasion, menace and mandatory blocks.",
                    attackers, 0, info.blockerMaxBlockCounts?.get(blocker) ?: 1)
            }.filterValues { it.isNotEmpty() })
        }
        if (action is CrewVehicle) return action.copy(crewCreatures = powerPayment(info))
        if (action is SaddleMount) return action.copy(saddleCreatures = powerPayment(info))
        if (action !is CastSpell && action !is ActivateAbility && action !is CycleCard) return action

        val x = if (info.hasXCost && info.maxAffordableX != null)
            q.number("Choose X", info.minX, info.maxAffordableX!!) else null
        if (action is CycleCard) return action.copy(xValue = x ?: action.xValue)

        val modal = info.modalEnumeration
        val modes = if (modal == null) emptyList() else {
            val chosen = mutableListOf<ModalEnumerationModeInfo>()
            while (chosen.size < modal.chooseCount) {
                val options = modal.modes.filter { it.available && (modal.allowRepeat || it !in chosen) }
                val choices: List<ModalEnumerationModeInfo?> = options +
                    if (chosen.size >= modal.minChooseCount) listOf(null) else emptyList()
                if (choices.isEmpty()) break
                val next = q.pick("Choose spell mode (${chosen.size} chosen)", choices) {
                    it?.description ?: "Finish choosing modes"
                } ?: break
                chosen += next
            }
            chosen
        }
        val modeTargets = modes.map { targets(it.targetRequirements, x) }
        val targetGroups = if (modal != null) modeTargets else targetGroups(
            info.targetRequirements ?: if (info.requiresTargets) listOf(LegalActionTargetInfo(
                index = 0, description = info.targetDescription ?: "Target", minTargets = info.minTargets,
                maxTargets = info.targetCount, validTargets = info.validTargets.orEmpty(),
                xConstrainsManaValue = info.xConstrainsTargetManaValue,
                xConstrainsManaValueExactly = info.xConstrainsTargetManaValueExactly,
                xConstrainsPower = info.xConstrainsTargetPower, xConstrainsCount = info.xConstrainsTargetCount,
                xConstrainsCountExactly = info.xConstrainsTargetCountExactly,
            )) else emptyList(), x)
        val targets = targetGroups.flatten()
        val costs = listOfNotNull(info.additionalCostInfo) + info.additionalCostInfo?.alsoRequired.orEmpty() + modes.mapNotNull { it.additionalCostInfo } +
            List((modes.size - 1).coerceAtLeast(0)) { modal?.additionalCostPerExtraMode }.filterNotNull()
        val payment = costs.map { cost(it, x.takeIf { action is CastSpell }) }.reduceOrNull(::combine)
        val manaPayment = manaPayment(info)
        val alternative = alternative(info)
        // A total that reads a mana-paid {X} (Shatterskull Smashing) is priced per X by the server.
        val divisionTotal = info.totalDamageByX?.get(x ?: 0) ?: info.totalDamageToDistribute
        val damage = if (info.requiresDamageDistribution && divisionTotal != null) {
            // A division scoped to one requirement (Magma Opus) is among that requirement's targets only.
            val divided = info.distributionTargetGroup?.let { targetGroups.getOrNull(it).orEmpty() } ?: targets
            if (info.distributionTargetGroup != null && divided.size < 2) null
            else JevDecisions(q, label).distribute("Divide damage", divided.map(::targetId), divisionTotal, info.minDamagePerTarget ?: 1)
        } else null
        val groupCounts = targetGroups.map { it.size }.takeIf { modal == null && info.distributionTargetGroup != null }
        return when (action) {
            is CastSpell -> action.copy(targets = targets.ifEmpty { action.targets }, xValue = x ?: action.xValue,
                // The engine pre-fills the named opponent of an "an opponent gains life" cost per cast.
                additionalCostPayment = payment?.copy(lifeGainRecipient = action.additionalCostPayment?.lifeGainRecipient)
                    ?: action.additionalCostPayment, paymentStrategy = manaPayment,
                alternativePayment = alternative.takeUnless { it.isEmpty } ?: action.alternativePayment, damageDistribution = damage,
                targetGroupCounts = groupCounts ?: action.targetGroupCounts,
                chosenModes = if (modal == null) action.chosenModes else modes.map { it.index },
                modeTargetsOrdered = if (modal == null) action.modeTargetsOrdered else modeTargets)
            is ActivateAbility -> action.copy(targets = targets.ifEmpty { action.targets }, xValue = x ?: action.xValue,
                costPayment = payment ?: action.costPayment, alternativePayment = alternative.takeUnless { it.isEmpty } ?: action.alternativePayment, damageDistribution = damage,
                manaColorChoice = if (info.requiresManaColorChoice)
                    q.pick("Choose mana color", info.availableManaColors?.map(Color::valueOf) ?: Color.entries.toList())
                else action.manaColorChoice)
            else -> action
        }
    }

    private fun powerPayment(info: LegalActionInfo) = cards(
        "Choose creatures with total power at least ${info.tapForPowerRequired}",
        info.tapForPowerCreatures.orEmpty().map { it.entityId })

    private fun targets(requirements: List<LegalActionTargetInfo>, x: Int?): List<ChosenTarget> =
        targetGroups(requirements, x).flatten()

    /** The chosen targets, one group per requirement. */
    private fun targetGroups(requirements: List<LegalActionTargetInfo>, x: Int?): List<List<ChosenTarget>> {
        val chosenIds = mutableListOf<EntityId>()
        val groups = mutableListOf<List<ChosenTarget>>()
        for (r in requirements) {
            val candidates = r.validTargetsByPrefix?.get(chosenIds.joinToString(","))
                ?: if (r.validTargetsByPrefix == null) r.validTargets else emptyList()
            val options = candidates.filter { id ->
                val card = state.cards[id]
                (x == null || !r.xConstrainsManaValue || card == null || card.manaValue <= x) &&
                    (x == null || !r.xConstrainsManaValueExactly || card?.manaValue == x) &&
                    (x == null || !r.xConstrainsPower || card?.power == x)
            }
            val max = if (r.xConstrainsCount && x != null) x else r.maxTargets
            val min = if (r.xConstrainsCountExactly && x != null) x else minOf(r.minTargets, max)
            val picked = cards(r.description, options, min, max)
            chosenIds += picked
            groups += picked.map(::target)
        }
        return groups
    }

    private fun target(id: EntityId): ChosenTarget {
        if (state.players.any { it.playerId == id }) return ChosenTarget.Player(id)
        val zone = state.zones.firstOrNull { id in it.cardIds }?.zoneId ?: state.cards[id]?.zone
            ?: error("Target has no visible zone")
        return when (zone.zoneType) {
            Zone.STACK -> ChosenTarget.Spell(id)
            Zone.BATTLEFIELD -> ChosenTarget.Permanent(id)
            else -> ChosenTarget.Card(id, zone.ownerId, zone.zoneType)
        }
    }

    /** [x] is the cast's announced X, which a "remove X counters" or "sacrifice X creatures" cost owes exactly. */
    private fun cost(c: AdditionalCostInfo, x: Int? = null): AdditionalCostPayment {
        fun select(prompt: String, ids: List<EntityId>, min: Int, max: Int = min) =
            if (ids.isEmpty()) emptyList() else cards(prompt, ids, min, max)
        val removals = mutableListOf<DistributedCounterRemoval>()
        var remaining = c.distributedCounterRemovalTotal.takeIf { it > 0 || c.costType != "RemoveCounters" } ?: (x ?: 0)
        for (creature in c.counterRemovalCreatures) {
            for ((type, available) in creature.availableCountersByType) {
                val amount = q.number("Remove $type counters from ${label(creature.entityId)} ($remaining required)", 0, minOf(remaining, available))
                if (amount > 0) removals += DistributedCounterRemoval(creature.entityId, type, amount)
                remaining -= amount
            }
        }
        // "Sacrifice any number of …" (Devouring Greed) returns its picks in the variable-count channel.
        val variableSacrifice = c.costType == "SacrificeVariable"
        return AdditionalCostPayment(
            sacrificedPermanents = if (variableSacrifice) emptyList()
            else select("Sacrifice for ${c.description}", c.validSacrificeTargets, c.sacrificeCount),
            discardedCards = select("Discard for ${c.description}", c.validDiscardTargets, c.discardCount),
            tappedPermanents = select("Tap for ${c.description}", c.validTapTargets, c.tapCount),
            bouncedPermanents = select("Return to hand for ${c.description}", c.validBounceTargets, c.bounceCount),
            exiledCards = if (c.validCraftMaterials.isNotEmpty())
                select("Exile craft materials", c.validCraftMaterials, c.craftMinCount, c.craftMaxCount ?: c.validCraftMaterials.size)
            else select(
                if (c.exileCardTypes.isNotEmpty())
                    "Exile for ${c.description}; ${c.exileMinTotalWeight} or more distinct card types among them"
                else "Exile for ${c.description}; required total weight ${c.exileMinTotalWeight}",
                c.validExileTargets, c.exileMinCount, c.exileMaxCount),
            beheldCards = select("Behold for ${c.description}", c.validBeholdTargets, c.beholdCount),
            revealedCards = select("Reveal for ${c.description}", c.validRevealTargets, c.revealCount),
            blightTargets = select("Blight for ${c.description}", c.validBlightTargets, 1),
            blightAmount = if (c.blightVariableMaxX > 0) q.number("Choose blight X", 0, c.blightVariableMaxX) else 0,
            payXLifeAmount = if (c.payXLifeMaxX > 0) q.number("Choose life to pay", 0, c.payXLifeMaxX) else 0,
            // "Sacrifice X creatures" (Eliminate the Competition) owes exactly the announced X.
            variableCostPermanents = if (variableSacrifice && c.sacrificeCountIsX)
                select("Sacrifice for ${c.description}", c.validSacrificeTargets, x ?: 0)
            else if (variableSacrifice)
                select("Sacrifice for ${c.description}", c.validSacrificeTargets, c.sacrificeCount, c.validSacrificeTargets.size)
            else if (c.costType == "TapForTotalPower")
                cards("Tap creatures with total power at least ${c.tapForPowerRequired}", c.tapForPowerCreatures.map { it.entityId })
            // null, not empty: an empty list is the answer "none", and would skip the engine's own
            // pause for an activated ability's variable-count cost.
            else null,
            distributedCounterRemovals = removals,
        )
    }

    private fun alternative(info: LegalActionInfo): AlternativePaymentChoice {
        val convoke = info.validConvokeCreatures.orEmpty()
        val tapped = cards("Choose creatures to convoke", convoke.map { it.entityId })
        return AlternativePaymentChoice(
            convokedCreatures = tapped.associateWith { id ->
                val colors: List<Color?> = listOf(null) + convoke.first { it.entityId == id }.colors
                ConvokePayment(q.pick("Convoke payment from ${label(id)}", colors) { it?.name ?: "Generic mana" })
            },
            delvedCards = cards("Exile cards to delve", info.validDelveCards.orEmpty().map { it.entityId }, info.minDelveNeeded ?: 0),
            tapForGenericPermanents = cards("Tap to pay generic mana", info.validTapForGenericPermanents.orEmpty().map { it.entityId },
                max = info.tapForGenericAmount ?: info.validTapForGenericPermanents.orEmpty().size).toSet(),
            harmonizeCreature = cards("Tap a creature to harmonize", info.validHarmonizeCreatures.orEmpty().map { it.entityId }, max = 1).firstOrNull(),
        )
    }

    private fun manaPayment(info: LegalActionInfo): PaymentStrategy {
        val sources = info.availableManaSources.orEmpty()
        if (sources.isEmpty()) return PaymentStrategy.AutoPay
        val auto = q.pick("How to pay ${info.manaCostString.orEmpty()} after alternative payments?",
            listOf(true, false)) { if (it) "Use the engine's automatic mana solver" else "Choose mana sources explicitly" }
        return if (auto) PaymentStrategy.AutoPay else PaymentStrategy.Explicit(
            cards("Choose mana sources to tap", sources.map { it.entityId }))
    }

    private fun combine(a: AdditionalCostPayment, b: AdditionalCostPayment) = AdditionalCostPayment(
        sacrificedPermanents = a.sacrificedPermanents + b.sacrificedPermanents,
        discardedCards = a.discardedCards + b.discardedCards,
        exiledCards = a.exiledCards + b.exiledCards,
        variableCostPermanents = if (a.variableCostPermanents == null && b.variableCostPermanents == null) null
            else a.variableCostPermanents.orEmpty() + b.variableCostPermanents.orEmpty(),
        beheldCards = a.beheldCards + b.beheldCards,
        revealedCards = a.revealedCards + b.revealedCards,
        tappedPermanents = a.tappedPermanents + b.tappedPermanents,
        bouncedPermanents = a.bouncedPermanents + b.bouncedPermanents,
        blightTargets = a.blightTargets + b.blightTargets,
        blightAmount = a.blightAmount + b.blightAmount,
        payXLifeAmount = a.payXLifeAmount + b.payXLifeAmount,
        distributedCounterRemovals = a.distributedCounterRemovals + b.distributedCounterRemovals,
    )

    private fun targetId(t: ChosenTarget): EntityId = when (t) {
        is ChosenTarget.Player -> t.playerId
        is ChosenTarget.Permanent -> t.entityId
        is ChosenTarget.Card -> t.cardId
        is ChosenTarget.Spell -> t.spellEntityId
    }
}
