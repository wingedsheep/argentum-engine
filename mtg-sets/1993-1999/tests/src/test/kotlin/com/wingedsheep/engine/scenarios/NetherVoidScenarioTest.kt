package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nether Void — "Whenever a player casts a spell, counter it unless that player pays {3}."
 */
class NetherVoidScenarioTest : ScenarioTestBase() {
    init {
        test("a spell whose caster can't pay {3} is countered") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Nether Void")
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 4)
                .withActivePlayer(2).build()

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("the caster may pay {3} and the spell resolves") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Nether Void")
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 5)
                .withActivePlayer(2).build()

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe game.player2Id
            game.answerYesNo(true).error shouldBe null
            game.submitManaSourcesAutoPay().error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("declining the tax counters even its controller's own spell") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Nether Void")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1).build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe game.player1Id
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
    }
}
