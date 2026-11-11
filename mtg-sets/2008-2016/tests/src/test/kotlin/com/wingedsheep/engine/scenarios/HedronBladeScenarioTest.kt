package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Hedron Blade — "Equipped creature gets +1/+1. Whenever equipped creature becomes blocked by one
 * or more colorless creatures, it gains deathtouch until end of turn."
 */
class HedronBladeScenarioTest : ScenarioTestBase() {
    init {
        fun game(vararg blockers: String) = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardAttachedTo(1, "Hedron Blade", "Hill Giant")
            .withCardOnBattlefield(1, "Mountain")
            .withCardOnBattlefield(1, "Mountain")
            .withCardInHand(1, "Shatter")
            .apply { blockers.forEach { withCardOnBattlefield(2, it) } }
            .apply { repeat(8) { withCardInLibrary(1, "Mountain") } }
            .apply { repeat(8) { withCardInLibrary(2, "Island") } }
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.attackAndBlock(vararg blockers: String) {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            declareBlockers(blockers.associateWith { listOf("Hill Giant") }).error shouldBe null
        }

        fun TestGame.giantHasDeathtouch() =
            state.projectedState.hasKeyword(findPermanent("Hill Giant")!!, Keyword.DEATHTOUCH)

        test("equipped creature gets +1/+1") {
            val game = game()
            val giant = game.findPermanent("Hill Giant")!!
            game.state.projectedState.getPower(giant) shouldBe 4
            game.state.projectedState.getToughness(giant) shouldBe 4
        }

        test("two colorless blockers trigger it once, and the equipped creature gains deathtouch") {
            val game = game("Ornithopter", "Bronze Sable")
            game.attackAndBlock("Ornithopter", "Bronze Sable")
            game.state.stack.size shouldBe 1
            game.giantHasDeathtouch() shouldBe false
            game.resolveStack()
            game.giantHasDeathtouch() shouldBe true
        }

        test("a colorless blocker beside a colored one still triggers once") {
            val game = game("Grizzly Bears", "Ornithopter")
            game.attackAndBlock("Grizzly Bears", "Ornithopter")
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.giantHasDeathtouch() shouldBe true
        }

        test("only colored blockers: no trigger, no deathtouch") {
            val game = game("Grizzly Bears", "Wind Drake")
            game.attackAndBlock("Grizzly Bears", "Wind Drake")
            game.state.stack.size shouldBe 0
            game.giantHasDeathtouch() shouldBe false
        }

        test("the equipped creature keeps the deathtouch grant when the Blade is destroyed in response") {
            val game = game("Ornithopter")
            game.attackAndBlock("Ornithopter")
            game.state.stack.size shouldBe 1
            // The defending player holds priority after declaring blocks; pass it back.
            if (game.state.priorityPlayerId != game.player1Id) game.passPriority()
            game.state.stack.size shouldBe 1
            game.castSpell(1, "Shatter", game.findPermanent("Hedron Blade")!!).error shouldBe null
            game.resolveStack()
            game.findPermanent("Hedron Blade") shouldBe null
            game.giantHasDeathtouch() shouldBe true
        }

        test("deathtouch lasts until end of turn") {
            val game = game("Ornithopter")
            game.attackAndBlock("Ornithopter")
            game.resolveStack()
            game.giantHasDeathtouch() shouldBe true
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.giantHasDeathtouch() shouldBe false
        }
    }
}
