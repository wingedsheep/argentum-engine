package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Proves the built-in AI actually chooses a value for `{X}` when it casts an X-cost *spell*.
 *
 * There is no engine-side choose-X pause on the cast path the way there is for an activated
 * ability: a `CastSpell` submitted with no `xValue` is bound to X=0 as it goes on the stack
 * (CR 601.2b), so before [XCostSelection] every X spell in a deck was dead weight. This test pins
 * the fix end to end, through real legal-action enumeration; [XCostSelectionTest] covers which X
 * values are offered and why.
 */
class XCostSpellAiTest : ScenarioTestBase() {

    // The AI runs its own simulator over the same pool the scenario was built from.
    private fun aiFor(playerId: EntityId) = AIPlayer.create(cardRegistry, playerId)

    init {
        /**
         * Goldvein Hydra ({X}{G}, "enters with X +1/+1 counters") is the card for the free-X case
         * because the X choice shows up directly in the thing the evaluator measures — at X=0 it is
         * a 0/0 that dies immediately, and each extra X is a bigger body. That keeps the test about
         * whether an X was chosen at all, rather than about how the evaluator prices a subtler
         * payoff.
         */
        context("X is free of the targets") {

            fun gameWithHydra(forests: Int) = scenario()
                .withPlayers()
                .withCardInHand(1, "Goldvein Hydra")
                .withLandsOnBattlefield(1, "Forest", forests)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("AI casts an X-cost creature spell for a real X, not the default of 0") {
                val game = gameWithHydra(forests = 7)

                val cast = aiFor(game.player1Id).chooseAction(game.state).shouldBeInstanceOf<CastSpell>()

                game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name shouldBe "Goldvein Hydra"
                cast.xValue.shouldNotBeNull()
                cast.xValue!! shouldBeGreaterThanOrEqual 1
            }

            test("the chosen X never exceeds what the AI can pay") {
                // Two Forests pay {X}{G} at X=1 and no more.
                val game = gameWithHydra(forests = 2)

                val cast = aiFor(game.player1Id).chooseAction(game.state).shouldBeInstanceOf<CastSpell>()

                cast.xValue shouldBe 1
            }
        }

        /**
         * Repeal ({X}{U}, "return target nonland permanent with mana value X to its owner's hand")
         * is the target-gated case, and the one that proves the *wiring* rather than the arithmetic:
         * its filter is `ManaValueEqualsX`, so an X that misses the target's mana value is rejected
         * outright, and the only thing that tells [XCostSelection] so is
         * `LegalAction.xConstrainsTargetManaValueExactly`. Without the flag the action looks like a
         * free X, the sweep offers the largest affordable value, and the cast is illegal.
         *
         * Driven off the real enumerated action rather than through `chooseAction`, because whether
         * the AI *wants* to cast Repeal in this window is the hold policy's business — it is an
         * instant, and holding it for the opponent's turn is the right play. That would make this a
         * test of the evaluator wearing a wiring test's name.
         */
        context("X gates which targets are legal") {

            fun repealAgainst(target: String): Pair<GameState, LegalAction> {
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all)
                driver.initMirrorMatch(deck = Deck.of("Island" to 20), startingLife = 20)
                driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
                val player = driver.activePlayer!!

                driver.putCreatureOnBattlefield(player, target)
                driver.putCardInHand(player, "Repeal")
                driver.giveMana(player, Color.BLUE, 1)
                driver.giveColorlessMana(player, 7)

                val cast = driver.legalActions(player).first { it.description == "Cast Repeal" }
                cast.hasXCost shouldBe true
                return driver.state to cast
            }

            test("X is the target's mana value, not the largest the AI could pay") {
                // Hill Giant is mana value 4, and 7 colorless would pay for an X of 7.
                val (state, action) = repealAgainst("Hill Giant")

                val expanded = XCostSelection.expandToX(state, action)

                expanded shouldHaveSize 1
                (expanded.single().action as CastSpell).xValue shouldBe 4
                // The narrowing leaves exactly the permanent that X reaches.
                expanded.single().validTargets.shouldNotBeNull() shouldHaveSize 1
            }

            test("the chosen X follows the board, not a constant") {
                // Forest Walker is {1}{G} — mana value 2.
                val (state, action) = repealAgainst("Forest Walker")

                val expanded = XCostSelection.expandToX(state, action)

                (expanded.single().action as CastSpell).xValue shouldBe 2
            }
        }
    }
}
