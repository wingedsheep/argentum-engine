package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Unweighted position facts used by Phase 9's offline evaluator fit.
 *
 * These deliberately contain no hand-tuned aggregation: the training script, rather than Kotlin,
 * decides how much a life point, untapped creature, or card in hand is worth. Battlefield facts
 * are read from [ProjectedState], so continuous type and P/T changes are represented correctly.
 */
@Serializable
data class RawBoardFeatures(
    val myLife: Int,
    val opponentLife: Int,
    val lifeDifference: Int,
    val myBurnRangeLife: Int,
    val opponentBurnRangeLife: Int,
    val creatureCountDifference: Int,
    val totalPowerDifference: Int,
    val totalToughnessDifference: Int,
    val evasiveCreatureDifference: Int,
    val untappedCreatureDifference: Int,
    val artifactCountDifference: Int,
    val enchantmentCountDifference: Int,
    val planeswalkerCountDifference: Int,
    val landCountDifference: Int,
    val planeswalkerLoyaltyDifference: Int,
    val handSizeDifference: Int,
    val myHandSize: Int,
    val opponentHandSize: Int,
    val untappedLandDifference: Int,
    val graveyardSizeDifference: Int,
    val summoningSickCreatureDifference: Int,
    val librarySizeDifference: Int,
    val removalInHandDifference: Int,
    val threatsInPlayDifference: Int,
    val turnNumber: Int,
    val isMyTurn: Int,
) {
    internal fun weightedSum(weights: Map<String, Double>): Double =
        myLife * weights.getValue("myLife") +
            opponentLife * weights.getValue("opponentLife") +
            lifeDifference * weights.getValue("lifeDifference") +
            myBurnRangeLife * weights.getValue("myBurnRangeLife") +
            opponentBurnRangeLife * weights.getValue("opponentBurnRangeLife") +
            creatureCountDifference * weights.getValue("creatureCountDifference") +
            totalPowerDifference * weights.getValue("totalPowerDifference") +
            totalToughnessDifference * weights.getValue("totalToughnessDifference") +
            evasiveCreatureDifference * weights.getValue("evasiveCreatureDifference") +
            untappedCreatureDifference * weights.getValue("untappedCreatureDifference") +
            artifactCountDifference * weights.getValue("artifactCountDifference") +
            enchantmentCountDifference * weights.getValue("enchantmentCountDifference") +
            planeswalkerCountDifference * weights.getValue("planeswalkerCountDifference") +
            landCountDifference * weights.getValue("landCountDifference") +
            planeswalkerLoyaltyDifference * weights.getValue("planeswalkerLoyaltyDifference") +
            handSizeDifference * weights.getValue("handSizeDifference") +
            myHandSize * weights.getValue("myHandSize") +
            opponentHandSize * weights.getValue("opponentHandSize") +
            untappedLandDifference * weights.getValue("untappedLandDifference") +
            graveyardSizeDifference * weights.getValue("graveyardSizeDifference") +
            summoningSickCreatureDifference * weights.getValue("summoningSickCreatureDifference") +
            librarySizeDifference * weights.getValue("librarySizeDifference") +
            removalInHandDifference * weights.getValue("removalInHandDifference") +
            threatsInPlayDifference * weights.getValue("threatsInPlayDifference") +
            turnNumber * weights.getValue("turnNumber") +
            isMyTurn * weights.getValue("isMyTurn")

    companion object {
        val names: Set<String> = setOf(
            "myLife", "opponentLife", "lifeDifference", "myBurnRangeLife", "opponentBurnRangeLife",
            "creatureCountDifference", "totalPowerDifference", "totalToughnessDifference",
            "evasiveCreatureDifference", "untappedCreatureDifference", "artifactCountDifference",
            "enchantmentCountDifference", "planeswalkerCountDifference", "landCountDifference",
            "planeswalkerLoyaltyDifference", "handSizeDifference", "myHandSize", "opponentHandSize",
            "untappedLandDifference", "graveyardSizeDifference", "summoningSickCreatureDifference",
            "librarySizeDifference", "removalInHandDifference", "threatsInPlayDifference", "turnNumber",
            "isMyTurn",
        )

        fun extract(
            state: GameState,
            projected: ProjectedState,
            playerId: EntityId,
            intents: IntentCatalog,
        ): RawBoardFeatures {
            val opponent: EntityId? = state.getOpponents(playerId).firstOrNull()
            val mine = facts(state, projected, playerId, intents)
            val theirs = opponent?.let { facts(state, projected, it, intents) } ?: Facts()
            val myLife = state.lifeTotal(playerId)
            val opponentLife = opponent?.let(state::lifeTotal) ?: 0
            return RawBoardFeatures(
                myLife, opponentLife, myLife - opponentLife,
                myLife.coerceAtMost(7), opponentLife.coerceAtMost(7),
                mine.creatures - theirs.creatures,
                mine.power - theirs.power,
                mine.toughness - theirs.toughness,
                mine.evasive - theirs.evasive,
                mine.untappedCreatures - theirs.untappedCreatures,
                mine.artifacts - theirs.artifacts,
                mine.enchantments - theirs.enchantments,
                mine.planeswalkers - theirs.planeswalkers,
                mine.lands - theirs.lands,
                mine.loyalty - theirs.loyalty,
                mine.hand - theirs.hand, mine.hand, theirs.hand,
                mine.untappedLands - theirs.untappedLands,
                mine.graveyard - theirs.graveyard,
                mine.summoningSick - theirs.summoningSick,
                mine.library - theirs.library,
                mine.removalInHand - theirs.removalInHand,
                mine.threatsInPlay - theirs.threatsInPlay,
                state.turnNumber,
                if (state.activePlayerId == playerId) 1 else 0,
            )
        }

        private fun facts(
            state: GameState,
            projected: ProjectedState,
            playerId: EntityId,
            intents: IntentCatalog,
        ): Facts {
            var result = Facts(
                hand = state.getHand(playerId).size,
                graveyard = state.getGraveyard(playerId).size,
                library = state.getLibrary(playerId).size,
            )
            for (entityId in projected.getBattlefieldControlledBy(playerId)) {
                val types = projected.getTypes(entityId)
                val entity = state.getEntity(entityId) ?: continue
                val card = entity.get<CardComponent>()
                if (CardType.CREATURE.name in types) {
                    val keywords = projected.getKeywords(entityId)
                    result = result.copy(
                        creatures = result.creatures + 1,
                        power = result.power + (projected.getPower(entityId) ?: 0),
                        toughness = result.toughness + (projected.getToughness(entityId) ?: 0),
                        evasive = result.evasive + if (keywords.any { it in EVASION }) 1 else 0,
                        untappedCreatures = result.untappedCreatures + if (!entity.has<TappedComponent>()) 1 else 0,
                        summoningSick = result.summoningSick + if (entity.has<SummoningSicknessComponent>()) 1 else 0,
                    )
                }
                result = result.copy(
                    artifacts = result.artifacts + if (CardType.ARTIFACT.name in types) 1 else 0,
                    enchantments = result.enchantments + if (CardType.ENCHANTMENT.name in types) 1 else 0,
                    planeswalkers = result.planeswalkers + if (CardType.PLANESWALKER.name in types) 1 else 0,
                    loyalty = result.loyalty + if (CardType.PLANESWALKER.name in types)
                        (entity.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0) else 0,
                    lands = result.lands + if (CardType.LAND.name in types) 1 else 0,
                    untappedLands = result.untappedLands + if (CardType.LAND.name in types && !entity.has<TappedComponent>()) 1 else 0,
                    // A *permanent's* threat, not its card's: a Room's locked half and an already
                    // spent Adventure are text that is not on the battlefield, so they must not
                    // count here. [IntentCatalog.forPermanent] is the same reading `BoardPresence`
                    // prices the permanent by.
                    threatsInPlay = result.threatsInPlay + if (card != null &&
                        intents.forPermanent(entity, card.name)
                            .any { intent -> intent.tags.any { it in THREAT_TAGS } }
                    ) 1 else 0,
                )
            }
            val removal = state.getHand(playerId).count { id ->
                state.getEntity(id)?.get<CardComponent>()?.let { intents.forName(it.name) }
                    ?.tags?.any { it in REMOVAL_TAGS } == true
            }
            return result.copy(removalInHand = removal)
        }

        private val EVASION = setOf(
            Keyword.FLYING.name, Keyword.MENACE.name, Keyword.FEAR.name,
            Keyword.INTIMIDATE.name, Keyword.SHADOW.name,
        )
        private val REMOVAL_TAGS = setOf(IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL, IntentTag.SWEEPER)
        private val THREAT_TAGS = setOf(IntentTag.ANTHEM, IntentTag.TOKEN_MAKER, IntentTag.TAPPER)
    }
}

private data class Facts(
    val creatures: Int = 0, val power: Int = 0, val toughness: Int = 0,
    val evasive: Int = 0, val untappedCreatures: Int = 0,
    val artifacts: Int = 0, val enchantments: Int = 0, val planeswalkers: Int = 0,
    val lands: Int = 0, val loyalty: Int = 0, val hand: Int = 0,
    val untappedLands: Int = 0, val graveyard: Int = 0, val summoningSick: Int = 0,
    val library: Int = 0, val removalInHand: Int = 0, val threatsInPlay: Int = 0,
)
