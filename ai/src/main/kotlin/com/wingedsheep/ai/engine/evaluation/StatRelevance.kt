package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlin.math.ln

/**
 * What a creature's power and toughness are worth *against the table it is facing* —
 * [CreatureValuation.statsReadTheBoard].
 *
 * The linear `power × 1.0 + toughness × 0.4` makes a 1000/1000 worth fifty 20/20s. It is not: both
 * kill anything they fight, both win the game in a hit or two, and both die to the same Murder.
 * Stats are worth what they *do*, and what they do is cross thresholds:
 *
 * - **Diminishing returns** ([soften]). Power is linear up to [POWER_KNEE] and toughness up to
 *   [TOUGHNESS_KNEE]; past the knee each further point is worth less (a logarithmic tail), so a
 *   1000/1000 prices at a little under twice a 20/20 rather than fifty times. The knees are fixed
 *   numbers on purpose: a knee that moved with the opponent's life total or board would make
 *   *damaging the opponent* or *killing their creature* lower the value of our own big creature,
 *   and the search would learn to avoid both.
 * - **Board thresholds** ([thresholdBonus]). A 5/5 facing 4/4s kills whatever blocks it and
 *   survives whatever it blocks; a 4/4 facing the same board only trades. [BEATS] is paid for the
 *   share of opposing creatures it would win a fight against — kill *and* survive — [OUTLASTS]
 *   for the share whose power its toughness survives (none, against deathtouch), and
 *   [OUT_OF_REACH] when its toughness is above the biggest size-limited removal the opponent has
 *   *shown*. A trade earns nothing on either side, and that is load-bearing: a bonus for merely
 *   killing let our Hill Giants beside a Serra Angel out-earn their Hill Giants (which cannot kill
 *   the Angel), so an even Giant-for-Giant trade read as a loss and the AI stopped pressing a lead.
 *   Shares rather than "beats the biggest one" for the same reason, one creature at a time. Both
 *   read the leaf's board, with temporary pumps taken off, and both move the right way: removing
 *   the creature that walls ours raises our creatures' worth. An empty opposing board pays both in
 *   full, so killing their last creature never costs us a bonus.
 *
 * "Shown" removal is public information only: damage and -N/-N spells in the opponent's graveyard
 * and repeatable ones on their battlefield (a Prodigal Sorcerer), read through
 * [com.wingedsheep.ai.engine.knowledge.CardIntent.removalReach]. It is read at the decision *root*
 * ([StatContext.shownReachAt]) rather than at each leaf: read at the leaf, casting our own Shock
 * would put it in our graveyard and raise every big opposing creature's value mid-line — the AI
 * would be charged for casting removal. Destruction and exile have no reach, and toughness is no
 * defence against them, so they leave this term alone.
 */
object StatRelevance {
    /** Power past which each further point is worth less — two hits of it kill from 20. */
    const val POWER_KNEE = 10

    /** Toughness past which each further point is worth less — few attackers or burn spells reach it. */
    const val TOUGHNESS_KNEE = 8

    /** How slowly the tail flattens: the first point past the knee is still worth almost a full point. */
    private const val TAIL = 3.0

    /** Paid in full when it wins a fight against every opposing creature: kills it and survives it. */
    const val BEATS = 0.75

    /** Paid in full when its toughness survives every opposing creature's power. */
    const val OUTLASTS = 0.5

    /** Toughness above the biggest damage or -N/-N removal the opponent has shown. */
    const val OUT_OF_REACH = 0.75

    /** [stat] with diminishing returns past [knee]. Identity at or below the knee. */
    fun soften(stat: Int, knee: Int): Double =
        if (stat <= knee) stat.toDouble() else knee + TAIL * ln(1.0 + (stat - knee) / TAIL)

    fun softPower(power: Int): Double = soften(power, POWER_KNEE)

    fun softToughness(toughness: Int): Double = soften(toughness, TOUGHNESS_KNEE)

    /**
     * The step bonuses for a creature with [power]/[toughness] and [keywords] facing [context].
     * Its own deathtouch kills whatever it fights, and indestructible survives whatever it fights,
     * whatever the numbers say.
     */
    fun thresholdBonus(power: Int, toughness: Int, keywords: Set<String>, context: StatContext): Double {
        val indestructible = Keyword.INDESTRUCTIBLE.name in keywords
        val opposing = context.opposingToughnesses.size
        val deathtouch = Keyword.DEATHTOUCH.name in keywords
        var beaten = 0
        var survived = 0
        for (i in 0 until opposing) {
            val survives = indestructible || toughness > context.opposingPowers[i]
            if (!survives) continue
            survived++
            if (power > 0 && (deathtouch || power >= context.opposingToughnesses[i])) beaten++
        }
        var bonus = if (context.wholeBoard) {
            (if (beaten == opposing) BEATS else 0.0) + (if (survived == opposing) OUTLASTS else 0.0)
        } else {
            BEATS * share(beaten, opposing) + OUTLASTS * share(survived, opposing)
        }
        if (opposing == 0 && power <= 0) bonus -= BEATS // a 0-power wall wins no fight, even on an empty board
        val reach = context.shownReach
        if (reach != null && (indestructible || toughness > reach)) bonus += OUT_OF_REACH
        return bonus
    }

    /** [count] of [total], with an empty board counting as beaten in full. */
    private fun share(count: Int, total: Int): Double = if (total == 0) 1.0 else count.toDouble() / total
}

/**
 * The opposing side's numbers that [StatRelevance.thresholdBonus] compares a creature against.
 *
 * @property opposingPowers each opposing creature's settled power — [Int.MAX_VALUE] for one with
 *   deathtouch and power to use it, which no toughness survives.
 * @property opposingToughnesses each opposing creature's settled toughness, in the same order.
 * @property shownReach the biggest size-limited removal the opponents have shown at the decision
 *   root, or null when they have shown none (or the evaluation was not rooted).
 * @property wholeBoard [CreatureValuation.thresholdsWholeBoard]: pay each fight bonus only when the
 *   creature beats (or outlasts) *every* opposing creature, instead of the share it does. A share
 *   has the opposing board in its denominator, so a creature it beats raises it: our own Savannah
 *   Lions made every opposing creature that eats it worth more, and with four of them that came to
 *   more than the Lions itself — losing it for nothing scored as a gain. All-or-nothing agrees with
 *   the share wherever the share is 0 or 1 (an empty board, a board all beaten, one it cannot
 *   beat), and a creature that cannot fight it back no longer moves anything.
 */
class StatContext(
    val opposingPowers: IntArray,
    val opposingToughnesses: IntArray,
    val shownReach: Int?,
    val wholeBoard: Boolean = false,
) {
    companion object {
        /** The context for creatures controlled by [controller], read off this leaf's board. */
        fun facing(
            state: GameState,
            projected: ProjectedState,
            controller: EntityId,
            shownReach: Map<EntityId, Int>?,
            wholeBoard: Boolean = false,
        ): StatContext {
            val powers = ArrayList<Int>()
            val toughnesses = ArrayList<Int>()
            var reach: Int? = null
            for (opponent in state.getOpponents(controller)) {
                for (id in projected.getBattlefieldControlledBy(opponent)) {
                    if (!projected.isCreature(id)) continue
                    // Settled stats, as for the creature being priced: a pump that is gone at
                    // cleanup must not read as having outgrown — or out-toughed — anything.
                    val (powerMod, toughnessMod) = ExpiringStats.temporaryModification(state, id)
                    val p = (projected.getPower(id) ?: 0) - powerMod
                    powers += if (p > 0 && Keyword.DEATHTOUCH.name in projected.getKeywords(id)) Int.MAX_VALUE else p
                    toughnesses += (projected.getToughness(id) ?: 0) - toughnessMod
                }
                shownReach?.get(opponent)?.let { reach = maxOf(reach ?: 0, it) }
            }
            return StatContext(powers.toIntArray(), toughnesses.toIntArray(), reach, wholeBoard)
        }

        /**
         * Per player, the biggest [com.wingedsheep.ai.engine.knowledge.CardIntent.removalReach] they
         * have shown at [root]: instants and sorceries in their graveyard, and repeatable removal on
         * their battlefield. Players who have shown none are absent.
         */
        fun shownReachAt(root: GameState, intents: IntentCatalog): Map<EntityId, Int> {
            if (intents === IntentCatalog.NONE) return emptyMap()
            val projected = root.projectedState
            val result = HashMap<EntityId, Int>()
            for (player in root.turnOrder) {
                var best: Int? = null
                for (id in root.getZone(player, Zone.GRAVEYARD)) {
                    val card = root.getEntity(id)?.get<CardComponent>() ?: continue
                    if (!card.typeLine.isInstant && !card.typeLine.isSorcery) continue
                    val intent = intents.forName(card.name) ?: continue
                    if (IntentTag.REMOVAL !in intent.tags) continue
                    intent.removalReach?.let { best = maxOf(best ?: 0, it) }
                }
                for (id in projected.getBattlefieldControlledBy(player)) {
                    val container = root.getEntity(id) ?: continue
                    val card = container.get<CardComponent>() ?: continue
                    for (intent in intents.forPermanent(container, card.name)) {
                        if (!intent.repeatable || IntentTag.REMOVAL !in intent.tags) continue
                        intent.removalReach?.let { best = maxOf(best ?: 0, it) }
                    }
                }
                best?.let { result[player] = it }
            }
            return result
        }
    }
}
