package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Invasion of Muraganda // Primordial Plasm.
 *
 * Front: enters, +1/+1 counter on target creature you control, then it fights up to one target
 * creature you don't control. Back: at the beginning of combat on your turn, another target
 * creature gets +2/+2 and loses all abilities until end of turn.
 */
class InvasionOfMuragandaScenarioTest : ScenarioTestBase() {

    init {
        test("enters: counter on your creature, then it fights the opponent's creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Muraganda")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Muraganda").error shouldBe null
            game.resolveStack()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val sel = game.submitDecision(
                TargetsResponse(game.state.pendingDecision!!.id, mapOf(0 to listOf(bears), 1 to listOf(giant)))
            )
            withClue("select: ${sel.error}") { sel.error shouldBe null }
            game.resolveStack()

            withClue("the 3/3 Bears deal 3 to the Giant and take 3") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }

        test("enters with no second target: just the counter, no fight") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Muraganda")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Muraganda").error shouldBe null
            game.resolveStack()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(bears)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.state.projectedState.getPower(bears) shouldBe 3
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.state.getEntity(game.findPermanent("Hill Giant")!!)
                ?.get<DamageComponent>()?.amount
                .let { (it ?: 0) shouldBe 0 }
        }

        test("Primordial Plasm: begin combat, another creature gets +2/+2 and loses all abilities") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Primordial Plasm")
                .withCardOnBattlefield(2, "Serra Angel")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            var guard = 0
            while (game.state.pendingDecision == null && game.state.step != Step.BEGIN_COMBAT && guard++ < 10) {
                game.passPriority()
            }
            guard = 0
            while (game.state.pendingDecision == null && guard++ < 5) game.passPriority()
            withClue("the begin-combat trigger asks for a target") {
                game.state.pendingDecision shouldNotBe null
            }
            val angel = game.findPermanent("Serra Angel")!!
            game.selectTargets(listOf(angel)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(angel) shouldBe 6
            projected.getToughness(angel) shouldBe 6
            projected.hasKeyword(angel, Keyword.FLYING) shouldBe false
            projected.hasKeyword(angel, Keyword.VIGILANCE) shouldBe false
        }
    }
}
