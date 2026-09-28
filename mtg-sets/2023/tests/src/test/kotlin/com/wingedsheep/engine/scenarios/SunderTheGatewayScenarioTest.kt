package com.wingedsheep.engine.scenarios

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
 * Sunder the Gateway (MOM #39) — "Choose one — • Destroy target nontoken artifact or enchantment
 * an opponent controls. Incubate 2. • Incubate 2, then transform an Incubator token you control."
 */
class SunderTheGatewayScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(block: ScenarioBuilder.() -> ScenarioBuilder = { this }) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Sunder the Gateway")
        .withLandsOnBattlefield(1, "Plains", 6)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .block()
        .build()

    init {
        context("Sunder the Gateway") {

            test("mode one destroys an opponent's nontoken artifact and incubates 2") {
                val game = board { withCardOnBattlefield(2, "Millstone") }
                val millstone = game.findPermanent("Millstone")!!

                game.castSpellWithMode(1, "Sunder the Gateway", 0, millstone).error shouldBe null
                game.resolveStack()

                withClue("Millstone destroyed") { game.isInGraveyard(2, "Millstone") shouldBe true }
                val incubator = game.findPermanent("Incubator")
                withClue("an untransformed Incubator with two +1/+1 counters") {
                    incubator shouldNotBe null
                    game.plusOneCounters(incubator!!) shouldBe 2
                    game.findPermanent("Phyrexian") shouldBe null
                }
            }

            test("mode one cannot target a token artifact or your own artifact") {
                val game = board {
                    withCardOnBattlefield(2, "Ornithopter", isToken = true)
                        .withCardOnBattlefield(1, "Millstone")
                }
                val tokenThopter = game.findPermanent("Ornithopter")!!
                val ownMillstone = game.findPermanent("Millstone")!!

                game.castSpellWithMode(1, "Sunder the Gateway", 0, tokenThopter).error shouldNotBe null
                game.castSpellWithMode(1, "Sunder the Gateway", 0, ownMillstone).error shouldNotBe null
            }

            test("mode two incubates 2 and transforms the only Incubator") {
                val game = board()

                game.castSpellWithMode(1, "Sunder the Gateway", 1).error shouldBe null
                game.resolveStack()

                withClue("the lone Incubator was chosen and transformed into a 2/2 Phyrexian") {
                    game.findPermanent("Incubator") shouldBe null
                    val phyrexian = game.findPermanent("Phyrexian")
                    phyrexian shouldNotBe null
                    game.plusOneCounters(phyrexian!!) shouldBe 2
                }
            }

            test("mode two may transform an Incubator you already controlled instead") {
                val game = board { withCardInHand(1, "Norn's Inquisitor") }
                game.castSpell(1, "Norn's Inquisitor").error shouldBe null
                game.resolveStack()
                val oldIncubator = game.findPermanent("Incubator")!!

                game.castSpellWithMode(1, "Sunder the Gateway", 1).error shouldBe null
                game.resolveStack()

                withClue("two Incubators to choose between") {
                    game.findPermanents("Incubator").size shouldBe 2
                }
                game.selectCards(listOf(oldIncubator)).error shouldBe null
                game.resolveStack()

                withClue("the old Incubator transformed; the new one stays an Incubator") {
                    game.findPermanent("Phyrexian") shouldBe oldIncubator
                    val remaining = game.findPermanents("Incubator")
                    remaining.size shouldBe 1
                    remaining.single() shouldNotBe oldIncubator
                }
            }
        }
    }
}
