package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Beamsplitter Mage — "Whenever you cast an instant or sorcery spell that targets only this
 * creature, if you control one or more other creatures that spell could target, choose one of those
 * creatures. Copy that spell. The copy targets the chosen creature."
 */
class BeamsplitterMageScenarioTest : ScenarioTestBase() {

    init {
        test("with several other creatures, you choose one and a single copy targets it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Beamsplitter Mage")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mage = game.findPermanent("Beamsplitter Mage")!!
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanents("Grizzly Bears")
            val myBears = bears.single { game.state.projectedState.getController(it) == game.player1Id }
            val theirBears = bears.single { it != myBears }

            game.castSpell(1, "Giant Growth", mage).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.options shouldContainExactlyInAnyOrder listOf(myBears, giant)
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(mage) shouldBe 5
            projected.getPower(giant) shouldBe 6
            projected.getPower(myBears) shouldBe 2
            projected.getPower(theirBears) shouldBe 2
        }

        test("with one other creature, the copy targets it without a choice") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Beamsplitter Mage")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mage = game.findPermanent("Beamsplitter Mage")!!
            game.castSpell(1, "Giant Growth", mage).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.state.projectedState.getPower(mage) shouldBe 5
            game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 5
        }

        test("with no other creature it could target, the ability doesn't trigger at all") {
            // Intervening "if" (CR 603.4): checked as the spell is cast, so no trigger is put on the
            // stack — a creature arriving before resolution can't then receive a copy.
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Beamsplitter Mage")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mage = game.findPermanent("Beamsplitter Mage")!!
            game.castSpell(1, "Giant Growth", mage).error shouldBe null
            withClue("only Giant Growth is on the stack") { game.state.stack.size shouldBe 1 }
            game.resolveStack()

            game.state.projectedState.getPower(mage) shouldBe 5
            game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
        }

        test("an opponent's spell targeting the Mage doesn't trigger it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Beamsplitter Mage")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Giant Growth")
                .withLandsOnBattlefield(2, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(2)
                .build()

            val mage = game.findPermanent("Beamsplitter Mage")!!
            game.castSpell(2, "Giant Growth", mage).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(mage) shouldBe 5
            game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
        }

        test("a spell targeting another creature doesn't trigger it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Beamsplitter Mage")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 5
            game.state.projectedState.getPower(game.findPermanent("Beamsplitter Mage")!!) shouldBe 2
        }
    }
}
