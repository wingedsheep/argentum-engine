package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.MindlessConscription
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Mindless Conscription (MH3 #101): "When this enchantment enters and whenever you draw your
 * third card each turn, amass Zombies 3."
 */
class MindlessConscriptionScenarioTest : ScenarioTestBase() {

    private val drawOne = card("Conscription Draw One Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }

    private fun ScenarioTestBase.TestGame.armyCounters(): Int? {
        val army = findPermanent("Zombie Army") ?: return null
        return state.getEntity(army)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE)
    }

    init {
        cardRegistry.register(MindlessConscription)
        cardRegistry.register(drawOne)

        test("entering amasses Zombies 3, creating a Zombie Army with three counters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Mindless Conscription")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Mindless Conscription").error shouldBe null
            game.resolveStack()

            withClue("the ETB trigger created a Zombie Army with three +1/+1 counters") {
                game.findAllPermanents("Zombie Army") shouldHaveSize 1
                game.armyCounters() shouldBe 3
            }
        }

        test("the third draw of the turn amasses again; the first two and the fourth do not") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mindless Conscription")
                .withCardsInHand(1, "Conscription Draw One Test", 4)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardsDrawnThisTurn(1, 0)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            repeat(2) {
                game.castSpell(1, "Conscription Draw One Test").error shouldBe null
                game.resolveStack()
            }
            withClue("two draws: no Army yet") { game.findPermanent("Zombie Army") shouldBe null }

            game.castSpell(1, "Conscription Draw One Test").error shouldBe null
            game.resolveStack()
            withClue("third draw: amass Zombies 3") { game.armyCounters() shouldBe 3 }

            game.castSpell(1, "Conscription Draw One Test").error shouldBe null
            game.resolveStack()
            withClue("fourth draw: no further amass") {
                game.findAllPermanents("Zombie Army") shouldHaveSize 1
                game.armyCounters() shouldBe 3
            }
        }

        test("an opponent's third draw does not trigger it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mindless Conscription")
                .withCardsInHand(2, "Conscription Draw One Test", 3)
                .withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withCardsDrawnThisTurn(2, 0)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            repeat(3) {
                game.castSpell(2, "Conscription Draw One Test").error shouldBe null
                game.resolveStack()
            }
            game.findPermanent("Zombie Army") shouldBe null
        }
    }
}
