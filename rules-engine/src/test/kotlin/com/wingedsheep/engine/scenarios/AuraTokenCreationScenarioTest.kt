package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `CreateAuraTokenEffect` on the Role path — the effect Roles share with other named Aura tokens
 * (Preston Garvey's Settlement has its own scenario test):
 *
 *  - CR 303.4i: an "up to one target" left unchosen means the object is undefined, so no Role token
 *    is created, and the rest of the spell still resolves.
 *  - CR 303.7a: a new Role on a creature replaces the Role the same player already controls there.
 */
class AuraTokenCreationScenarioTest : ScenarioTestBase() {

    private fun roleCount(game: TestGame, name: String): Int =
        game.state.getBattlefield().count { game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == name }

    init {
        test("Cut In with no creature chosen for the Role deals its damage and creates no Role (CR 303.4i)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withCardInHand(1, "Cut In")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Cut In", giant).error shouldBe null
            game.resolveStack()

            withClue("the damage half resolves") { game.findPermanent("Hill Giant") shouldBe null }
            withClue("no Young Hero Role without a chosen creature") { roleCount(game, "Young Hero Role") shouldBe 0 }
        }

        test("a second Role from the same player replaces the first on that creature (CR 303.7a)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Monstrous Rage")
                .withCardInHand(1, "Monstrous Rage")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bear = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Monstrous Rage", bear).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Monstrous Rage", bear).error shouldBe null
            game.resolveStack()

            withClue("only the newer Monster Role remains") { roleCount(game, "Monster Role") shouldBe 1 }
            withClue("2/2 + 2×(+2/+0) + one Role's +1/+1 = 7/3") {
                game.state.projectedState.getPower(bear) shouldBe 7
                game.state.projectedState.getToughness(bear) shouldBe 3
            }
        }
    }
}
