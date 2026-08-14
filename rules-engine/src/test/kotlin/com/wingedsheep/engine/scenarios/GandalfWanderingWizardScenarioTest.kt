package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Gandalf, Wandering Wizard (HOB) — "{6}: Gandalf's owner shuffles him into their library and draws
 * three cards."
 *
 * The card that motivated `Player.OwnerOfSource`. The ability names its *own source's owner*, which
 * no existing player reference could express: `Player.You` is the ability's controller and
 * `Player.OwnerOf(desc)` reads the owner of the first chosen *target*, of which this ability has
 * none. The second test is the one that would fail under either substitute — it splits ownership
 * from control and checks that the owner, not the activating thief, refills.
 */
class GandalfWanderingWizardScenarioTest : ScenarioTestBase() {

    /** Activate Gandalf's only activated ability, letting the engine auto-pay the {6}. */
    private fun TestGame.activateGandalf(playerNumber: Int) = execute(
        ActivateAbility(
            playerId = if (playerNumber == 1) player1Id else player2Id,
            sourceId = findPermanent("Gandalf, Wandering Wizard")
                ?: error("Gandalf is not on the battlefield"),
            abilityId = cardRegistry.getCard("Gandalf, Wandering Wizard")!!
                .activatedAbilities[0].id,
            paymentStrategy = PaymentStrategy.AutoPay,
        )
    )

    init {
        test("the owner shuffles Gandalf into their library and draws three") {
            val game = scenario()
                .withPlayers("Owner", "Opponent")
                .withCardOnBattlefield(1, "Gandalf, Wandering Wizard")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)

            game.activateGandalf(1).error.shouldBeNull()
            game.resolveStack()

            game.findPermanent("Gandalf, Wandering Wizard").shouldBeNull()
            game.handSize(1) shouldBe handBefore + 3
            // Five Forests, plus Gandalf shuffled in, minus the three cards drawn.
            game.librarySize(1) shouldBe 3
        }

        test("a thief may pay the {6}, but it is the owner who shuffles and draws") {
            val game = scenario()
                .withPlayers("Owner", "Thief")
                .withCardOnBattlefield(1, "Gandalf, Wandering Wizard")
                .withLandsOnBattlefield(2, "Island", 6)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Simulate a Mind Control effect: control moves to player 2, ownership stays with 1.
            val stolen = game.findPermanent("Gandalf, Wandering Wizard")
                ?: error("Gandalf is not on the battlefield")
            game.state = game.state.updateEntity(stolen) { container ->
                container.with(ControllerComponent(game.player2Id))
            }

            val ownerHandBefore = game.handSize(1)
            val thiefHandBefore = game.handSize(2)

            game.activateGandalf(2).error.shouldBeNull()
            game.resolveStack()

            game.findPermanent("Gandalf, Wandering Wizard").shouldBeNull()
            game.handSize(1) shouldBe ownerHandBefore + 3
            game.librarySize(1) shouldBe 3

            // The thief paid, but drew nothing and kept their own library intact.
            game.handSize(2) shouldBe thiefHandBefore
            game.librarySize(2) shouldBe 5
        }
    }
}
