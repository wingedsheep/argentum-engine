package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Sunfall — "Exile all creatures. Incubate X, where X is the number of creatures exiled this way."
 */
class SunfallScenarioTest : ScenarioTestBase() {
    init {
        context("Sunfall") {
            test("exiles every creature on both sides, tokens included, and incubates that many") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sunfall")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Phyrexian Awakening")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sunfall").error shouldBe null
                game.resolveStack()

                withClue("no creature besides the new Incubator remains") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.findPermanent("Hill Giant") shouldBe null
                }
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Hill Giant") shouldBe true
                withClue("noncreature permanents stay") {
                    game.findPermanent("Phyrexian Awakening").shouldNotBeNull()
                }

                val incubator = game.findPermanent("Incubator").shouldNotBeNull()
                game.state.getBattlefield(game.player1Id).contains(incubator) shouldBe true
                withClue("three creatures (one a token) were exiled") {
                    game.state.getEntity(incubator)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                }
            }

            test("with no creatures it still incubates 0") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sunfall")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sunfall").error shouldBe null
                game.resolveStack()

                val incubator = game.findPermanent("Incubator").shouldNotBeNull()
                (game.state.getEntity(incubator)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
            }
        }
    }
}
