package com.wingedsheep.gameserver.ai

import com.wingedsheep.gameserver.dto.*
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.engine.core.*
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Converts game state, legal actions, and pending decisions into concise text
 * that an LLM can reason about.
 *
 * Entity IDs are mapped to short numeric labels, and legal actions to letter labels.
 */
class GameStateFormatter {

    /**
     * Format a full state update for the LLM.
     */
    fun format(
        state: ClientGameState,
        legalActions: List<LegalActionInfo>,
        pendingDecision: PendingDecision?
    ): String {
        val sb = StringBuilder()

        // Build entity ID label map for readable references
        val entityLabels = buildEntityLabels(state)

        sb.appendLine("=== GAME STATE ===")
        sb.appendLine("Turn ${state.turnNumber} | Phase: ${state.currentPhase} | Step: ${state.currentStep}")
        sb.appendLine("Active: ${if (state.activePlayerId == state.viewingPlayerId) "You" else "Opponent"} | Priority: ${if (state.priorityPlayerId == state.viewingPlayerId) "You" else "Opponent"}")
        sb.appendLine()

        // Format players
        val you = state.players.find { it.playerId == state.viewingPlayerId }
        val opponent = state.players.find { it.playerId != state.viewingPlayerId }

        if (you != null) {
            sb.appendLine("-- YOU --")
            formatPlayer(sb, you, state, entityLabels, isYou = true)
        }
        sb.appendLine()
        if (opponent != null) {
            sb.appendLine("-- OPPONENT --")
            formatPlayer(sb, opponent, state, entityLabels, isYou = false)
        }

        // Format combat state
        if (state.combat != null) {
            sb.appendLine()
            formatCombat(sb, state.combat, state, entityLabels)
        }

        // Format stack
        val stackZone = state.zones.find { it.zoneId.zoneType == Zone.STACK }
        if (stackZone != null && stackZone.cardIds.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("=== STACK (top first) ===")
            for (cardId in stackZone.cardIds.reversed()) {
                val card = state.cards[cardId] ?: continue
                val label = entityLabels[cardId] ?: cardId.value
                val description = card.stackText ?: card.oracleText.takeIf { it.isNotBlank() }
                sb.appendLine("  [$label] ${card.name}${description?.let { " — $it" } ?: ""}")
            }
        }

        // Format pending decision or legal actions
        if (pendingDecision != null) {
            sb.appendLine()
            formatDecision(sb, pendingDecision, state, entityLabels)
        } else if (legalActions.isNotEmpty()) {
            sb.appendLine()
            formatLegalActions(sb, legalActions, state, entityLabels)
        }

        return sb.toString()
    }

    /**
     * Format a mulligan decision.
     */
    fun formatMulligan(cardNames: List<String>, mulliganCount: Int, isOnThePlay: Boolean): String {
        val sb = StringBuilder()
        sb.appendLine("=== MULLIGAN DECISION ===")
        sb.appendLine("You are on the ${if (isOnThePlay) "play" else "draw"}.")
        sb.appendLine("Mulligan count: $mulliganCount (keeping ${7 - mulliganCount} cards)")
        sb.appendLine()
        sb.appendLine("Your hand:")
        for ((i, name) in cardNames.withIndex()) {
            sb.appendLine("  [${i + 1}] $name")
        }
        sb.appendLine()
        sb.appendLine("Choose: [A] Keep hand, [B] Mulligan")
        return sb.toString()
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private fun buildEntityLabels(state: ClientGameState): Map<EntityId, String> {
        val labels = mutableMapOf<EntityId, String>()
        var counter = 1
        // Label all visible cards
        for ((id, _) in state.cards) {
            labels[id] = counter.toString()
            counter++
        }
        // Label players
        for (player in state.players) {
            if (player.playerId !in labels) {
                labels[player.playerId] = "P${if (player.playerId == state.viewingPlayerId) "1" else "2"}"
            }
        }
        return labels
    }

    private fun formatPlayer(
        sb: StringBuilder,
        player: ClientPlayer,
        state: ClientGameState,
        labels: Map<EntityId, String>,
        isYou: Boolean
    ) {
        sb.appendLine("Life: ${player.life} | Hand: ${player.handSize} | Library: ${player.librarySize}")

        // Battlefield
        val battlefield = state.zones
            .filter { it.zoneId.zoneType == Zone.BATTLEFIELD && it.zoneId.ownerId == player.playerId }
            .flatMap { it.cardIds }
            .mapNotNull { id -> state.cards[id]?.let { id to it } }

        if (battlefield.isNotEmpty()) {
            sb.appendLine("Battlefield:")
            // Lands
            val lands = battlefield.filter { "Land" in it.second.cardTypes }
            if (lands.isNotEmpty()) {
                val landGroups = lands.groupBy { Triple(it.second.name, it.second.isTapped, it.second.controllerId) }
                for ((key, group) in landGroups) {
                    val (name, tapped, _) = key
                    val tappedStr = if (tapped) " (tapped)" else ""
                    if (group.size > 1) {
                        sb.appendLine("  ${group.size}x $name$tappedStr")
                    } else {
                        val label = labels[group.first().first] ?: "?"
                        sb.appendLine("  [$label] $name$tappedStr")
                    }
                }
            }
            // Non-lands
            val nonLands = battlefield.filter { "Land" !in it.second.cardTypes }
            for ((id, card) in nonLands) {
                val label = labels[id] ?: "?"
                sb.append("  [$label] ${card.name}")
                if (card.power != null && card.toughness != null) {
                    sb.append(" ${card.power}/${card.toughness}")
                    if (card.damage != null && card.damage > 0) sb.append(" (${card.damage} dmg)")
                }
                if (card.isTapped) sb.append(" (tapped)")
                if (card.hasSummoningSickness) sb.append(" (summoning sick)")
                if (card.isFaceDown) sb.append(" (face-down)")
                val keywords = card.keywords
                if (keywords.isNotEmpty()) sb.append(" [${keywords.joinToString(", ") { it.name.lowercase() }}]")
                if (card.counters.isNotEmpty()) {
                    sb.append(" {${card.counters.entries.joinToString(", ") { "${it.value} ${it.key.name.lowercase()}" }}}")
                }
                if (card.attachments.isNotEmpty()) {
                    val attachNames = card.attachments.mapNotNull { state.cards[it]?.name }
                    if (attachNames.isNotEmpty()) sb.append(" equipped: ${attachNames.joinToString(", ")}")
                }
                // Oracle text for non-vanilla creatures (skip if face-down since text is hidden)
                if (!card.isFaceDown && card.oracleText.isNotBlank()) {
                    sb.append(" — \"${card.oracleText}\"")
                }
                sb.appendLine()
            }
        }

        // Hand (only for "you")
        if (isYou) {
            val handZone = state.zones
                .find { it.zoneId.zoneType == Zone.HAND && it.zoneId.ownerId == player.playerId }
            val handCards = handZone?.cardIds?.mapNotNull { id -> state.cards[id]?.let { id to it } } ?: emptyList()
            if (handCards.isNotEmpty()) {
                sb.appendLine("Hand:")
                for ((id, card) in handCards) {
                    val label = labels[id] ?: "?"
                    sb.append("  [$label] ${card.name} ${card.manaCost}")
                    sb.append(" — ${card.typeLine}")
                    if (card.power != null && card.toughness != null) sb.append(" ${card.power}/${card.toughness}")
                    if (card.keywords.isNotEmpty()) sb.append(" [${card.keywords.joinToString(", ") { it.name.lowercase() }}]")
                    if (card.oracleText.isNotBlank()) sb.append(" — \"${card.oracleText}\"")
                    sb.appendLine()
                }
            }
        }

        // Graveyard
        if (player.graveyardSize > 0) {
            val graveyardZone = state.zones
                .find { it.zoneId.zoneType == Zone.GRAVEYARD && it.zoneId.ownerId == player.playerId }
            val graveyardCards = graveyardZone?.cardIds?.mapNotNull { id ->
                state.cards[id]?.let { card ->
                    val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
                    "${card.name}$stats"
                }
            } ?: emptyList()
            if (graveyardCards.isNotEmpty()) {
                sb.appendLine("Graveyard (${player.graveyardSize}): ${graveyardCards.joinToString(", ")}")
            } else {
                sb.appendLine("Graveyard: ${player.graveyardSize} cards")
            }
        }

        // Exile summary
        if (player.exileSize > 0) {
            sb.appendLine("Exile: ${player.exileSize} cards")
        }
    }

    private fun formatCombat(
        sb: StringBuilder,
        combat: ClientCombatState,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("=== COMBAT ===")
        if (combat.attackers.isNotEmpty()) {
            sb.appendLine("Attackers:")
            for (attacker in combat.attackers) {
                val card = state.cards[attacker.creatureId]
                val label = labels[attacker.creatureId] ?: "?"
                val stats = if (card != null) " ${card.power}/${card.toughness}" else ""
                val keywords = card?.keywords?.takeIf { it.isNotEmpty() }?.let { kws ->
                    " [${kws.joinToString(", ") { it.name.lowercase() }}]"
                } ?: ""
                val blockerNames = attacker.blockedBy.mapNotNull { state.cards[it]?.name }
                val blockedStr = if (blockerNames.isNotEmpty()) " blocked by: ${blockerNames.joinToString(", ")}" else " (unblocked)"
                sb.appendLine("  [$label] ${attacker.creatureName}$stats$keywords$blockedStr")
            }
        }
        if (combat.blockers.isNotEmpty()) {
            sb.appendLine("Blockers:")
            for (blocker in combat.blockers) {
                val card = state.cards[blocker.creatureId]
                val label = labels[blocker.creatureId] ?: "?"
                val stats = if (card != null) " ${card.power}/${card.toughness}" else ""
                val keywords = card?.keywords?.takeIf { it.isNotEmpty() }?.let { kws ->
                    " [${kws.joinToString(", ") { it.name.lowercase() }}]"
                } ?: ""
                val attackerName = state.cards[blocker.blockingAttacker]?.name ?: "?"
                sb.appendLine("  [$label] ${blocker.creatureName}$stats$keywords blocking $attackerName")
            }
        }
    }

    private fun formatLegalActions(
        sb: StringBuilder,
        actions: List<LegalActionInfo>,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("=== LEGAL ACTIONS ===")
        for ((i, action) in actions.withIndex()) {
            val letter = actionLetter(i)
            sb.append("[$letter] ${action.description}")
            if (action.manaCostString != null) sb.append(" ${action.manaCostString}")
            if (!action.isAffordable) sb.append(" (can't afford)")
            if (action.requiresTargets && action.targetRequirements != null) {
                val targetDescs = action.targetRequirements.map { req ->
                    val validNames = req.validTargets?.mapNotNull { tid ->
                        val card = state.cards[tid]
                        val label = labels[tid] ?: tid.value
                        if (card != null) "[$label] ${card.name}" else "[$label] player"
                    } ?: emptyList()
                    "${req.description}: ${validNames.joinToString(", ")}"
                }
                sb.append(" — targets: ${targetDescs.joinToString("; ")}")
            }
            sb.appendLine()
        }
    }

    private fun formatDecision(
        sb: StringBuilder,
        decision: PendingDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("=== DECISION REQUIRED ===")
        sb.appendLine(decision.prompt)
        sb.appendLine()

        when (decision) {
            is ChooseTargetsDecision -> {
                for (req in decision.targetRequirements) {
                    sb.appendLine("Target ${req.index + 1}: ${req.description} (choose ${req.minTargets}-${req.maxTargets})")
                    val validIds = decision.legalTargets[req.index] ?: emptyList()
                    for ((j, tid) in validIds.withIndex()) {
                        val card = state.cards[tid]
                        val label = labels[tid] ?: tid.value
                        val name = card?.name ?: "Player"
                        val letter = actionLetter(j)
                        sb.appendLine("  [$letter] [$label] $name${card?.let { " ${it.power ?: ""}/${it.toughness ?: ""}" } ?: ""}")
                    }
                }
            }

            is SelectCardsDecision -> {
                sb.appendLine("Select ${decision.minSelections}-${decision.maxSelections} card(s):")
                for ((j, eid) in decision.options.withIndex()) {
                    val card = state.cards[eid]
                    val info = decision.cardInfo?.get(eid)
                    val name = card?.name ?: info?.name ?: "Unknown"
                    val letter = actionLetter(j)
                    sb.appendLine("  [$letter] $name")
                }
            }

            is YesNoDecision -> {
                sb.appendLine("[A] ${decision.yesText}")
                sb.appendLine("[B] ${decision.noText}")
            }

            is ChooseModeDecision -> {
                sb.appendLine("Choose ${decision.minModes}-${decision.maxModes} mode(s):")
                for (mode in decision.modes) {
                    val letter = actionLetter(mode.index)
                    val available = if (mode.available) "" else " (unavailable)"
                    sb.appendLine("  [$letter] ${mode.text}$available")
                }
            }

            is ChooseColorDecision -> {
                sb.appendLine("Choose a color:")
                val colors = decision.availableColors.toList()
                for ((j, color) in colors.withIndex()) {
                    sb.appendLine("  [${actionLetter(j)}] ${color.name}")
                }
            }

            is ChooseNumberDecision -> {
                sb.appendLine("Choose a number between ${decision.minValue} and ${decision.maxValue}.")
                sb.appendLine("Reply with the number.")
            }

            is DistributeDecision -> {
                sb.appendLine("Distribute ${decision.totalAmount} among targets (min ${decision.minPerTarget} each):")
                for ((j, tid) in decision.targets.withIndex()) {
                    val card = state.cards[tid]
                    val name = card?.name ?: "Player"
                    val label = labels[tid] ?: tid.value
                    sb.appendLine("  [${actionLetter(j)}] [$label] $name")
                }
                sb.appendLine("Reply with amounts per target (e.g., \"A:2, B:1\").")
            }

            is OrderObjectsDecision -> {
                sb.appendLine("Order these objects (first receives priority):")
                for ((j, eid) in decision.objects.withIndex()) {
                    val card = state.cards[eid]
                    val info = decision.cardInfo?.get(eid)
                    val name = card?.name ?: info?.name ?: "Unknown"
                    sb.appendLine("  [${actionLetter(j)}] $name")
                }
                sb.appendLine("Reply with the order (e.g., \"B, A, C\").")
            }

            is SplitPilesDecision -> {
                sb.appendLine("Split into ${decision.numberOfPiles} piles:")
                if (decision.pileLabels.isNotEmpty()) {
                    sb.appendLine("Piles: ${decision.pileLabels.joinToString(", ")}")
                }
                for ((j, eid) in decision.cards.withIndex()) {
                    val card = state.cards[eid]
                    val info = decision.cardInfo?.get(eid)
                    val name = card?.name ?: info?.name ?: "Unknown"
                    sb.appendLine("  [${actionLetter(j)}] $name")
                }
                sb.appendLine("Reply with pile assignment (e.g., \"Pile 1: A, B; Pile 2: C\").")
            }

            is ChooseOptionDecision -> {
                for ((j, option) in decision.options.withIndex()) {
                    sb.appendLine("  [${actionLetter(j)}] $option")
                }
            }

            is AssignDamageDecision -> {
                // Should be handled by auto-default, but format just in case
                sb.appendLine("Assign ${decision.availablePower} damage:")
                for ((j, tid) in decision.orderedTargets.withIndex()) {
                    val name = state.cards[tid]?.name ?: "Creature"
                    val min = decision.minimumAssignments[tid] ?: 0
                    sb.appendLine("  [${actionLetter(j)}] $name (min: $min)")
                }
                if (decision.hasTrample && decision.defenderId != null) {
                    sb.appendLine("  Remaining goes to defending player (trample)")
                }
            }

            is SearchLibraryDecision -> {
                sb.appendLine("Search library (${decision.filterDescription}):")
                sb.appendLine("Select ${decision.minSelections}-${decision.maxSelections} card(s):")
                for ((j, eid) in decision.options.withIndex()) {
                    val info = decision.cards[eid]
                    val name = info?.name ?: "Unknown"
                    val cost = info?.manaCost ?: ""
                    val type = info?.typeLine ?: ""
                    sb.appendLine("  [${actionLetter(j)}] $name $cost — $type")
                }
                if (decision.minSelections == 0) {
                    sb.appendLine("  [${actionLetter(decision.options.size)}] Fail to find (select nothing)")
                }
            }

            is ReorderLibraryDecision -> {
                sb.appendLine("Reorder cards on top of library (first = top):")
                for ((j, eid) in decision.cards.withIndex()) {
                    val info = decision.cardInfo[eid]
                    val name = info?.name ?: "Unknown"
                    sb.appendLine("  [${actionLetter(j)}] $name")
                }
                sb.appendLine("Reply with the order (e.g., \"B, A, C\").")
            }

            is SelectManaSourcesDecision -> {
                // Should be handled by auto-pay shortcut
                sb.appendLine("Select mana sources to pay ${decision.requiredCost}:")
                sb.appendLine("Reply: [A] Auto Pay")
            }
        }
    }

    companion object {
        fun actionLetter(index: Int): String {
            if (index < 26) return ('A' + index).toString()
            // For more than 26 options, use AA, AB, etc.
            return "A${('A' + (index - 26))}"
        }

        fun letterToIndex(letter: String): Int? {
            val upper = letter.uppercase().trim()
            if (upper.length == 1 && upper[0] in 'A'..'Z') {
                return upper[0] - 'A'
            }
            if (upper.length == 2 && upper[0] == 'A' && upper[1] in 'A'..'Z') {
                return 26 + (upper[1] - 'A')
            }
            return null
        }
    }
}

