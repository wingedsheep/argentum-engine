package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scrabbling Claws (MRD) — "{T}: Target player exiles a card from their graveyard.
 * {1}, Sacrifice this artifact: Exile target card from a graveyard. Draw a card."
 *
 * The card's whole point is that its two abilities put the choice in *different hands*, so that is
 * what these tests pin:
 *
 *  - The tap ability targets a player, and the decision to pick which card gets exiled must land on
 *    that **targeted player**, not on the Claws' controller — including when the target is the
 *    controller themselves.
 *  - The sacrifice ability targets a card directly, so the **controller** picks, no decision is
 *    raised, and the draw follows the exile.
 *
 * Plus the two boundary cases: an empty graveyard makes the tap ability a legal but inert
 * activation, and the sacrificed Claws end up in their own controller's graveyard rather than
 * being exiled by their own ability.
 */
class ScrabblingClawsScenarioTest : ScenarioTestBase() {

    /** Activate the ability at [index] of Scrabbling Claws, letting the engine auto-pay. */
    private fun TestGame.activateClaws(
        index: Int,
        targets: List<ChosenTarget> = emptyList(),
    ) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Scrabbling Claws") ?: error("Claws are not on the battlefield"),
            abilityId = cardRegistry.getCard("Scrabbling Claws")!!.activatedAbilities[index].id,
            targets = targets,
            paymentStrategy = PaymentStrategy.AutoPay,
        )
    )

    private fun TestGame.playerId(playerNumber: Int): EntityId =
        if (playerNumber == 1) player1Id else player2Id

    init {
        test("the tap ability lets the targeted opponent choose which of their cards is exiled") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Scrabbling Claws")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInGraveyard(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateClaws(0, listOf(ChosenTarget.Player(game.playerId(2)))).error shouldBe null
            game.resolveStack()

            // The opponent — not the Claws' controller — is asked, and picks from their own cards.
            val decision = game.getPendingDecision().shouldNotBeNull()
            decision.playerId shouldBe game.playerId(2)

            val courser = game.findCardsInGraveyard(2, "Centaur Courser").single()
            game.selectCards(listOf(courser))

            game.isInExile(2, "Centaur Courser") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.graveyardSize(2) shouldBe 1
        }

        test("the tap ability can target its own controller, who then chooses") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Scrabbling Claws")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateClaws(0, listOf(ChosenTarget.Player(game.playerId(1)))).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldNotBeNull()
            decision.playerId shouldBe game.playerId(1)

            game.selectCards(listOf(game.findCardsInGraveyard(1, "Grizzly Bears").single()))
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Centaur Courser") shouldBe true
        }

        test("a sole card in the graveyard is exiled without prompting — the choice is forced") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Scrabbling Claws")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateClaws(0, listOf(ChosenTarget.Player(game.playerId(2)))).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.graveyardSize(2) shouldBe 0
        }

        test("targeting a player with an empty graveyard resolves as a no-op") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Scrabbling Claws")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateClaws(0, listOf(ChosenTarget.Player(game.playerId(2)))).error shouldBe null
            game.resolveStack()

            // Nothing to gather, so nobody is asked anything.
            game.hasPendingDecision() shouldBe false
            game.graveyardSize(2) shouldBe 0
        }

        test("the sacrifice ability exiles the controller's chosen card and draws") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Scrabbling Claws")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findCardsInGraveyard(2, "Centaur Courser").single()
            val handBefore = game.handSize(1)

            game.activateClaws(
                1,
                listOf(ChosenTarget.Card(courser, game.playerId(2), Zone.GRAVEYARD))
            ).error shouldBe null
            game.resolveStack()

            // The controller targeted the card directly — no selection decision is raised.
            game.hasPendingDecision() shouldBe false
            game.isInExile(2, "Centaur Courser") shouldBe true
            game.handSize(1) shouldBe handBefore + 1
        }

        test("the sacrificed Claws land in their controller's graveyard, not in exile") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Scrabbling Claws")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findCardsInGraveyard(2, "Centaur Courser").single()
            game.activateClaws(
                1,
                listOf(ChosenTarget.Card(courser, game.playerId(2), Zone.GRAVEYARD))
            ).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Scrabbling Claws") shouldBe false
            game.isInGraveyard(1, "Scrabbling Claws") shouldBe true
        }
    }
}
