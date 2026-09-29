package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Invasion of Ulgrotha // Grandmother Ravi Sengir.
 *
 * Front: enters, 3 damage to any other target and you gain 3 life. Back: flying; whenever a
 * creature an opponent controls dies, +1/+1 counter on it and you gain 1 life.
 */
class InvasionOfUlgrothaScenarioTest : ScenarioTestBase() {

    init {
        test("enters: 3 damage to an opponent's creature and you gain 3 life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Ulgrotha")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Ulgrotha").error shouldBe null
            game.resolveStack()
            val giant = game.findPermanent("Hill Giant")!!
            val sel = game.selectTargets(listOf(giant))
            withClue("select: ${sel.error}") { sel.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.getLifeTotal(1) shouldBe 23
        }

        test("enters: 3 damage to the opponent and you gain 3 life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Ulgrotha")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Ulgrotha").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 23
        }

        test("Grandmother Ravi Sengir: an opponent's creature dying grows it and gains 1 life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grandmother Ravi Sengir")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            val ravi = game.findPermanent("Grandmother Ravi Sengir")!!
            game.state.getEntity(ravi)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.getLifeTotal(1) shouldBe 21
        }

        test("Grandmother Ravi Sengir: your own creature dying does not trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grandmother Ravi Sengir")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            val ravi = game.findPermanent("Grandmother Ravi Sengir")!!
            (game.state.getEntity(ravi)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
