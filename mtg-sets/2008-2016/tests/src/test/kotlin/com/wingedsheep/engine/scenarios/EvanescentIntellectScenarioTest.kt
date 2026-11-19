package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Evanescent Intellect (BNG #38) — "Enchanted creature has '{1}{U}, {T}: Target player mills
 * three cards.'"
 */
class EvanescentIntellectScenarioTest : ScenarioTestBase() {
    init {
        val grantedId = cardRegistry.getCard("Evanescent Intellect")!!.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id

        test("the enchanted creature makes the target player mill three") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Evanescent Intellect", "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.execute(
                ActivateAbility(game.player1Id, bears, grantedId, listOf(ChosenTarget.Player(game.player2Id)))
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.graveyardSize(2) shouldBe 3
            game.librarySize(2) shouldBe 1
        }
    }
}
