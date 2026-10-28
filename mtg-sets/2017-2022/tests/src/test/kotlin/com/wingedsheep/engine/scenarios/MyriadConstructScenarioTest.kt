package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Myriad Construct (ZNR #246) — {4} Artifact Creature — Construct, 4/4.
 *
 *   Kicker {3}
 *   If this creature was kicked, it enters with a +1/+1 counter on it for each nonbasic land your
 *   opponents control.
 *   When this creature becomes the target of a spell, sacrifice it and create a number of 1/1
 *   colorless Construct artifact creature tokens equal to its power.
 *
 * The kicked count reads only the opponents' nonbasic lands; the token count is the Construct's
 * power as it last existed on the battlefield (it is sacrificed before the tokens are made).
 */
class MyriadConstructScenarioTest : ScenarioTestBase() {

    private fun TestGame.cast(kicked: Boolean) {
        val cardId = findCardsInHand(1, "Myriad Construct").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = actions.single { it.actionType == if (kicked) "CastWithKicker" else "CastSpell" }
        execute(chosen.action).error shouldBe null
        resolveStack()
    }

    private fun TestGame.plusCounters(): Int =
        state.getEntity(findPermanent("Myriad Construct")!!)!!
            .get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Myriad Construct")
        .withLandsOnBattlefield(1, "Plains", 7)
        .withCardOnBattlefield(1, "Terramorphic Expanse")
        .withCardOnBattlefield(2, "Terramorphic Expanse")
        .withCardOnBattlefield(2, "Terramorphic Expanse")
        .withLandsOnBattlefield(2, "Island", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("kicked, it enters with a counter per nonbasic land the opponents control") {
            val game = board()
            game.cast(kicked = true)

            withClue("two opposing nonbasic lands; your own and basics don't count") {
                game.plusCounters() shouldBe 2
            }
            game.state.projectedState.getPower(game.findPermanent("Myriad Construct")!!) shouldBe 6
        }

        test("unkicked, it enters with no counters") {
            val game = board()
            game.cast(kicked = false)

            game.plusCounters() shouldBe 0
        }

        test("targeted by a spell, it is sacrificed and leaves tokens equal to its last-known power") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Myriad Construct")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val construct = game.findPermanent("Myriad Construct")!!
            game.state = game.state.updateEntity(construct) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }

            game.execute(
                CastSpell(
                    game.player1Id,
                    game.findCardsInHand(1, "Shock").single(),
                    listOf(ChosenTarget.Permanent(construct)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.findPermanent("Myriad Construct") shouldBe null
            game.isInGraveyard(1, "Myriad Construct") shouldBe true
            withClue("a 5/5 when sacrificed makes five Constructs") {
                game.findPermanents("Construct Token").size shouldBe 5
            }
        }
    }
}
