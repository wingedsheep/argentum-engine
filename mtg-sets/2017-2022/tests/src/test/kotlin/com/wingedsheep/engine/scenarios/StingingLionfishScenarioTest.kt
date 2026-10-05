package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Stinging Lionfish ({1}{U}, 2/1 Enchantment Creature — Fish).
 *
 * Whenever you cast your first spell during each opponent's turn, you may tap or untap target
 * nonland permanent.
 *
 * Covers the `Triggers.you.castsNth(1)` trigger gated by `Conditions.IsOpponentsTurn`: it fires on
 * the first spell cast on an opponent's turn (both directions of the resolution-time choice), not on
 * the second, and not on your own turn; lands are not legal targets.
 */
class StingingLionfishScenarioTest : ScenarioTestBase() {

    // A {0} instant standing in for "casting a spell".
    private val flashProbe = card("Flash Probe") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "You gain 1 life."
        spell {
            effect = Effects.GainLife(1)
        }
    }

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.get<TappedComponent>() != null

    private fun TestGame.chooseMode(prefix: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.startsWith(prefix) }
        check(index >= 0) { "$prefix not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, optionIndex = index)).error shouldBe null
    }

    init {
        cardRegistry.register(flashProbe)

        context("Stinging Lionfish") {

            test("first spell on an opponent's turn taps target nonland permanent; lands can't be targeted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stinging Lionfish")
                    .withCardsInHand(1, "Flash Probe", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val forest = game.findPermanent("Forest")!!

                game.castSpell(1, "Flash Probe").error shouldBe null

                val targets = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                val legal = targets.legalTargets[0].orEmpty()
                withClue("a nonland permanent is a legal target, a land is not") {
                    legal shouldContain bears
                    legal shouldNotContain forest
                }
                game.submitDecision(TargetsResponse(targets.id, mapOf(0 to listOf(bears)))).error shouldBe null

                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                game.chooseMode("Tap")
                game.resolveStack()

                withClue("the trigger tapped the Bears") { game.isTapped(bears) shouldBe true }

                game.castSpell(1, "Flash Probe").error shouldBe null
                withClue("the second spell this turn does not trigger") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                    game.state.stack.size shouldBe 1
                }
                game.resolveStack()
                game.getPendingDecision() shouldBe null
            }

            test("can untap instead — the direction is chosen on resolution") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stinging Lionfish")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardInHand(1, "Flash Probe")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Flash Probe").error shouldBe null
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)
                game.chooseMode("Untap")
                game.resolveStack()

                game.isTapped(bears) shouldBe false
            }

            test("does not trigger on your own turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stinging Lionfish")
                    .withCardInHand(1, "Flash Probe")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Flash Probe").error shouldBe null
                withClue("no trigger: only the probe is on the stack") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                    game.state.stack.size shouldBe 1
                }
                game.resolveStack()
                game.isTapped(game.findPermanent("Grizzly Bears")!!) shouldBe false
            }
        }
    }
}
