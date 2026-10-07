package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import com.wingedsheep.sdk.core.Keyword

class GeistOfSaintThaliaScenarioTest : ScenarioTestBase() {
    init {
        test("reduces your noncreature spell's generic cost") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Geist of Saint Thalia")
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.state.projectedState.hasKeyword(game.findPermanent("Geist of Saint Thalia")!!, Keyword.FLYING) shouldBe true
            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()
            game.findCardsInHand(1, "Island").size shouldBe 2
        }
        test("does not discount creature spells") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Geist of Saint Thalia")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, "Grizzly Bears").error.shouldNotBeNull()
        }
        test("does not discount an opponent's spells") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Geist of Saint Thalia")
                .withCardInHand(2, "Divination")
                .withLandsOnBattlefield(2, "Island", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(2, "Divination").error.shouldNotBeNull()
        }
        // A prepare-spell copy has only the prepare spell's characteristics (CR 722.3c), so it is a
        // noncreature spell even though the card it copies is a creature.
        test("discounts a prepared creature's prepare spell") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Geist of Saint Thalia")
                .withCardInHand(1, "Studious First-Year")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, "Studious First-Year").error shouldBe null
            game.resolveStack()
            val firstYear = game.findPermanent("Studious First-Year")!!
            game.state.getEntity(firstYear)?.get<PreparedComponent>().shouldNotBeNull()

            val copyId = game.state.getExile(game.player1Id).first {
                game.state.getEntity(it)?.has<PreparedSpellCopyComponent>() == true
            }
            val offer = game.getLegalActions(1).first { (it.action as? CastSpell)?.cardId == copyId }
            offer.manaCostString shouldBe "{G}"
            offer.isAffordable shouldBe true

            // Rampant Growth costs {1}{G}; one Forest is left, so only the discount pays for it.
            game.execute(CastSpell(game.player1Id, copyId, faceIndex = 0)).error shouldBe null
            game.resolveStack()
            game.state.getEntity(firstYear)?.get<PreparedComponent>().shouldBeNull()
        }
        // An Adventure on the stack has only its alternative characteristics (CR 715.3b).
        test("discounts a creature card cast as its Adventure") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Geist of Saint Thalia")
                .withCardInHand(1, "Minecart Daredevil")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val geist = game.findPermanent("Geist of Saint Thalia")!!
            val daredevil = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Minecart Daredevil"
            }
            // Ride the Rails costs {1}{R}; a single Mountain pays it only with the discount.
            game.execute(
                CastSpell(game.player1Id, daredevil, listOf(ChosenTarget.Permanent(geist)), faceIndex = 0)
            ).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(geist) shouldBe 3
        }
        // CR 118.9d: cost reductions that affect a spell apply to an alternative cost it is cast for.
        test("discounts a flashback cost") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Geist of Saint Thalia")
                .withCardInGraveyard(1, "Think Twice")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val thinkTwice = game.findCardsInGraveyard(1, "Think Twice").single()
            val offer = game.legalActions(game.player1Id).first {
                (it.action as? CastSpell)?.let { a -> a.cardId == thinkTwice && a.alternativeCostType == AlternativeCostType.FLASHBACK } == true
            }
            offer.manaCostString shouldBe "{1}{U}"
            // Flashback {2}{U}; two Islands pay it only with the discount.
            game.execute(
                CastSpell(
                    game.player1Id, thinkTwice,
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.FLASHBACK,
                )
            ).error shouldBe null
            game.resolveStack()
            game.findCardsInHand(1, "Island").size shouldBe 1
        }
    }
}
