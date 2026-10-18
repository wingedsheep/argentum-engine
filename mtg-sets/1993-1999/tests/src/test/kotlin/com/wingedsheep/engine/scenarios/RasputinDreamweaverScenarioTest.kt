package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rasputin Dreamweaver (LEG #253).
 *
 * "Rasputin enters with seven dream counters on it.
 *  Remove a dream counter from Rasputin: Add {C}.
 *  Remove a dream counter from Rasputin: Prevent the next 1 damage that would be dealt to Rasputin this turn.
 *  At the beginning of your upkeep, if Rasputin started the turn untapped, put a dream counter on it.
 *  Rasputin can't have more than seven dream counters on it."
 */
class RasputinDreamweaverScenarioTest : ScenarioTestBase() {

    private val manaAbilityId by lazy {
        cardRegistry.getCard("Rasputin Dreamweaver")!!.script.activatedAbilities[0].id
    }
    private val preventAbilityId by lazy {
        cardRegistry.getCard("Rasputin Dreamweaver")!!.script.activatedAbilities[1].id
    }

    private fun TestGame.dreams(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.DREAM) ?: 0

    private fun TestGame.setDreams(id: EntityId, n: Int) {
        state = state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.DREAM, n)) }
    }

    /** Rasputin on player 1's battlefield, in player 2's main phase; [toRasputinMain] moves on to its upkeep. */
    private fun onBoard(tapped: Boolean = false) = scenario()
        .withPlayers("Dreamer", "Opponent")
        .withCardOnBattlefield(1, "Rasputin Dreamweaver", tapped = tapped)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.toRasputinMain() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        state.activePlayerId shouldBe player1Id
    }

    init {
        context("Rasputin Dreamweaver") {

            test("enters with seven dream counters") {
                val game = scenario()
                    .withPlayers("Dreamer", "Opponent")
                    .withCardInHand(1, "Rasputin Dreamweaver")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Rasputin Dreamweaver").error shouldBe null
                game.resolveStack()
                game.dreams(game.findPermanent("Rasputin Dreamweaver")!!) shouldBe 7
            }

            test("removing a dream counter adds {C}") {
                val game = scenario()
                    .withPlayers("Dreamer", "Opponent")
                    .withCardOnBattlefield(1, "Rasputin Dreamweaver")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val rasputin = game.findPermanent("Rasputin Dreamweaver")!!
                game.setDreams(rasputin, 7)
                game.execute(ActivateAbility(game.player1Id, rasputin, manaAbilityId)).error shouldBe null
                game.dreams(rasputin) shouldBe 6
                game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
            }

            test("removing dream counters prevents damage to Rasputin") {
                val game = scenario()
                    .withPlayers("Dreamer", "Opponent")
                    .withCardOnBattlefield(1, "Rasputin Dreamweaver")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val rasputin = game.findPermanent("Rasputin Dreamweaver")!!
                game.setDreams(rasputin, 7)
                repeat(2) {
                    game.execute(ActivateAbility(game.player1Id, rasputin, preventAbilityId)).error shouldBe null
                    game.resolveStack()
                }
                game.castSpell(1, "Shock", rasputin).error shouldBe null
                game.resolveStack()
                withClue("both shields absorb Shock's 2 damage") {
                    game.isOnBattlefield("Rasputin Dreamweaver") shouldBe true
                }
                game.dreams(rasputin) shouldBe 5
            }

            test("upkeep adds a dream counter when it started the turn untapped") {
                val game = onBoard()
                val rasputin = game.findPermanent("Rasputin Dreamweaver")!!
                game.setDreams(rasputin, 5)
                game.toRasputinMain()
                game.dreams(rasputin) shouldBe 6
            }

            test("no dream counter when it started the turn tapped") {
                val game = onBoard(tapped = true)
                val rasputin = game.findPermanent("Rasputin Dreamweaver")!!
                game.setDreams(rasputin, 5)
                game.toRasputinMain()
                game.dreams(rasputin) shouldBe 5
            }

            test("can't have more than seven dream counters") {
                val game = onBoard()
                val rasputin = game.findPermanent("Rasputin Dreamweaver")!!
                game.setDreams(rasputin, 7)
                game.toRasputinMain()
                game.dreams(rasputin) shouldBe 7
            }
        }
    }
}
