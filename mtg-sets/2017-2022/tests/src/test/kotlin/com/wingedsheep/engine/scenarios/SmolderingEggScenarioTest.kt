package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Smoldering Egg // Ashmouth Dragon (MID).
 *
 * Pins the ember count (the mana spent on the instant or sorcery), the seven-counter transform that
 * clears them, and the Dragon's 2 damage per instant or sorcery cast.
 */
class SmolderingEggScenarioTest : ScenarioTestBase() {

    private fun builder() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Smoldering Egg")
        .withLandsOnBattlefield(1, "Mountain", 7)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.name(id: EntityId): String = state.getEntity(id)!!.get<CardComponent>()!!.name

    private fun TestGame.embers(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.EMBER) ?: 0

    init {
        context("Smoldering Egg") {
            test("adds ember counters equal to the mana spent on the spell") {
                val game = builder().withCardInHand(1, "Lava Axe").build()
                val egg = game.findPermanent("Smoldering Egg")!!

                game.castSpellTargetingPlayer(1, "Lava Axe", 2).error shouldBe null
                game.resolveStack()

                game.embers(egg) shouldBe 5
                game.name(egg) shouldBe "Smoldering Egg"
            }

            test("at seven or more it removes them all and transforms") {
                val game = builder().withCardInHand(1, "Lava Axe").build()
                val egg = game.findPermanent("Smoldering Egg")!!
                game.state = game.state.updateEntity(egg) {
                    it.with(CountersComponent(mapOf(CounterType.EMBER to 3)))
                }

                game.castSpellTargetingPlayer(1, "Lava Axe", 2).error shouldBe null
                game.resolveStack()

                game.name(egg) shouldBe "Ashmouth Dragon"
                withClue("all eight ember counters are removed, not just seven") {
                    game.embers(egg) shouldBe 0
                }
            }
        }

        context("Ashmouth Dragon") {
            test("deals 2 damage to any target whenever you cast an instant or sorcery") {
                val game = builder().withCardInHand(1, "Lava Axe").withCardInHand(1, "Shock").build()
                val egg = game.findPermanent("Smoldering Egg")!!
                game.state = game.state.updateEntity(egg) {
                    it.with(CountersComponent(mapOf(CounterType.EMBER to 2)))
                }
                game.castSpellTargetingPlayer(1, "Lava Axe", 2).error shouldBe null
                game.resolveStack()
                game.name(egg) shouldBe "Ashmouth Dragon"
                game.getLifeTotal(2) shouldBe 15

                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                withClue("the Dragon's trigger asks for a target") { game.hasPendingDecision() shouldBe true }
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("2 from the Dragon, then 2 from Shock") { game.getLifeTotal(2) shouldBe 11 }
            }
        }
    }
}
