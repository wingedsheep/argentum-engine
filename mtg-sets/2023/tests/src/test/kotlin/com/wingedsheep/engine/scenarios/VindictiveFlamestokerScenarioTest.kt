package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vindictive Flamestoker (ONE #154).
 *
 *   Whenever you cast a noncreature spell, put an oil counter on this creature.
 *   {6}{R}, Sacrifice this creature: Discard your hand, then draw four cards. This ability costs
 *   {1} less to activate for each oil counter on this creature.
 */
class VindictiveFlamestokerScenarioTest : ScenarioTestBase() {

    private fun abilityId() = cardRegistry.getCard("Vindictive Flamestoker")!!.activatedAbilities.first().id

    private fun oil(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    private fun setOil(game: TestGame, id: EntityId, n: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.OIL, n))
        }
    }

    init {
        context("Vindictive Flamestoker") {

            test("noncreature spells add an oil counter; creature spells do not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vindictive Flamestoker")
                    .withCardInHand(1, "Divination")
                    .withCardInHand(1, "Raging Goblin")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stoker = game.findPermanent("Vindictive Flamestoker")!!

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()
                withClue("Divination added one oil counter") { oil(game, stoker) shouldBe 1 }

                game.castSpell(1, "Raging Goblin").error shouldBe null
                game.resolveStack()
                withClue("a creature spell adds nothing") { oil(game, stoker) shouldBe 1 }
            }

            test("four oil counters cut the cost to {2}{R}; discards the hand, then draws four") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vindictive Flamestoker")
                    .withCardInHand(1, "Raging Goblin")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stoker = game.findPermanent("Vindictive Flamestoker")!!
                setOil(game, stoker, 4)

                val result = game.execute(ActivateAbility(game.player1Id, stoker, abilityId()))
                withClue("{2}{R} from three Mountains: ${result.error}") { result.error shouldBe null }
                withClue("sacrificed as a cost") { game.isInGraveyard(1, "Vindictive Flamestoker") shouldBe true }
                game.resolveStack()

                withClue("old hand discarded") {
                    game.isInGraveyard(1, "Raging Goblin") shouldBe true
                    game.isInGraveyard(1, "Divination") shouldBe true
                }
                withClue("drew exactly four new cards, none discarded") {
                    game.handSize(1) shouldBe 4
                    game.findCardsInHand(1, "Island").size shouldBe 4
                }
            }

            test("the discount is only one per oil counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vindictive Flamestoker")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stoker = game.findPermanent("Vindictive Flamestoker")!!
                setOil(game, stoker, 3)

                val result = game.execute(ActivateAbility(game.player1Id, stoker, abilityId()))
                withClue("{3}{R} can't be paid from three Mountains") { result.error shouldNotBe null }
            }
        }
    }
}
