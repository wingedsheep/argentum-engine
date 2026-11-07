package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Merchant's Dockhand (AER #163):
 * "{3}{U}, {T}, Tap X untapped artifacts you control: Look at the top X cards of your library.
 * Put one of them into your hand and the rest on the bottom of your library in any order."
 *
 * Pins the two things the script alone can't show: the Dockhand pays its own {T}, so it is never
 * one of the X artifacts (ruling 2017-02-09), and the X chosen by the tap cost is the number of
 * cards looked at.
 */
class MerchantsDockhandScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Merchant's Dockhand", summoningSickness = false)
        .withCardOnBattlefield(1, "Ornithopter")
        .withCardOnBattlefield(1, "Ornithopter")
        .withCardOnBattlefield(1, "Ornithopter")
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Raging Goblin")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activateDockhand() {
        val dockhand = findPermanent("Merchant's Dockhand")!!
        val activate = getLegalActions(1)
            .mapNotNull { it.action as? ActivateAbility }
            .single { it.sourceId == dockhand }
        execute(activate).error shouldBe null
    }

    init {
        context("Merchant's Dockhand") {

            test("taps X other artifacts, looks at X cards, keeps one and bottoms the rest") {
                val game = setup()
                val dockhand = game.findPermanent("Merchant's Dockhand")!!
                val ornithopters = game.findPermanents("Ornithopter")
                val library = game.state.getZone(game.player1Id, Zone.LIBRARY)
                val topThree = library.take(3)
                val handBefore = game.handSize(1)

                game.activateDockhand()

                val xDecision = game.getPendingDecision().shouldNotBeNull()
                xDecision.shouldBeInstanceOf<ChooseNumberDecision>()
                withClue("X counts only the other untapped artifacts — the Dockhand pays {T}") {
                    xDecision.maxValue shouldBe 3
                }
                game.chooseNumber(3).error shouldBe null

                val tapDecision = game.getPendingDecision().shouldNotBeNull()
                tapDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the Dockhand can't be one of the X artifacts it taps") {
                    tapDecision.options shouldNotContain dockhand
                }
                game.selectCards(ornithopters).error shouldBe null

                (ornithopters + dockhand).forEach { id ->
                    game.state.getEntity(id)!!.has<TappedComponent>() shouldBe true
                }

                game.resolveStack()

                val keep = game.getPendingDecision().shouldNotBeNull()
                keep.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("looks at exactly the top X = 3 cards") {
                    keep.options shouldContainExactlyInAnyOrder topThree
                }
                game.selectCards(listOf(topThree[1])).error shouldBe null

                if (game.getPendingDecision() is ReorderLibraryDecision) {
                    game.keepLibraryOrder().error shouldBe null
                }
                game.getPendingDecision().shouldBeNull()

                game.handSize(1) shouldBe handBefore + 1
                (topThree[1] in game.state.getZone(game.player1Id, Zone.HAND)) shouldBe true
                val after = game.state.getZone(game.player1Id, Zone.LIBRARY)
                withClue("the untouched cards stay on top; the other two looked-at cards go to the bottom") {
                    after.take(2) shouldBe library.drop(3)
                    after.takeLast(2) shouldContainExactlyInAnyOrder listOf(topThree[0], topThree[2])
                }
            }

            test("the legal action offers X and tap targets without the Dockhand itself") {
                val game = setup()
                val dockhand = game.findPermanent("Merchant's Dockhand")!!
                val ornithopters = game.findPermanents("Ornithopter")
                val legal = game.getLegalActions(1)
                    .single { (it.action as? ActivateAbility)?.sourceId == dockhand }

                withClue("max X is the three other untapped artifacts, not four") {
                    legal.maxAffordableX shouldBe 3
                }
                val costInfo = legal.additionalCostInfo.shouldNotBeNull()
                costInfo.validTapTargets shouldContainExactlyInAnyOrder ornithopters
                costInfo.description shouldBe "Tap X untapped other artifacts you control"
            }

            test("X = 0 looks at and moves no cards") {
                val game = setup()
                val dockhand = game.findPermanent("Merchant's Dockhand")!!
                val ornithopters = game.findPermanents("Ornithopter")
                val library = game.state.getZone(game.player1Id, Zone.LIBRARY)
                val handBefore = game.handSize(1)

                game.activateDockhand()
                game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                game.chooseNumber(0).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeNull()
                game.handSize(1) shouldBe handBefore
                game.state.getZone(game.player1Id, Zone.LIBRARY) shouldBe library
                game.state.getEntity(dockhand)!!.has<TappedComponent>() shouldBe true
                ornithopters.forEach { id ->
                    game.state.getEntity(id)!!.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
