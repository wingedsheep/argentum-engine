package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Five-Alarm Fire — one blaze counter per creature of yours that deals combat damage (a trampler
 * hitting a blocker and the player still adds one; a double striker adds two), and removing five
 * deals 5 damage to any target.
 */
class FiveAlarmFireScenarioTest : ScenarioTestBase() {

    private fun blaze(game: TestGame): Int =
        game.state.getEntity(game.findPermanent("Five-Alarm Fire")!!)
            ?.get<CountersComponent>()?.getCount(CounterType.BLAZE) ?: 0

    private fun setBlaze(game: TestGame, amount: Int) {
        val fire = game.findPermanent("Five-Alarm Fire")!!
        game.state = game.state.updateEntity(fire) { c ->
            c.with(CountersComponent().withAdded(CounterType.BLAZE, amount))
        }
    }

    private fun activate(game: TestGame, target: ChosenTarget) = game.execute(
        ActivateAbility(
            playerId = game.player1Id,
            sourceId = game.findPermanent("Five-Alarm Fire")!!,
            abilityId = cardRegistry.getCard("Five-Alarm Fire")!!.script.activatedAbilities.single().id,
            targets = listOf(target),
        )
    )

    init {
        test("each of your creatures dealing combat damage adds one counter, a trampler hitting two recipients still one") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Five-Alarm Fire")
                .withCardOnBattlefield(1, "Colossal Dreadmaw")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Colossal Dreadmaw" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Colossal Dreadmaw"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("Dreadmaw trampled 3 over Hill Giant and Bears connected: 3 + 2 damage") {
                game.getLifeTotal(2) shouldBe 15
            }
            withClue("two creatures dealt combat damage; the blocking Hill Giant is not yours") {
                blaze(game) shouldBe 2
            }
        }

        test("a double striker adds a counter in each combat damage step") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Five-Alarm Fire")
                .withCardOnBattlefield(1, "Fencing Ace")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Fencing Ace" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 18
            blaze(game) shouldBe 2
        }

        test("removing five blaze counters deals 5 damage to any target; four is not enough") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Five-Alarm Fire")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            setBlaze(game, 4)
            withClue("four counters can't pay the cost") {
                activate(game, ChosenTarget.Player(game.player2Id)).error shouldNotBe null
            }

            setBlaze(game, 6)
            activate(game, ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!)).error shouldBe null
            blaze(game) shouldBe 1
            game.resolveStack()
            game.isInGraveyard(2, "Hill Giant") shouldBe true

            setBlaze(game, 5)
            activate(game, ChosenTarget.Player(game.player2Id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 15
            blaze(game) shouldBe 0
        }
    }
}
