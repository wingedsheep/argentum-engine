package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Invasion of Belenon // Belenon War Anthem — the Siege's enters trigger makes a 2/2 white and
 * blue Knight with vigilance; defeating it casts the back face, an anthem for creatures you
 * control only.
 */
class InvasionOfBelenonScenarioTest : ScenarioTestBase() {

    init {
        test("it enters with 5 defense counters and creates a 2/2 white and blue vigilant Knight") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Belenon")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Belenon").error shouldBe null
            game.resolveStack()
            game.resolveStack()

            val siege = game.findPermanent("Invasion of Belenon")!!
            game.state.getEntity(siege)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) shouldBe 5

            val knights = game.findPermanents("Knight Token")
            knights.size shouldBe 1
            val knight = knights.single()
            val projected = game.state.projectedState
            projected.getPower(knight) shouldBe 2
            projected.getToughness(knight) shouldBe 2
            projected.hasKeyword(knight, Keyword.VIGILANCE) shouldBe true
            projected.getColors(knight) shouldBe setOf(Color.WHITE.name, Color.BLUE.name)
        }

        test("defeating it casts Belenon War Anthem, which pumps only your creatures") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Belenon")
                .withCardOnBattlefield(1, "Serra Angel", summoningSickness = false)
                .withCardOnBattlefield(1, "Shivan Dragon", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf(
                    "Serra Angel" to "Invasion of Belenon",
                    "Shivan Dragon" to "Invasion of Belenon",
                )
            ).error shouldBe null

            var guard = 0
            while (game.state.pendingDecision == null && guard++ < 30) {
                if (game.state.step == Step.DECLARE_BLOCKERS &&
                    game.state.getEntity(game.player2Id)?.has<BlockersDeclaredThisCombatComponent>() != true
                ) {
                    game.declareNoBlockers()
                } else {
                    game.passPriority()
                }
            }
            withClue("the defeat trigger offers to cast the back face") {
                game.state.pendingDecision shouldNotBe null
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Belenon War Anthem") shouldBe true
            val projected = game.state.projectedState
            projected.getPower(game.findPermanent("Serra Angel")!!) shouldBe 5
            projected.getToughness(game.findPermanent("Serra Angel")!!) shouldBe 5
            withClue("an opponent's creature is not pumped") {
                projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
            }
        }
    }
}
