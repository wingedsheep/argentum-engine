package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gift of Compleation — "When this enchantment enters, incubate 3. Whenever a Phyrexian you
 * control dies, surveil 1."
 */
class GiftOfCompleationScenarioTest : ScenarioTestBase() {
    init {
        context("Gift of Compleation") {
            test("entering creates an Incubator with three counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Gift of Compleation")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gift of Compleation").error shouldBe null
                game.resolveStack()

                val incubator = game.findPermanent("Incubator").shouldNotBeNull()
                game.state.getEntity(incubator)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
            }

            test("a Phyrexian you control dying surveils 1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gift of Compleation")
                    .withCardOnBattlefield(1, "Phyrexian Ghoul")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ghoul = game.findPermanent("Phyrexian Ghoul")!!
                game.castSpell(1, "Lightning Bolt", ghoul).error shouldBe null
                game.resolveStack()

                withClue("the Phyrexian died and its controller surveils") {
                    game.isOnBattlefield("Phyrexian Ghoul") shouldBe false
                    game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                }
            }

            test("a non-Phyrexian dying does not surveil") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gift of Compleation")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.hasPendingDecision() shouldBe false
            }
        }
    }
}
