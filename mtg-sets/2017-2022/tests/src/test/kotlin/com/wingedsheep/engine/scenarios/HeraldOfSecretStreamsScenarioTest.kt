package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Herald of Secret Streams (XLN #59; {3}{U}, 2/3).
 *
 *   Creatures you control with +1/+1 counters on them can't be blocked.
 *
 * The first `CantBeBlocked` whose group is gated on a counter state predicate: only the creatures
 * of yours that carry a +1/+1 counter are unblockable — not the Herald itself, not your uncountered
 * creatures, and not an opponent's countered creature.
 */
class HeraldOfSecretStreamsScenarioTest : ScenarioTestBase() {

    init {
        context("Herald of Secret Streams") {

            test("your creature with a +1/+1 counter can't be blocked; one without a counter can") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Herald of Secret Streams", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Wall of Air", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                addPlusOne(game, game.findPermanent("Grizzly Bears")!!)

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("the countered Bears can't be blocked") {
                    game.declareBlockers(mapOf("Wall of Air" to listOf("Grizzly Bears"))).error shouldNotBe null
                }
                withClue("the uncountered Giant can be blocked") {
                    game.declareBlockers(mapOf("Wall of Air" to listOf("Hill Giant"))).error shouldBe null
                }
            }

            test("an opponent's creature with a +1/+1 counter can still be blocked") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Herald of Secret Streams", summoningSickness = false)
                    .withCardOnBattlefield(1, "Wall of Air", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                addPlusOne(game, game.findPermanent("Grizzly Bears")!!)

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.declareBlockers(mapOf("Wall of Air" to listOf("Grizzly Bears"))).error shouldBe null
            }
        }
    }

    private fun addPlusOne(game: TestGame, id: EntityId) {
        game.state = game.state.updateEntity(id) { c ->
            val counters = c.get<CountersComponent>() ?: CountersComponent()
            c.with(counters.withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        }
    }
}
