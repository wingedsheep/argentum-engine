package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kogla and Yidaro (MOM #244) — ETB modal (trample+haste / fight a creature you don't control)
 * and a from-hand discard ability that destroys up to one artifact or enchantment, shuffles the
 * card into its owner's library from the graveyard, then draws.
 */
class KoglaAndYidaroScenarioTest : ScenarioTestBase() {

    private fun discardAbilityId() = cardRegistry.getCard("Kogla and Yidaro")!!
        .activatedAbilities.first { it.activateFromZone == Zone.HAND }.id

    private fun castAndChooseMode(game: TestGame, mode: Int) {
        game.castSpell(1, "Kogla and Yidaro").error shouldBe null
        game.resolveStack()
        val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
            ?: error("expected a ChooseOptionDecision; got ${game.state.pendingDecision}")
        game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = mode))
    }

    private fun castBoard() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Kogla and Yidaro")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun channelBoard() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Kogla and Yidaro")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardOnBattlefield(2, "Ornithopter")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("trample and haste mode grants both until end of turn") {
            val game = castBoard()
            castAndChooseMode(game, 0)
            game.resolveStack()

            val kogla = game.findPermanent("Kogla and Yidaro")!!
            game.state.projectedState.hasKeyword(kogla, Keyword.TRAMPLE) shouldBe true
            game.state.projectedState.hasKeyword(kogla, Keyword.HASTE) shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("fight mode kills a creature you don't control") {
            val game = castBoard()
            val bears = game.findPermanent("Grizzly Bears")!!
            castAndChooseMode(game, 1)
            val decision = game.state.pendingDecision as? ChooseTargetsDecision
            if (decision != null) {
                game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(bears))))
            }
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isOnBattlefield("Kogla and Yidaro") shouldBe true
        }

        test("discard ability destroys an artifact, shuffles itself into the library and draws") {
            val game = channelBoard()
            val thopter = game.findPermanent("Ornithopter")!!
            val handCard = game.findCardsInHand(1, "Kogla and Yidaro").first()

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = handCard,
                    abilityId = discardAbilityId(),
                    targets = listOf(ChosenTarget.Permanent(thopter))
                )
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.isInGraveyard(1, "Kogla and Yidaro") shouldBe true
            game.resolveStack()

            game.isOnBattlefield("Ornithopter") shouldBe false
            withClue("Kogla left the graveyard for the library") {
                game.isInGraveyard(1, "Kogla and Yidaro") shouldBe false
            }
            withClue("library: 2 Forests + Kogla − 1 drawn") { game.librarySize(1) shouldBe 2 }
            game.handSize(1) shouldBe 1
        }

        test("discard ability with no target still shuffles and draws") {
            val game = channelBoard()
            val handCard = game.findCardsInHand(1, "Kogla and Yidaro").first()

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = handCard,
                    abilityId = discardAbilityId(),
                    targets = emptyList()
                )
            )
            withClue("activation with no target should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Ornithopter") shouldBe true
            game.isInGraveyard(1, "Kogla and Yidaro") shouldBe false
            game.librarySize(1) shouldBe 2
            game.handSize(1) shouldBe 1
        }
    }
}
