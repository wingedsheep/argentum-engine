package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Break Out (MKM) — {R}{G} sorcery, look at the top six, may reveal a creature card, put it onto
 * the battlefield with haste if its mana value is 2 or less, otherwise into your hand, rest on the
 * bottom in a random order.
 *
 * Break Out composes entirely from existing pipeline steps, but it stacks two independent "may"s
 * over one card and routes three different outcomes, so these tests pin the partition rather than
 * the primitives: cheap-and-accepted goes to the battlefield hasted, cheap-and-declined and
 * too-expensive both go to hand, and a declined reveal leaves all six on the bottom.
 *
 * Each scenario seeds exactly six library cards with a single creature among five Forests, so the
 * `filter = Creature` selection offers exactly one candidate no matter how the library is ordered.
 */
class BreakOutScenarioTest : ScenarioTestBase() {

    private fun board(creature: String): ScenarioBuilder {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Break Out")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withLandsOnBattlefield(1, "Forest", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withCardInLibrary(1, creature)
        repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
        return builder
    }

    private fun handNames(game: TestGame): List<String> =
        game.state.getHand(game.player1Id)
            .mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }

    init {
        test("a revealed mana-value-2 creature can be put onto the battlefield with haste") {
            val game = board("Grizzly Bears").build()
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()

            game.castSpell(1, "Break Out").error shouldBe null
            game.resolveStack()

            withClue("first decision: which creature card to reveal") {
                game.hasPendingDecision() shouldBe true
                game.selectCards(listOf(bears)).error shouldBe null
            }
            withClue("second decision: whether to put the mana-value-2 creature onto the battlefield") {
                game.hasPendingDecision() shouldBe true
                game.selectCards(listOf(bears)).error shouldBe null
            }
            game.resolveStack()

            val onBattlefield = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            withClue("it gains haste until end of turn") {
                game.state.projectedState.hasKeyword(onBattlefield, "HASTE") shouldBe true
            }
            withClue("it went to the battlefield, not to hand") {
                handNames(game) shouldContainExactly emptyList()
            }
            withClue("the other five go to the bottom of the library") {
                game.librarySize(1) shouldBe 5
            }
        }

        test("a revealed creature costing more than 2 goes to hand instead") {
            val game = board("Hill Giant").build()
            val giant = game.findCardsInLibrary(1, "Hill Giant").single()

            game.castSpell(1, "Break Out").error shouldBe null
            game.resolveStack()

            withClue("the reveal is offered; mana value 4 is not") {
                game.hasPendingDecision() shouldBe true
                game.selectCards(listOf(giant)).error shouldBe null
            }
            game.resolveStack()

            withClue("no battlefield option for a mana value above 2") {
                game.findPermanent("Hill Giant") shouldBe null
            }
            handNames(game) shouldContainExactly listOf("Hill Giant")
            game.librarySize(1) shouldBe 5
        }

        test("declining the battlefield option still puts the creature into your hand") {
            val game = board("Grizzly Bears").build()
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()

            game.castSpell(1, "Break Out").error shouldBe null
            game.resolveStack()

            game.selectCards(listOf(bears)).error shouldBe null
            withClue("decline the second may") {
                game.skipSelection().error shouldBe null
            }
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            handNames(game) shouldContainExactly listOf("Grizzly Bears")
            game.librarySize(1) shouldBe 5
        }

        test("declining the reveal sends all six to the bottom") {
            val game = board("Grizzly Bears").build()

            game.castSpell(1, "Break Out").error shouldBe null
            game.resolveStack()

            withClue("decline the reveal") {
                game.skipSelection().error shouldBe null
            }
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            handNames(game) shouldContainExactly emptyList()
            withClue("nothing was kept — all six went back") {
                game.librarySize(1) shouldBe 6
            }
        }
    }
}
