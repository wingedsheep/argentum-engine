package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Starfield Mystic (M20 #39) — {1}{W} 2/2 Human Cleric.
 *
 * "Enchantment spells you cast cost {1} less to cast.
 *  Whenever an enchantment you control is put into a graveyard from the battlefield, put a +1/+1
 *  counter on this creature."
 *
 * Covers the generic reduction on an enchantment spell, the counter when your enchantment is
 * destroyed, and no counter when an opponent's enchantment is.
 */
class StarfieldMysticScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Starfield Mystic") {

            test("enchantment spells you cast cost {1} less") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Starfield Mystic")
                    .withCardInHand(1, "Pacifism")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("{1}{W} Pacifism is castable off a single Plains") {
                    game.castSpell(1, "Pacifism", bears).error shouldBe null
                }
                game.resolveStack()

                game.isOnBattlefield("Pacifism") shouldBe true
            }

            test("your enchantment going to the graveyard puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Starfield Mystic")
                    .withCardOnBattlefield(1, "Rule of Law")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mystic = game.findPermanent("Starfield Mystic")!!
                val ruleOfLaw = game.findPermanent("Rule of Law")!!
                game.castSpell(1, "Disenchant", ruleOfLaw).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Rule of Law") shouldBe true
                withClue("one counter -> 3/3") {
                    game.plusOnes(mystic) shouldBe 1
                    game.state.projectedState.getPower(mystic) shouldBe 3
                    game.state.projectedState.getToughness(mystic) shouldBe 3
                }
            }

            test("an opponent's enchantment going to the graveyard adds nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Starfield Mystic")
                    .withCardOnBattlefield(2, "Rule of Law")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mystic = game.findPermanent("Starfield Mystic")!!
                val ruleOfLaw = game.findPermanent("Rule of Law")!!
                game.castSpell(1, "Disenchant", ruleOfLaw).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Rule of Law") shouldBe true
                game.plusOnes(mystic) shouldBe 0
            }
        }
    }
}
