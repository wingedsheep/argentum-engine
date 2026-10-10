package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Saruman, the White Hand (LTC #8).
 *
 * "Whenever you cast a noncreature spell, amass Orcs X, where X is that spell's mana value.
 *  Goblins and Orcs you control have ward {2}."
 *
 * Exercises X read off the triggering spell's mana value, the creature-spell exclusion, and the
 * ward grant reaching an Army that became an Orc only by amassing (projected subtype).
 */
class SarumanTheWhiteHandScenarioTest : ScenarioTestBase() {

    init {
        context("Saruman, the White Hand") {

            test("casting a noncreature spell amasses Orcs X = its mana value, and the Orc Army has ward") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Saruman, the White Hand")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Divination")
                withClue("casting Divination should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val army = game.findPermanent("Orc Army")
                    ?: error("Saruman's trigger should have created an Orc Army")
                withClue("amass Orcs 3 puts three +1/+1 counters on the Army") {
                    game.state.getEntity(army)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                }
                withClue("the Orc Army is a 3/3") {
                    game.state.projectedState.getPower(army) shouldBe 3
                }
                withClue("the Orc Army has ward {2} from Saruman") {
                    game.state.projectedState.hasKeyword(army, Keyword.WARD) shouldBe true
                }
                val saruman = game.findPermanent("Saruman, the White Hand")!!
                withClue("Saruman (an Avatar Wizard) does not grant itself ward") {
                    game.state.projectedState.hasKeyword(saruman, Keyword.WARD) shouldBe false
                }
            }

            test("a creature spell does not trigger amass") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Saruman, the White Hand")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Grizzly Bears")
                withClue("casting Grizzly Bears should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("no Orc Army is created for a creature spell") {
                    game.findPermanent("Orc Army") shouldBe null
                }
            }

            test("Goblins you control have ward {2}; opponents' Goblins do not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Saruman, the White Hand")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(2, "Goblin Piker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("your Goblin has ward") {
                    game.state.projectedState.hasKeyword(game.findPermanent("Raging Goblin")!!, Keyword.WARD) shouldBe true
                }
                withClue("an opponent's Goblin does not") {
                    game.state.projectedState.hasKeyword(game.findPermanent("Goblin Piker")!!, Keyword.WARD) shouldBe false
                }
            }
        }
    }
}
