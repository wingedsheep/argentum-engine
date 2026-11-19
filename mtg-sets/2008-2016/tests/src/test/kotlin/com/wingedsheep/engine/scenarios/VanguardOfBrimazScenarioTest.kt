package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Vanguard of Brimaz: "Heroic — Whenever you cast a spell that targets this creature, create a
 * 1/1 white Cat Soldier creature token with vigilance."
 */
class VanguardOfBrimazScenarioTest : ScenarioTestBase() {

    init {
        test("targeting it with a spell creates a 1/1 Cat Soldier with vigilance") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Vanguard of Brimaz")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val vanguard = game.findPermanent("Vanguard of Brimaz")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Giant Growth").single(),
                    targets = listOf(ChosenTarget.Permanent(vanguard)),
                )
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val tokens = game.state.getBattlefield().filter { game.state.getEntity(it)?.has<TokenComponent>() == true }
            tokens.size shouldBe 1
            projected.getPower(tokens.single()) shouldBe 1
            projected.getToughness(tokens.single()) shouldBe 1
            projected.hasKeyword(tokens.single(), Keyword.VIGILANCE) shouldBe true
            projected.getPower(vanguard) shouldBe 5
        }
    }
}
