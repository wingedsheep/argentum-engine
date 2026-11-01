package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A bite run over a gathered collection ([com.wingedsheep.sdk.scripting.effects.ForEachEffect] over
 * `gather(ChosenTargets)`): "one or two target creatures you control each deal damage equal to their
 * power to target creature an opponent controls". Each chosen creature of ours lends its power and
 * takes nothing back, like a one-sided bite's source, so its slot reads
 * [TargetPolarity.BENEFICIAL]; the victim, named by the loop's body, reads [TargetPolarity.HARMFUL].
 *
 * Unread, the source slot fell back to the removal ranking, which orders our own creatures worst
 * first: Common Goal bit with Llanowar Elves and Grizzly Bears and left the opponent's Craw Wurm alive.
 */
class CollectionBiteReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.onBattlefield(playerId: EntityId, name: String): EntityId =
        state.getBattlefield(playerId).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("a collection bite's sources read as ours to lend; its victim as harmful") {
            withClue("Common Goal: one or two target creatures you control each deal damage to target creature") {
                intents.spellTargetPolarities("Common Goal") shouldBe listOf(TargetPolarity.HARMFUL, TargetPolarity.BENEFICIAL)
            }
        }

        test("Common Goal bites with our Craw Wurm, not Llanowar Elves and Grizzly Bears") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Common Goal")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Craw Wurm")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val cast = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { action ->
                    val spell = action.action as? CastSpell ?: return@first false
                    game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Common Goal"
                }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as CastSpell
            val targets = filled.targets.map { (it as ChosenTarget.Permanent).entityId }

            withClue("collection bite sources read as creatures we lend: our Craw Wurm's 6 kills theirs, Elves + Bears' 3 do not") {
                targets.first() shouldBe game.onBattlefield(game.player2Id, "Craw Wurm")
                targets shouldContain game.onBattlefield(game.player1Id, "Craw Wurm")
            }
        }
    }
}
