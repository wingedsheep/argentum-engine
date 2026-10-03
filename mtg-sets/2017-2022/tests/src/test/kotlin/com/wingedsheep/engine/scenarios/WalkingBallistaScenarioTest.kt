package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.aer.cards.WalkingBallista
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Walking Ballista (AER #181) — enters with X counters for {X}{X}, grows for {4}, and spends a
 * counter to ping any target.
 */
class WalkingBallistaScenarioTest : ScenarioTestBase() {
    init {
        val growId = WalkingBallista.activatedAbilities[0].id
        val pingId = WalkingBallista.activatedAbilities[1].id

        fun counters(game: TestGame) = game.state.getEntity(game.findPermanent("Walking Ballista")!!)!!
            .get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("X = 2 costs four mana and enters with two +1/+1 counters") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Walking Ballista")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val r = game.castXSpell(1, "Walking Ballista", 2)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()
            counters(game) shouldBe 2
            game.state.projectedState.getPower(game.findPermanent("Walking Ballista")!!) shouldBe 2
        }

        test("X = 2 can't be paid with three lands — it's {X}{X}, not {X}") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Walking Ballista")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castXSpell(1, "Walking Ballista", 2).error shouldNotBe null
        }

        test("{4} adds a counter; removing counters pings face and creatures") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Walking Ballista")
                .withCardOnBattlefield(2, "Memnite")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ballista = game.findPermanent("Walking Ballista")!!
            game.state = game.state.updateEntity(ballista) {
                it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
            }

            game.execute(ActivateAbility(game.player1Id, ballista, growId)).error shouldBe null
            game.resolveStack()
            counters(game) shouldBe 2

            game.execute(
                ActivateAbility(game.player1Id, ballista, pingId, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 19
            counters(game) shouldBe 1

            game.execute(
                ActivateAbility(
                    game.player1Id, ballista, pingId,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Memnite")!!))
                )
            ).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Memnite") shouldBe true
            withClue("the last counter was spent, so the 0/0 dies") {
                game.isInGraveyard(1, "Walking Ballista") shouldBe true
            }
        }
    }
}
