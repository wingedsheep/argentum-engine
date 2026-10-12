package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import java.lang.reflect.Modifier
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Whether a player's cards **use their own graveyard** — the question behind every "target player
 * mills N" and "exile target card from a graveyard" answer.
 *
 * The board evaluator has no library or graveyard term, so a mill aimed at either player scores the
 * same and the simulation alone can't choose. Milling the opponent is the default; milling yourself
 * is a real play when your deck turns graveyard cards into value. This reads that structurally, per
 * card definition, with no per-card table.
 *
 * ## What counts as a graveyard payoff
 *
 * A card is a payoff when anything in its definition (every face, back face and keyword ability)
 *
 *  - carries a keyword that is cast or activated from, or feeds on, the graveyard — flashback,
 *    escape, delve, dredge, disturb, embalm, unearth, harmonize, mayhem, aftermath;
 *  - has an ability that works from the graveyard (`activateFromZone`, a trigger's `activeZones`,
 *    a state trigger's `activeZone`);
 *  - moves cards out of a graveyard (`fromZone` / `fromZones` — recursion, cast-from-graveyard
 *    permissions);
 *  - counts or reads **your** graveyard — any object holding `zone = GRAVEYARD` beside
 *    `player = You` (`DynamicAmount.Count(You, GRAVEYARD)` for threshold-style counts, the
 *    distinct-type aggregation behind delirium, a gather from your graveyard).
 *
 * The walk is reflective over the SDK's pure-data definition graph, so a new effect, amount or
 * keyword ability that names those fields is recognized without touching this file. A false
 * positive (a graveyard *hate* card that exiles `fromZone = GRAVEYARD`) costs little: a deck is
 * only read as a graveyard deck past [MIN_PAYOFFS] payoff cards.
 *
 * Results are memoized per card name, like [CardIntentAnalyzer].
 */
object GraveyardSynergy {

    /** Payoff cards a player must own before their graveyard is treated as a resource. */
    const val MIN_PAYOFFS = 4

    private val GRAVEYARD_KEYWORDS: Set<Keyword> = setOf(
        Keyword.FLASHBACK, Keyword.ESCAPE, Keyword.DELVE, Keyword.DREDGE, Keyword.DISTURB,
        Keyword.EMBALM, Keyword.UNEARTH, Keyword.HARMONIZE, Keyword.MAYHEM, Keyword.AFTERMATH,
    )

    private val ZONE_FIELDS = setOf("activateFromZone", "activeZone", "fromZone", "sourceZone", "copyFromZone")
    private val ZONE_SET_FIELDS = setOf("activeZones", "fromZones")
    private val OWN_ZONE_FIELDS = setOf("zone", "zones")

    private val cache = ConcurrentHashMap<String, Boolean>()

    /** Whether [card] gets value from cards in its owner's graveyard. */
    fun isPayoff(card: CardDefinition): Boolean = cache.getOrPut(card.name) { Walk().visit(card) }

    /**
     * How many of [player]'s nonland cards — library, hand, graveyard and battlefield, i.e. the
     * deck as its owner knows it — are graveyard payoffs.
     */
    fun payoffCount(state: GameState, player: EntityId, registry: CardRegistry): Int {
        var count = 0
        for (zone in OWNED_ZONES) {
            for (id in state.getZone(player, zone)) {
                val card = state.getEntity(id)?.get<CardComponent>() ?: continue
                if (card.typeLine.isLand) continue
                if (zone == Zone.BATTLEFIELD && card.ownerId != null && card.ownerId != player) continue
                val definition = registry.getCard(card.cardDefinitionId) ?: continue
                if (isPayoff(definition)) count++
            }
        }
        return count
    }

    /** Whether [player]'s deck uses its graveyard enough that filling it is worth something. */
    fun benefitsFromGraveyard(state: GameState, player: EntityId, registry: CardRegistry): Boolean =
        payoffCount(state, player, registry) >= MIN_PAYOFFS

    private val OWNED_ZONES = listOf(Zone.LIBRARY, Zone.HAND, Zone.GRAVEYARD, Zone.BATTLEFIELD)

    /** One reflective walk over a definition graph, stopping at the first payoff signal. */
    private class Walk {
        private val seen = IdentityHashMap<Any, Unit>()

        fun visit(node: Any?): Boolean {
            if (node == null || node is String || node is Number || node is Boolean || node is Char) return false
            if (node is Keyword) return node in GRAVEYARD_KEYWORDS
            if (node is Enum<*>) return false
            if (seen.put(node, Unit) != null) return false
            if (node is KeywordAbility && node.keyword in GRAVEYARD_KEYWORDS) return true
            return when (node) {
                is Collection<*> -> node.any(::visit)
                is Map<*, *> -> node.keys.any(::visit) || node.values.any(::visit)
                is Array<*> -> node.any(::visit)
                else -> if (node.javaClass.name.startsWith(SDK_PACKAGE)) visitFields(node) else false
            }
        }

        private fun visitFields(node: Any): Boolean {
            val fields = fieldsOf(node.javaClass)
            val values = fields.associate { it.name to runCatching { it.get(node) }.getOrNull() }
            for ((name, value) in values) {
                when (name) {
                    in ZONE_FIELDS -> if (value == Zone.GRAVEYARD) return true
                    in ZONE_SET_FIELDS -> if (value is Collection<*> && Zone.GRAVEYARD in value) return true
                    in OWN_ZONE_FIELDS -> {
                        val graveyard = value == Zone.GRAVEYARD || (value is Collection<*> && Zone.GRAVEYARD in value)
                        if (graveyard && values["player"] == Player.You) return true
                    }
                }
            }
            return values.values.any(::visit)
        }
    }

    private const val SDK_PACKAGE = "com.wingedsheep.sdk."

    private val fieldCache = ConcurrentHashMap<Class<*>, List<java.lang.reflect.Field>>()

    private fun fieldsOf(type: Class<*>): List<java.lang.reflect.Field> = fieldCache.getOrPut(type) {
        generateSequence(type) { it.superclass }
            .takeWhile { it.name.startsWith(SDK_PACKAGE) }
            .flatMap { it.declaredFields.asSequence() }
            .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            .filter { runCatching { it.isAccessible = true }.isSuccess }
            .toList()
    }
}
