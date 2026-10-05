package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wavebreak Hippocamp ({2}{U}, 2/2 Enchantment Creature — Horse Fish).
 *
 * Whenever you cast your first spell during each opponent's turn, draw a card.
 *
 * The first spell on an opponent's turn draws; the second does not; your own turn never does.
 */
class WavebreakHippocampScenarioTest : ScenarioTestBase() {

    // A {0} instant standing in for "casting a spell".
    private val flashProbe = card("Flash Probe") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "You gain 1 life."
        spell {
            effect = Effects.GainLife(1)
        }
    }

    init {
        cardRegistry.register(flashProbe)

        context("Wavebreak Hippocamp") {

            test("first spell on an opponent's turn draws a card; the second does not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wavebreak Hippocamp")
                    .withCardsInHand(1, "Flash Probe", 2)
                    .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island").withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Flash Probe").error shouldBe null
                withClue("the trigger sits above the probe") { game.state.stack.size shouldBe 2 }
                game.resolveStack()
                withClue("one probe left in hand plus the drawn card") { game.handSize(1) shouldBe 2 }

                game.castSpell(1, "Flash Probe").error shouldBe null
                withClue("the second spell this turn does not trigger") { game.state.stack.size shouldBe 1 }
                game.resolveStack()
                game.handSize(1) shouldBe 1
            }

            test("does not trigger on your own turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wavebreak Hippocamp")
                    .withCardInHand(1, "Flash Probe")
                    .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island").withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Flash Probe").error shouldBe null
                withClue("no trigger: only the probe is on the stack") { game.state.stack.size shouldBe 1 }
                game.resolveStack()
                game.handSize(1) shouldBe 0
            }
        }
    }
}
