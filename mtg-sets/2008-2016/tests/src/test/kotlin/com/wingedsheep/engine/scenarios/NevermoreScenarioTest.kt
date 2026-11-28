package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nevermore ({1}{W}{W}, Enchantment):
 *   As this enchantment enters, choose a nonland card name.
 *   Spells with the chosen name can't be cast.
 *
 * Covers the composition EntersWithChoice(CARD_NAME, NONLAND) + PlayersCantCastSpells(Player.Each)
 * keyed on the permanent's durable chosen name: only the named card is locked, for every player
 * (Nevermore's controller included), and the lock ends when Nevermore leaves.
 */
class NevermoreScenarioTest : ScenarioTestBase() {

    private fun TestGame.castNevermoreNaming(name: String) {
        castSpell(1, "Nevermore")
        resolveStack()
        val decision = getPendingDecision()
        withClue("Nevermore must present an as-enters card-name choice") {
            (decision is ChooseOptionDecision) shouldBe true
        }
        decision as ChooseOptionDecision
        withClue("The pool is nonland names only") {
            decision.options shouldNotContain "Plains"
            decision.options shouldContain name
        }
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(name)))
    }

    private fun TestGame.castableCards(playerNumber: Int): Set<EntityId> =
        getLegalActions(playerNumber).mapNotNull { (it.action as? CastSpell)?.cardId }.toSet()

    init {
        test("the named spell can't be cast by its controller; other spells still can") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Nevermore")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Llanowar Elves")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Forest", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInHand(1, "Grizzly Bears").first()
            val elves = game.findCardsInHand(1, "Llanowar Elves").first()
            game.castableCards(1) shouldContain bears

            game.castNevermoreNaming("Grizzly Bears")

            val nevermore = game.findPermanent("Nevermore")!!
            game.state.getEntity(nevermore)?.chosenCardName() shouldBe "Grizzly Bears"

            withClue("Grizzly Bears is no longer offered as a cast") {
                game.castableCards(1) shouldNotContain bears
            }
            withClue("An unnamed spell is unaffected") {
                game.castableCards(1) shouldContain elves
            }
            withClue("A direct cast attempt is rejected") {
                game.castSpell(1, "Grizzly Bears").error shouldNotBe null
            }
        }

        test("the opponent can't cast the named spell either") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Nevermore")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInHand(2, "Lightning Bolt")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castNevermoreNaming("Lightning Bolt")
            game.passPriority()

            val bolt = game.findCardsInHand(2, "Lightning Bolt").first()
            val shock = game.findCardsInHand(2, "Shock").first()
            val castable = game.castableCards(2)
            withClue("The opponent can't cast the named instant") {
                castable shouldNotContain bolt
            }
            withClue("…but can still cast a differently named instant") {
                castable shouldContain shock
            }
        }
    }
}
