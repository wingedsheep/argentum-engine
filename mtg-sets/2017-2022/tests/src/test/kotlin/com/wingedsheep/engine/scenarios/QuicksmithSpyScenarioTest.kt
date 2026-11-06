package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Quicksmith Spy (AER #41).
 *
 * {3}{U} Creature — Human Artificer 2/3
 * "When this creature enters, target artifact you control gains '{T}: Draw a card' for as long as
 *  you control this creature."
 *
 * Pins the `Duration.WhileYouControlSource` activated-ability grant: the artifact taps to draw while
 * you hold the Spy, the grant ends when the Spy dies or changes control, and getting the Spy back
 * does not restore it (CR 611.2b).
 */
class QuicksmithSpyScenarioTest : ScenarioTestBase() {

    private fun setUp(): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Sol Ring")
            .withCardInHand(1, "Quicksmith Spy")
            .withCardInHand(1, "Switcheroo")
            .withCardInHand(1, "Switcheroo")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Island", 14)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
        repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
        val game = builder.build()

        game.castSpell(1, "Quicksmith Spy").error shouldBe null
        game.resolveStack()
        if (game.getPendingDecision() != null) {
            game.selectTargets(listOf(game.solRing())).error shouldBe null
        }
        game.resolveStack()
        return game
    }

    private fun TestGame.solRing(): EntityId = findPermanent("Sol Ring")!!

    private fun TestGame.spyGrant() =
        state.grantedActivatedAbilities.firstOrNull { it.entityId == solRing() }

    /** Cast Switcheroo exchanging the Spy and Grizzly Bears; returns the cast error, if any. */
    private fun TestGame.switcheroo(): String? {
        val card = findCardsInHand(1, "Switcheroo").first()
        val targets = listOf(
            ChosenTarget.Permanent(findPermanent("Quicksmith Spy")!!),
            ChosenTarget.Permanent(findPermanent("Grizzly Bears")!!)
        )
        val error = execute(CastSpell(player1Id, card, targets)).error
        resolveStack()
        return error
    }

    init {
        context("Quicksmith Spy") {

            test("the targeted artifact gains {T}: Draw a card") {
                val game = setUp()
                val grant = game.spyGrant()
                withClue("Sol Ring carries the granted ability") { grant shouldNotBe null }

                val handBefore = game.handSize(1)
                game.execute(ActivateAbility(game.player1Id, game.solRing(), grant!!.ability.id)).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe handBefore + 1
            }

            test("the grant ends when Quicksmith Spy leaves the battlefield") {
                val game = setUp()
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Quicksmith Spy")).error shouldBe null
                game.resolveStack()
                game.findPermanent("Quicksmith Spy") shouldBe null
                withClue("the draw ability went with the Spy") { game.spyGrant() shouldBe null }
            }

            test("losing control of the Spy ends the grant, and getting it back does not restore it") {
                val game = setUp()
                val grantId = game.spyGrant()!!.ability.id

                game.switcheroo() shouldBe null
                withClue("the opponent now controls the Spy") {
                    game.state.projectedState.getController(game.findPermanent("Quicksmith Spy")!!) shouldBe
                        game.player2Id
                }
                withClue("you no longer control the Spy, so Sol Ring lost the ability") {
                    game.spyGrant() shouldBe null
                    game.execute(ActivateAbility(game.player1Id, game.solRing(), grantId)).error shouldNotBe null
                }

                game.switcheroo() shouldBe null
                withClue("the Spy is yours again") {
                    game.state.projectedState.getController(game.findPermanent("Quicksmith Spy")!!) shouldBe
                        game.player1Id
                }
                withClue("a 'for as long as' effect does not restart (CR 611.2b)") { game.spyGrant() shouldBe null }
            }
        }
    }
}
