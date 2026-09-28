package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Invasion of Regatha // Disciples of the Inferno.
 *
 * Front: enters, 4 damage to another target battle or opponent and 1 damage to up to one target
 * creature. Back: prowess; noncreature sources you control deal 2 extra damage to creatures,
 * battles and opponents.
 */
class InvasionOfRegathaScenarioTest : ScenarioTestBase() {

    init {
        test("enters: 4 damage to the opponent and 1 damage to a creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Regatha")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Regatha").error shouldBe null
            game.resolveStack()
            val giant = game.findPermanent("Hill Giant")!!
            val sel = game.submitDecision(
                TargetsResponse(game.state.pendingDecision!!.id, mapOf(0 to listOf(game.player2Id), 1 to listOf(giant)))
            )
            withClue("select: ${sel.error}") { sel.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 16
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.state.getEntity(giant)
                ?.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()?.amount shouldBe 1
        }

        test("enters with no creature target: still deals 4 to the opponent") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Regatha")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Regatha").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 16
        }

        test("Disciples of the Inferno: a noncreature spell deals 2 extra damage to an opponent") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Disciples of the Inferno")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()
            withClue("3 + 2") { game.getLifeTotal(2) shouldBe 15 }
        }

        test("Disciples of the Inferno: does not boost damage to yourself") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Disciples of the Inferno")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpellTargetingPlayer(1, "Lightning Bolt", 1)
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
        }

        test("Disciples of the Inferno: an opponent's spell is not boosted") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Disciples of the Inferno")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpellTargetingPlayer(2, "Lightning Bolt", 1)
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
        }
    }
}
