package com.wingedsheep.ai.llm

import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.ai.llm.decision.AiDecisionHandlerRegistry
import com.wingedsheep.engine.view.*
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.engine.core.*
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/** Flatten newlines in oracle text so it stays on a single line in prompts/logs. */
fun String.flattenOracle(): String = replace("\n", " / ")

/**
 * Converts game state, legal actions, and pending decisions into concise text
 * that an LLM can reason about.
 *
 * Entity IDs are mapped to short numeric labels, and legal actions to letter labels.
 */
class GameStateFormatter(
    private val decisionHandlerRegistry: AiDecisionHandlerRegistry = AiDecisionHandlerRegistry()
) {

    /**
     * Format a full state update for the LLM.
     */
    fun format(
        state: ClientGameState,
        legalActions: List<LegalActionInfo>,
        pendingDecision: PendingDecision?,
        recentGameLog: List<String> = emptyList()
    ): String {
        val sb = StringBuilder()

        // Recent game log — gives the AI context about what just happened
        if (recentGameLog.isNotEmpty()) {
            sb.appendLine("=== RECENT GAME LOG ===")
            for (entry in recentGameLog) {
                sb.appendLine("  - $entry")
            }
            sb.appendLine()
        }

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

        val combat = state.combat
        if (combat != null) {
            sb.appendLine()
            formatCombat(sb, combat, state, entityLabels)
        }

        // Format stack
        val stackZone = state.zones.find { it.zoneId.zoneType == Zone.STACK }
        if (stackZone != null && stackZone.cardIds.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("=== STACK (top first) ===")
            for (cardId in stackZone.cardIds.reversed()) {
                val card = state.cards[cardId] ?: continue
                val label = entityLabels[cardId] ?: cardId.value
                val cost = card.manaCost.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
                val typeLine = " — ${card.typeLine}"
                val description = card.stackText?.flattenOracle() ?: card.oracleText.takeIf { it.isNotBlank() }?.flattenOracle()
                val xValue = card.chosenX?.let { " (X=$it)" } ?: ""
                val targets = if (card.targets.isNotEmpty()) {
                    val targetNames = card.targets.mapNotNull { t ->
                        val tid = when (t) {
                            is ClientChosenTarget.Player -> t.playerId
                            is ClientChosenTarget.Permanent -> t.entityId
                            is ClientChosenTarget.Spell -> t.spellEntityId
                            is ClientChosenTarget.Card -> t.cardId
                        }
                        state.cards[tid]?.let { c ->
                            val owner = if (c.controllerId == state.viewingPlayerId) "your" else "opponent's"
                            "$owner ${c.name}"
                        } ?: state.players.find { it.playerId == tid }?.let { p ->
                            if (p.playerId == state.viewingPlayerId) "you" else "opponent"
                        }
                    }
                    if (targetNames.isNotEmpty()) " → targeting: ${targetNames.joinToString(", ")}" else ""
                } else ""
                sb.appendLine("  [$label] ${card.name}$cost$typeLine$xValue$targets${description?.let { " — \"$it\"" } ?: ""}")
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
     * Format a mulligan decision with full card info.
     */
    fun formatMulligan(
        cards: List<MulliganCardDisplay>,
        mulliganCount: Int,
        isOnThePlay: Boolean
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== MULLIGAN DECISION ===")
        sb.appendLine("You are on the ${if (isOnThePlay) "play" else "draw"}.")
        sb.appendLine("Mulligan count: $mulliganCount (keeping ${7 - mulliganCount} cards)")
        if (mulliganCount > 0) {
            sb.appendLine("If you keep, you must put $mulliganCount card(s) on the bottom of your library.")
        }
        sb.appendLine()

        // Categorize cards for quick overview
        val lands = cards.filter { it.typeLine?.contains("Land") == true }
        val nonLands = cards.filter { it.typeLine?.contains("Land") != true }
        val manaCosts = nonLands.mapNotNull { it.manaCost }

        sb.appendLine("Hand summary: ${lands.size} lands, ${nonLands.size} spells")
        sb.appendLine()

        sb.appendLine("Your hand:")
        for ((i, card) in cards.withIndex()) {
            sb.append("  [${i + 1}] ${card.name}")
            if (card.manaCost != null) sb.append(" ${card.manaCost}")
            if (card.typeLine != null) sb.append(" — ${card.typeLine}")
            if (card.power != null && card.toughness != null) sb.append(" ${card.power}/${card.toughness}")
            if (card.oracleText != null) sb.append(" — ${card.oracleText.flattenOracle()}")
            sb.appendLine()
        }
        sb.appendLine()
        sb.appendLine("MULLIGAN GUIDELINES:")
        sb.appendLine("- A good hand has 2-4 lands and a mix of early and late plays")
        sb.appendLine("- Hands with 0-1 lands or 6+ lands are almost always mulligans")
        sb.appendLine("- Consider whether you can cast your spells with the lands you have (color match)")
        sb.appendLine("- Each mulligan costs a card — a mediocre 7 is often better than a risky 6")
        sb.appendLine()
        sb.appendLine("Choose: [A] Keep hand, [B] Mulligan")
        return sb.toString()
    }

    /**
     * Format a choose-bottom-cards decision with full card info.
     */
    fun formatChooseBottomCards(
        cards: List<MulliganCardDisplay>,
        cardIds: List<EntityId>,
        count: Int
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== CHOOSE CARDS TO PUT ON BOTTOM ===")
        sb.appendLine("You must put $count card(s) on the bottom of your library.")
        sb.appendLine("Keep the cards that give you the best curve and color access.")
        sb.appendLine()
        sb.appendLine("Your hand:")
        for ((i, card) in cards.withIndex()) {
            sb.append("  [${actionLetter(i)}] ${card.name}")
            if (card.manaCost != null) sb.append(" ${card.manaCost}")
            if (card.typeLine != null) sb.append(" — ${card.typeLine}")
            if (card.power != null && card.toughness != null) sb.append(" ${card.power}/${card.toughness}")
            if (card.oracleText != null) sb.append(" — ${card.oracleText.flattenOracle()}")
            sb.appendLine()
        }
        sb.appendLine()
        sb.appendLine("Reply with the letter(s) of the $count card(s) to put on bottom.")
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

        // Mana pool (floating mana)
        val manaPool = player.manaPool
        if (isYou && manaPool != null && !manaPool.isEmpty) {
            val pool = manaPool
            val parts = mutableListOf<String>()
            if (pool.white > 0) parts.add("{W}x${pool.white}")
            if (pool.blue > 0) parts.add("{U}x${pool.blue}")
            if (pool.black > 0) parts.add("{B}x${pool.black}")
            if (pool.red > 0) parts.add("{R}x${pool.red}")
            if (pool.green > 0) parts.add("{G}x${pool.green}")
            if (pool.colorless > 0) parts.add("{C}x${pool.colorless}")
            sb.appendLine("Mana pool: ${parts.joinToString(", ")}")
        }

        // Active player effects
        if (player.activeEffects.isNotEmpty()) {
            for (effect in player.activeEffects) {
                sb.appendLine("Active effect: ${effect.name}${effect.description?.let { " — $it" } ?: ""}")
            }
        }

        // Battlefield
        val battlefield = state.zones
            .filter { it.zoneId.zoneType == Zone.BATTLEFIELD && it.zoneId.ownerId == player.playerId }
            .flatMap { it.cardIds }
            .mapNotNull { id -> state.cards[id]?.let { id to it } }

        if (battlefield.isNotEmpty()) {
            // Available mana summary (count untapped lands/mana sources)
            if (isYou) {
                val untappedMana = computeAvailableMana(battlefield)
                if (untappedMana.isNotEmpty()) {
                    sb.appendLine("Untapped lands: ${untappedMana.joinToString("")} (${untappedMana.size} sources)")
                }
            }

            sb.appendLine("Battlefield:")
            for ((id, card) in battlefield) {
                sb.append("  [${ labels[id] ?: "?" }] ")
                formatBattlefieldCard(sb, card, state)
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
                    sb.append("  [$label] ${card.name}")
                    if (card.manaCost.isNotBlank()) sb.append(" ${card.manaCost}")
                    sb.append(" — ${card.typeLine}")
                    if (card.power != null && card.toughness != null) sb.append(" ${card.power}/${card.toughness}")
                    if (card.keywords.isNotEmpty()) sb.append(" [${card.keywords.joinToString(", ") { it.name.lowercase() }}]")
                    if (card.abilityFlags.isNotEmpty()) sb.append(" [${card.abilityFlags.joinToString(", ") { it.displayName }}]")
                    if (card.oracleText.isNotBlank()) sb.append(" — \"${card.oracleText.flattenOracle()}\"")
                    sb.appendLine()
                }
            }
        }

        // Graveyard with full card info
        if (player.graveyardSize > 0) {
            val graveyardZone = state.zones
                .find { it.zoneId.zoneType == Zone.GRAVEYARD && it.zoneId.ownerId == player.playerId }
            val graveyardCards = graveyardZone?.cardIds?.mapNotNull { id ->
                state.cards[id]?.let { card ->
                    val cost = card.manaCost.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
                    val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
                    "${card.name}$cost$stats"
                }
            } ?: emptyList()
            if (graveyardCards.isNotEmpty()) {
                sb.appendLine("Graveyard (${player.graveyardSize}): ${graveyardCards.joinToString(", ")}")
            } else {
                sb.appendLine("Graveyard: ${player.graveyardSize} cards")
            }
        }

        // Exile with card names
        if (player.exileSize > 0) {
            val exileZone = state.zones
                .find { it.zoneId.zoneType == Zone.EXILE && it.zoneId.ownerId == player.playerId }
            val exileCards = exileZone?.cardIds?.mapNotNull { id ->
                state.cards[id]?.name
            } ?: emptyList()
            if (exileCards.isNotEmpty()) {
                sb.appendLine("Exile (${player.exileSize}): ${exileCards.joinToString(", ")}")
            } else {
                sb.appendLine("Exile: ${player.exileSize} cards")
            }
        }
    }

    /**
     * Format a single battlefield permanent with full projected state.
     */
    private fun formatBattlefieldCard(sb: StringBuilder, card: ClientCard, state: ClientGameState) {
        sb.append(card.name)

        // Mana cost (for all permanents, helps LLM assess mana value/threat level)
        if (card.manaCost.isNotBlank()) sb.append(" ${card.manaCost}")

        // Type line (always show — critical for knowing what type a permanent is)
        sb.append(" — ${card.typeLine}")

        // Power/toughness with base stats comparison and damage
        val power = card.power
        val toughness = card.toughness
        if (power != null && toughness != null) {
            sb.append(" $power/$toughness")
            if (card.basePower != null && card.baseToughness != null &&
                (card.basePower != power || card.baseToughness != toughness)) {
                sb.append(" (base ${card.basePower}/${card.baseToughness})")
            }
            val damage = card.damage
            if (damage != null && damage > 0) {
                val remaining = toughness - damage
                sb.append(" [$damage dmg, $remaining remaining]")
            }
        }

        // State flags
        if (card.isTapped) sb.append(" TAPPED")
        if (card.hasSummoningSickness) sb.append(" (summoning sick)")
        if (card.isFaceDown) sb.append(" (face-down)")

        // Keywords
        if (card.keywords.isNotEmpty()) {
            sb.append(" [${card.keywords.joinToString(", ") { it.name.lowercase() }}]")
        }

        // Ability flags (non-keyword abilities like "can't be blocked")
        if (card.abilityFlags.isNotEmpty()) {
            sb.append(" [${card.abilityFlags.joinToString(", ") { it.displayName }}]")
        }

        // Protection
        if (card.protections.isNotEmpty()) {
            sb.append(" [protection from ${card.protections.joinToString(", ") { it.name.lowercase() }}]")
        }

        // Counters
        if (card.counters.isNotEmpty()) {
            sb.append(" {${card.counters.entries.joinToString(", ") { "${it.value} ${it.key.name.lowercase()}" }}}")
        }

        // Attachments (equipment, auras)
        if (card.attachments.isNotEmpty()) {
            val attachNames = card.attachments.mapNotNull { state.cards[it]?.name }
            if (attachNames.isNotEmpty()) sb.append(" equipped: ${attachNames.joinToString(", ")}")
        }

        // Linked exile (e.g., Oblivion Ring exiled cards)
        if (card.linkedExile.isNotEmpty()) {
            val exiledNames = card.linkedExile.mapNotNull { state.cards[it]?.name }
            if (exiledNames.isNotEmpty()) sb.append(" (exiling: ${exiledNames.joinToString(", ")})")
        }

        // Active temporary effects
        if (card.activeEffects.isNotEmpty()) {
            for (effect in card.activeEffects) {
                sb.append(" <${effect.description}>")
            }
        }

        // Chosen type/color (e.g., Doom Cannon choosing a creature type)
        if (card.chosenCreatureType != null) sb.append(" (chosen type: ${card.chosenCreatureType})")
        if (card.chosenColor != null) sb.append(" (chosen color: ${card.chosenColor})")

        // Oracle text (skip for face-down cards and basic lands with no rules text beyond mana)
        if (!card.isFaceDown && card.oracleText.isNotBlank()) {
            sb.append(" — \"${card.oracleText.flattenOracle()}\"")
        }
    }

    /**
     * Compute available mana from untapped lands/sources for the AI mana summary line.
     */
    private fun computeAvailableMana(battlefield: List<Pair<EntityId, ClientCard>>): List<String> {
        val manaSymbols = mutableListOf<String>()
        for ((_, card) in battlefield) {
            if ("Land" !in card.cardTypes || card.isTapped) continue
            // Parse oracle text to find mana production
            val text = card.oracleText
            when {
                card.subtypes.contains("Plains") -> manaSymbols.add("{W}")
                card.subtypes.contains("Island") -> manaSymbols.add("{U}")
                card.subtypes.contains("Swamp") -> manaSymbols.add("{B}")
                card.subtypes.contains("Mountain") -> manaSymbols.add("{R}")
                card.subtypes.contains("Forest") -> manaSymbols.add("{G}")
                // Dual/multi lands — show first mana symbol from oracle text
                text.contains("Add {W}") && text.contains("Add {B}") -> manaSymbols.add("{W}/{B}")
                text.contains("Add {W}") && text.contains("Add {U}") -> manaSymbols.add("{W}/{U}")
                text.contains("Add {W}") && text.contains("Add {R}") -> manaSymbols.add("{W}/{R}")
                text.contains("Add {W}") && text.contains("Add {G}") -> manaSymbols.add("{W}/{G}")
                text.contains("Add {U}") && text.contains("Add {B}") -> manaSymbols.add("{U}/{B}")
                text.contains("Add {U}") && text.contains("Add {R}") -> manaSymbols.add("{U}/{R}")
                text.contains("Add {U}") && text.contains("Add {G}") -> manaSymbols.add("{U}/{G}")
                text.contains("Add {B}") && text.contains("Add {R}") -> manaSymbols.add("{B}/{R}")
                text.contains("Add {B}") && text.contains("Add {G}") -> manaSymbols.add("{B}/{G}")
                text.contains("Add {R}") && text.contains("Add {G}") -> manaSymbols.add("{R}/{G}")
                text.contains("Add {W}") -> manaSymbols.add("{W}")
                text.contains("Add {U}") -> manaSymbols.add("{U}")
                text.contains("Add {B}") -> manaSymbols.add("{B}")
                text.contains("Add {R}") -> manaSymbols.add("{R}")
                text.contains("Add {G}") -> manaSymbols.add("{G}")
                text.contains("Add {C}") -> manaSymbols.add("{C}")
            }
        }
        return manaSymbols
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
                val aDmg = card?.damage
                val damage = if (aDmg != null && aDmg > 0) " ($aDmg dmg)" else ""
                val keywords = card?.keywords?.takeIf { it.isNotEmpty() }?.let { kws ->
                    " [${kws.joinToString(", ") { it.name.lowercase() }}]"
                } ?: ""
                val abilityFlags = card?.abilityFlags?.takeIf { it.isNotEmpty() }?.let { flags ->
                    " [${flags.joinToString(", ") { it.displayName }}]"
                } ?: ""
                val blockerNames = attacker.blockedBy.mapNotNull { state.cards[it]?.name }
                val blockedStr = if (blockerNames.isNotEmpty()) " blocked by: ${blockerNames.joinToString(", ")}" else " (unblocked)"
                sb.appendLine("  [$label] ${attacker.creatureName}$stats$damage$keywords$abilityFlags$blockedStr")
            }
        }
        if (combat.blockers.isNotEmpty()) {
            sb.appendLine("Blockers:")
            for (blocker in combat.blockers) {
                val card = state.cards[blocker.creatureId]
                val label = labels[blocker.creatureId] ?: "?"
                val stats = if (card != null) " ${card.power}/${card.toughness}" else ""
                val bDmg = card?.damage
                val damage = if (bDmg != null && bDmg > 0) " ($bDmg dmg)" else ""
                val keywords = card?.keywords?.takeIf { it.isNotEmpty() }?.let { kws ->
                    " [${kws.joinToString(", ") { it.name.lowercase() }}]"
                } ?: ""
                val attackerName = state.cards[blocker.blockingAttacker]?.name ?: "?"
                sb.appendLine("  [$label] ${blocker.creatureName}$stats$damage$keywords blocking $attackerName")
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

            // X-cost spells: show the range of X values the player can choose
            if (action.hasXCost && action.maxAffordableX != null) {
                sb.append(" (X can be ${action.minX}-${action.maxAffordableX})")
            }

            // Additional costs: show sacrifice/discard/etc. requirements
            val costInfo = action.additionalCostInfo
            if (costInfo != null) {
                sb.append(" [additional cost: ${costInfo.description}]")
            }

            // Convoke: show that creatures can help pay
            val convokeCreatures = action.validConvokeCreatures
            if (action.hasConvoke && !convokeCreatures.isNullOrEmpty()) {
                val creatureNames = convokeCreatures.map { c ->
                    val colors = c.colors.joinToString("/") { it.name.lowercase() }
                    "${c.name} ($colors)"
                }
                sb.append(" [Convoke: tap creatures to help pay — ${creatureNames.joinToString(", ")}]")
            }

            // Delve: show that graveyard cards can reduce cost
            val delveCards = action.validDelveCards
            if (action.hasDelve && !delveCards.isNullOrEmpty()) {
                val minNeeded = action.minDelveNeeded ?: 0
                sb.append(" [Delve: exile graveyard cards to pay {1} each, ${delveCards.size} available")
                if (minNeeded > 0) sb.append(", need at least $minNeeded")
                sb.append("]")
            }

            // Inline targets for LLM selection
            if (action.requiresTargets) {
                // Show oracle text so the LLM knows what the spell does when picking targets
                val oracleText = (action.action as? CastSpell)?.cardId?.let { cardId ->
                    state.cards[cardId]?.oracleText?.takeIf { it.isNotBlank() }?.flattenOracle()
                }
                if (oracleText != null && oracleText !in action.description) {
                    sb.append(" — \"$oracleText\"")
                }

                // Gather all valid targets from targetRequirements or validTargets
                val targetReqs = action.targetRequirements
                val validTargets = action.validTargets
                val allTargets = if (!targetReqs.isNullOrEmpty()) {
                    targetReqs.flatMap { req ->
                        req.validTargets.map { tid ->
                            formatTarget(tid, state, labels)
                        }
                    }
                } else if (!validTargets.isNullOrEmpty()) {
                    validTargets.map { tid ->
                        formatTarget(tid, state, labels)
                    }
                } else {
                    emptyList()
                }

                if (allTargets.size >= 2) {
                    sb.appendLine()
                    sb.append("    Targets: ")
                    sb.appendLine(allTargets.mapIndexed { j, (_, name) ->
                        "[${j + 1}] $name"
                    }.joinToString(", "))
                    sb.append("    Reply \"$letter\" + target number (e.g., \"${letter}1\" for ${allTargets.first().second})")
                } else if (allTargets.size == 1) {
                    sb.append(" — target: ${allTargets.first().second}")
                }
            }
            sb.appendLine()
        }
    }

    private fun formatTarget(
        tid: EntityId,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ): Pair<EntityId, String> {
        val card = state.cards[tid]
        val label = labels[tid] ?: tid.value
        return if (card != null) {
            val owner = if (card.controllerId == state.viewingPlayerId) "your" else "opponent's"
            val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
            val tapped = if (card.isTapped) " TAPPED" else ""
            val keywords = card.keywords.takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { it.name.lowercase() }
                ?.let { " [$it]" } ?: ""
            Pair(tid, "[$label] $owner ${card.name}$stats$tapped$keywords")
        } else {
            val playerName = if (tid == state.viewingPlayerId) "you" else "opponent"
            val life = state.players.find { it.playerId == tid }?.life
            val lifeStr = if (life != null) " (life: $life)" else ""
            Pair(tid, "[$label] $playerName$lifeStr")
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

        // Delegate to the handler registry
        val handler = decisionHandlerRegistry.getHandler(decision)
        if (handler != null) {
            handler.format(sb, decision, state, labels)
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

/**
 * Card display info for mulligan/bottom-cards decisions.
 */
data class MulliganCardDisplay(
    val name: String,
    val manaCost: String? = null,
    val typeLine: String? = null,
    val power: Int? = null,
    val toughness: Int? = null,
    val oracleText: String? = null
)
