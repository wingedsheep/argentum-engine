package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Bloodchief Ascension (ZEN #82).
 *
 * Proves the end-step intervening "if" reads an opponent's life lost this turn (counted through
 * `countPlayersWith`), and that the graveyard trigger fires for a card going to an opponent's
 * graveyard only once the enchantment holds three quest counters — draining 2 when accepted.
 */
class BloodchiefAscensionScenarioTest : ScenarioTestBase() {

    private fun questCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.QUEST) ?: 0

    private fun setQuestCounters(game: TestGame, id: EntityId, n: Int) {
        game.state = game.state.updateEntity(id) {
            it.with(CountersComponent(mapOf(CounterType.QUEST to n)))
        }
    }

    init {
        context("Bloodchief Ascension") {
            test("an opponent who lost 2 life this turn lets you add a quest counter at end step") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodchief Ascension")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(3) { builder = builder.withCardInLibrary(1, "Swamp") }
                repeat(3) { builder = builder.withCardInLibrary(2, "Swamp") }
                val game = builder.build()
                val ascension = game.findPermanent("Bloodchief Ascension").shouldNotBeNull()

                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 18

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                questCounters(game, ascension) shouldBe 1
            }

            test("with no life lost this turn, the end-step ability doesn't trigger") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodchief Ascension")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(3) { builder = builder.withCardInLibrary(1, "Swamp") }
                repeat(3) { builder = builder.withCardInLibrary(2, "Swamp") }
                val game = builder.build()
                val ascension = game.findPermanent("Bloodchief Ascension").shouldNotBeNull()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                questCounters(game, ascension) shouldBe 0
            }

            test("with three quest counters, an opponent's card hitting the graveyard drains 2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodchief Ascension")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                setQuestCounters(game, game.findPermanent("Bloodchief Ascension")!!, 3)
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("the Bears going to Player2's graveyard triggers; the Bolt in ours doesn't") {
                    game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 22
            }

            test("with fewer than three quest counters, the graveyard ability doesn't trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodchief Ascension")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                setQuestCounters(game, game.findPermanent("Bloodchief Ascension")!!, 2)
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.hasPendingDecision() shouldBe false
                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
