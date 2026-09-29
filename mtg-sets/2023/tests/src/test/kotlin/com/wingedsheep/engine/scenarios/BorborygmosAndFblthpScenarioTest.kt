package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.BorborygmosAndFblthp
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Borborygmos and Fblthp (MOM #219) — enters or attacks: draw a card, then discard any number of
 * land cards; when you discard one or more this way, it deals twice that much damage to target
 * creature. {1}{U}: put it into its owner's library third from the top.
 */
class BorborygmosAndFblthpScenarioTest : ScenarioTestBase() {

    init {
        fun castGame(): TestGame {
            var b = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Borborygmos and Fblthp")
                .withCardInHand(1, "Forest")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(3) { b = b.withCardInLibrary(1, "Island") }
            repeat(3) { b = b.withCardInLibrary(2, "Island") }
            val game = b.build()
            game.castSpell(1, "Borborygmos and Fblthp").error shouldBe null
            game.resolveStack()
            return game
        }

        context("Borborygmos and Fblthp") {
            test("enters: draws, offers only land cards, and deals twice the discarded count") {
                val game = castGame()

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val forest = game.findCardsInHand(1, "Forest").single()
                val plains = game.findCardsInHand(1, "Plains").single()
                withClue("the drawn Plains and the Forest are offered; Grizzly Bears is not") {
                    decision.options shouldContainExactlyInAnyOrder listOf(forest, plains)
                }
                game.selectCards(listOf(forest, plains)).error shouldBe null

                val giant = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Forest") shouldBe true
                game.isInGraveyard(1, "Plains") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe true
                withClue("two lands discarded -> 4 damage kills the 3/3") {
                    game.findPermanent("Hill Giant") shouldBe null
                }
            }

            test("one land discarded deals 2 damage") {
                val game = castGame()
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val forest = game.findCardsInHand(1, "Forest").single()
                game.selectCards(listOf(forest)).error shouldBe null

                val giant = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("2 damage doesn't kill the 3/3") {
                    game.findPermanent("Hill Giant") shouldBe giant
                }
                game.isInHand(1, "Plains") shouldBe true
            }

            test("discarding nothing creates no reflexive trigger") {
                val game = castGame()
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.handSize(1) shouldBe 3
                (game.findPermanent("Hill Giant") != null) shouldBe true
            }

            test("{1}{U} puts it into its owner's library third from the top") {
                var b = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Borborygmos and Fblthp")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(4) { b = b.withCardInLibrary(1, "Forest") }
                repeat(3) { b = b.withCardInLibrary(2, "Island") }
                val game = b.build()

                val borborygmos = game.findPermanent("Borborygmos and Fblthp")!!
                val abilityId = BorborygmosAndFblthp.activatedAbilities.single().id
                game.execute(ActivateAbility(game.player1Id, borborygmos, abilityId)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Borborygmos and Fblthp") shouldBe false
                val library = game.state.getLibrary(game.player1Id)
                val inLibrary = game.findCardsInLibrary(1, "Borborygmos and Fblthp").single()
                library.indexOf(inLibrary) shouldBe 2
            }
        }
    }
}
