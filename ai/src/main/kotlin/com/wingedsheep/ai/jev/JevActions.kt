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
                val defenders: List<EntityId?> = info.validAttackTargets.orEmpty() +
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
        val targets = if (modal != null) modeTargets.flatten() else targets(
            info.targetRequirements ?: if (info.requiresTargets) listOf(LegalActionTargetInfo(
                index = 0, description = info.targetDescription ?: "Target", minTargets = info.minTargets,
                maxTargets = info.targetCount, validTargets = info.validTargets.orEmpty(),
                xConstrainsManaValue = info.xConstrainsTargetManaValue,
                xConstrainsManaValueExactly = info.xConstrainsTargetManaValueExactly,
                xConstrainsPower = info.xConstrainsTargetPower, xConstrainsCount = info.xConstrainsTargetCount,
            )) else emptyList(), x)
        val costs = listOfNotNull(info.additionalCostInfo) + modes.mapNotNull { it.additionalCostInfo } +
            List((modes.size - 1).coerceAtLeast(0)) { modal?.additionalCostPerExtraMode }.filterNotNull()
        val payment = costs.map(::cost).reduceOrNull(::combine)
        val manaPayment = manaPayment(info)
        val alternative = alternative(info)
        val damage = if (info.requiresDamageDistribution && info.totalDamageToDistribute != null) {
            JevDecisions(q, label).distribute("Divide damage", targets.map(::targetId), info.totalDamageToDistribute!!, info.minDamagePerTarget ?: 1)
        } else null
        return when (action) {
            is CastSpell -> action.copy(targets = targets.ifEmpty { action.targets }, xValue = x ?: action.xValue,
                additionalCostPayment = payment ?: action.additionalCostPayment, paymentStrategy = manaPayment,
                alternativePayment = alternative.takeUnless { it.isEmpty } ?: action.alternativePayment, damageDistribution = damage,
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
        requirements.flatMap { r ->
            val options = r.validTargets.filter { id ->
                val card = state.cards[id]
                (x == null || !r.xConstrainsManaValue || card == null || card.manaValue <= x) &&
                    (x == null || !r.xConstrainsManaValueExactly || card?.manaValue == x) &&
                    (x == null || !r.xConstrainsPower || card?.power == x)
            }
            cards(r.description, options, r.minTargets,
                if (r.xConstrainsCount && x != null) minOf(x, r.maxTargets) else r.maxTargets).map(::target)
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

    private fun cost(c: AdditionalCostInfo): AdditionalCostPayment {
        fun select(prompt: String, ids: List<EntityId>, min: Int, max: Int = min) =
            if (ids.isEmpty()) emptyList() else cards(prompt, ids, min, max)
        val removals = mutableListOf<DistributedCounterRemoval>()
        var remaining = c.distributedCounterRemovalTotal
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
            else select("Exile for ${c.description}; required total weight ${c.exileMinTotalWeight}",
                c.validExileTargets, c.exileMinCount, c.exileMaxCount),
            beheldCards = select("Behold for ${c.description}", c.validBeholdTargets, c.beholdCount),
            revealedCards = select("Reveal for ${c.description}", c.validRevealTargets, c.revealCount),
            blightTargets = select("Blight for ${c.description}", c.validBlightTargets, 1),
            blightAmount = if (c.blightVariableMaxX > 0) q.number("Choose blight X", 0, c.blightVariableMaxX) else 0,
            payXLifeAmount = if (c.payXLifeMaxX > 0) q.number("Choose life to pay", 0, c.payXLifeMaxX) else 0,
            variableCostPermanents = if (variableSacrifice)
                select("Sacrifice for ${c.description}", c.validSacrificeTargets, c.sacrificeCount, c.validSacrificeTargets.size)
            else cards("Tap creatures with total power at least ${c.tapForPowerRequired}", c.tapForPowerCreatures.map { it.entityId }),
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
        variableCostPermanents = a.variableCostPermanents + b.variableCostPermanents,
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
