package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Aetherspouts (M15 #44) — {3}{U}{U} Instant.
 *
 *   For each attacking creature, its owner puts it on their choice of the top or bottom of
 *   their library.
 *
 * Rulings: the owner of each attacking creature chooses, the active player first; two or more
 * cards going to the top (or bottom) together are arranged in any order by their owner.
 */
class AetherspoutsScenarioTest : ScenarioTestBase() {

    init {
        test("the owner splits their attackers between top and bottom and orders each pile") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Craw Wurm") // stays home
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Plains")
                .withCardInHand(2, "Aetherspouts")
                .withLandsOnBattlefield(2, "Island", 5)
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2, "Hill Giant" to 2))
                .error shouldBe null
            game.execute(PassPriority(game.player1Id))
            game.castSpell(2, "Aetherspouts").error shouldBe null
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            val lions = game.findPermanent("Savannah Lions")!!
            val giant = game.findPermanent("Hill Giant")!!

            val split = game.getPendingDecision()
            split.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the attacking creatures' owner makes the choice, over exactly their attackers") {
                split.playerId shouldBe game.player1Id
                split.options shouldContainExactlyInAnyOrder listOf(bears, lions, giant)
            }
            game.selectCards(listOf(bears, lions)).error shouldBe null

            val order = game.getPendingDecision()
            order.shouldBeInstanceOf<ReorderLibraryDecision>()
            withClue("the owner arranges the two cards going on top") {
                order.playerId shouldBe game.player1Id
                order.cards shouldContainExactlyInAnyOrder listOf(bears, lions)
                order.prompt shouldBe "Put these permanents on the top of your library in any order."
            }
            game.submitDecision(OrderedResponse(order.id, listOf(lions, bears))).error shouldBe null
            game.resolveStack()

            val library = game.state.getLibrary(game.player1Id)
            val names = library.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
            withClue("Lions then Bears on top in the chosen order, Hill Giant on the bottom") {
                names.take(2) shouldBe listOf("Savannah Lions", "Grizzly Bears")
                names.last() shouldBe "Hill Giant"
                library.size shouldBe 5
            }
            game.isOnBattlefield("Craw Wurm") shouldBe true
            game.isOnBattlefield("Centaur Courser") shouldBe true
        }

        test("a stolen attacker goes into its owner's library, the owner choosing after the active player") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardInHand(1, "Act of Treason")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Aetherspouts")
                .withLandsOnBattlefield(2, "Island", 5)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Act of Treason", giant).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            game.execute(PassPriority(game.player1Id))
            game.castSpell(2, "Aetherspouts").error shouldBe null
            game.resolveStack()

            val first = game.getPendingDecision()
            first.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the active player chooses first, and only for the attacker they own") {
                first.playerId shouldBe game.player1Id
                first.options shouldBe listOf(bears)
            }
            game.selectCards(emptyList()).error shouldBe null

            val second = game.getPendingDecision()
            second.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the stolen Hill Giant's owner decides where it goes") {
                second.playerId shouldBe game.player2Id
                second.options shouldBe listOf(giant)
            }
            game.selectCards(listOf(giant)).error shouldBe null
            withClue("a lone permanent going on top has no order to choose and nothing hidden to look at") {
                game.getPendingDecision() shouldBe null
            }
            game.resolveStack()

            withClue("each creature went into its owner's library") {
                game.state.getLibrary(game.player1Id).last() shouldBe bears
                game.state.getLibrary(game.player2Id).first() shouldBe giant
            }
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isOnBattlefield("Hill Giant") shouldBe false
        }
    }
}
