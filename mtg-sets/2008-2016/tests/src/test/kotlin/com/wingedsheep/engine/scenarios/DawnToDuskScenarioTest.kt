package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dawn to Dusk (BNG #6) — "Choose one or both — • Return target enchantment card from your
 * graveyard to your hand. • Destroy target enchantment."
 */
class DawnToDuskScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Dawn to Dusk")
        .withCardInGraveyard(1, "Glorious Anthem")
        .withCardOnBattlefield(2, "Crusade")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("choosing both modes returns an enchantment card and destroys an enchantment") {
            val game = setup()
            val anthem = game.findCardsInGraveyard(1, "Glorious Anthem").single()
            val crusade = game.findPermanent("Crusade")!!
            val returnTarget = ChosenTarget.Card(anthem, game.player1Id, Zone.GRAVEYARD)
            val destroyTarget = ChosenTarget.Permanent(crusade)

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Dawn to Dusk").single(),
                    targets = listOf(returnTarget, destroyTarget),
                    chosenModes = listOf(0, 1),
                    modeTargetsOrdered = listOf(listOf(returnTarget), listOf(destroyTarget)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("Glorious Anthem returned to hand") { game.isInHand(1, "Glorious Anthem") shouldBe true }
            withClue("Crusade was destroyed") { game.isInGraveyard(2, "Crusade") shouldBe true }
        }

        test("choosing only the destroy mode leaves the graveyard alone") {
            val game = setup()
            val crusade = game.findPermanent("Crusade")!!

            game.castSpellWithMode(1, "Dawn to Dusk", 1, crusade).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Crusade") shouldBe true
            game.isInGraveyard(1, "Glorious Anthem") shouldBe true
        }
    }
}
