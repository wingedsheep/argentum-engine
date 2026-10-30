package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.SleeperDart
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.matchers.shouldBe

/**
 * Sleeper Dart (IKO #240) — {2} Artifact.
 *
 *   When this artifact enters, draw a card.
 *   {T}, Sacrifice this artifact: Target creature doesn't untap during its controller's next untap step.
 */
class SleeperDartScenarioTest : ScenarioTestBase() {

    init {
        test("entering draws a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Sleeper Dart")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.handSize(1) shouldBe 1
            game.castSpell(1, "Sleeper Dart").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Sleeper Dart") shouldBe true
            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 0
        }

        test("sacrificing it keeps a tapped creature tapped through its controller's next untap step only") {
            val d = GameTestDriver().also {
                it.registerCards(TestCards.all)
                it.initMirrorMatch(Deck.of("Plains" to 40))
                it.passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            val player = d.activePlayer!!
            val opponent = d.getOpponent(player)
            val dart = d.putPermanentOnBattlefield(player, "Sleeper Dart")
            val bears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
            d.tapPermanent(bears)

            d.submitSuccess(
                ActivateAbility(
                    playerId = player,
                    sourceId = dart,
                    abilityId = SleeperDart.script.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            )
            d.state.getBattlefield().contains(dart) shouldBe false
            d.bothPass()

            d.passPriorityUntil(Step.UPKEEP, opponent)
            d.isTapped(bears) shouldBe true

            d.passPriorityUntil(Step.UPKEEP, player)
            d.passPriorityUntil(Step.UPKEEP, opponent)
            d.isTapped(bears) shouldBe false
        }
    }
}
