package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.GollumRiddleMaster
import com.wingedsheep.mtg.sets.definitions.mrd.cards.RaiseTheAlarm
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Gollum, Riddle Master (HOB #70) — {1}{B} Legendary Creature — Halfling Horror 3/1.
 *
 *   As Gollum enters, choose odd or even. (Zero is even.)
 *   Whenever an opponent casts a spell with mana value of the chosen quality, choose one that
 *   hasn't been chosen — • +1/+1 counter on Gollum • drain 2 • draw a card.
 *
 * The as-enters choice is preset through [CastChoicesComponent] the way the Siege tests do, so each
 * test pins one half of the parity gate. The card lowers the single printed trigger to two mirrored
 * triggers (odd-filtered and even-filtered), so the thing most worth proving is that the *wrong*
 * parity stays silent — a mis-gated mirror would fire on every opponent spell.
 */
class GollumRiddleMasterScenarioTest : ScenarioTestBase() {

    private val counterMode = "Put a +1/+1 counter on Gollum"
    private val drainMode = "Each opponent loses 2 life and you gain 2 life"
    private val drawMode = "Draw a card"

    private fun TestGame.setParity(gollum: EntityId, parity: String) {
        state = state.updateEntity(gollum) {
            it.with(
                CastChoicesComponent(
                    chosen = mapOf(ChoiceSlot.MODE to ChoiceValue.TextChoice(parity))
                )
            )
        }
    }

    private fun TestGame.chooseMode(decision: ChooseOptionDecision, description: String) {
        val index = decision.options.indexOf(description)
        check(index >= 0) { "Mode '$description' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        cardRegistry.register(GollumRiddleMaster)
        cardRegistry.register(RaiseTheAlarm)

        context("Gollum, Riddle Master") {

            test("odd: an opponent's mana-value-1 spell triggers the riddle") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gollum, Riddle Master")
                    .withCardInHand(2, "Lightning Bolt") // mana value 1 — odd
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gollum = game.findPermanent("Gollum, Riddle Master")!!
                game.setParity(gollum, "odd")

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision()
                choice.shouldNotBeNull()
                choice as ChooseOptionDecision

                withClue("all three modes are available on the first trigger") {
                    choice.options.size shouldBe 3
                    choice.options shouldContain counterMode
                }

                game.chooseMode(choice, counterMode)
                game.resolveStack()

                withClue("the chosen mode put a +1/+1 counter on Gollum") {
                    counters(game, gollum) shouldBe 1
                }
            }

            test("odd: an opponent's mana-value-2 spell does NOT trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gollum, Riddle Master")
                    .withCardInHand(2, "Raise the Alarm") // mana value 2 — even
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gollum = game.findPermanent("Gollum, Riddle Master")!!
                game.setParity(gollum, "odd")

                game.castSpell(2, "Raise the Alarm").error shouldBe null
                game.resolveStack()

                withClue("an even spell must not wake the odd-gated trigger") {
                    game.hasPendingDecision() shouldBe false
                    counters(game, gollum) shouldBe 0
                }
            }

            test("even: an opponent's mana-value-2 spell triggers and drains") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gollum, Riddle Master")
                    .withCardInHand(2, "Raise the Alarm") // mana value 2 — even
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gollum = game.findPermanent("Gollum, Riddle Master")!!
                game.setParity(gollum, "even")

                val myLife = game.getLifeTotal(1)
                val theirLife = game.getLifeTotal(2)

                game.castSpell(2, "Raise the Alarm").error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision()
                choice.shouldNotBeNull()
                game.chooseMode(choice as ChooseOptionDecision, drainMode)
                game.resolveStack()

                withClue("each opponent loses 2 and Gollum's controller gains 2") {
                    game.getLifeTotal(2) shouldBe theirLife - 2
                    game.getLifeTotal(1) shouldBe myLife + 2
                }
            }

            test("a mode already taken is not offered again") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gollum, Riddle Master")
                    .withCardsInHand(2, "Lightning Bolt", 2)
                    .withLandsOnBattlefield(2, "Mountain", 6)
                    // The draw mode is taken below — without a library Gollum's controller would
                    // lose to the empty-draw state-based action before the second Bolt is cast.
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gollum = game.findPermanent("Gollum, Riddle Master")!!
                game.setParity(gollum, "odd")

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                val first = game.getPendingDecision() as ChooseOptionDecision
                first.options.size shouldBe 3
                game.chooseMode(first, drawMode)
                game.resolveStack()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                val second = game.getPendingDecision() as ChooseOptionDecision

                withClue("'choose one that hasn't been chosen' drops the used mode") {
                    second.options.size shouldBe 2
                    second.options shouldNotContain drawMode
                }
            }
        }
    }
}
