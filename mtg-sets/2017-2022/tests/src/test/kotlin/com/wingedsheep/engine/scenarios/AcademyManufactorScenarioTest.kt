package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

/**
 * Academy Manufactor (MH2 #219) — "If you would create a Clue, Food, or Treasure token, instead
 * create one of each." Pins the three rulings: that many of each, three times that many with two
 * Manufactors, and other tokens untouched.
 */
class AcademyManufactorScenarioTest : ScenarioTestBase() {

    private val twoTreasures = card("Test Two Treasures") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateTreasure(2) }
    }

    private val oneClue = card("Test One Clue") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateClue(1) }
    }

    private val oneSoldier = card("Test One Soldier") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Soldier")) }
    }

    init {
        listOf(twoTreasures, oneClue, oneSoldier).forEach { cardRegistry.register(it) }

        fun cast(spell: String, manufactors: Int = 1): TestGame {
            val builder = scenario().withPlayers("P1", "P2")
            repeat(manufactors) { builder.withCardOnBattlefield(1, "Academy Manufactor") }
            val game = builder
                .withCardInHand(1, spell)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, spell)
            game.resolveStack()
            return game
        }

        fun TestGame.counts() = Triple(
            findPermanents("Clue").size, findPermanents("Food").size, findPermanents("Treasure").size
        )

        test("a Clue becomes one of each") {
            cast("Test One Clue").counts() shouldBe Triple(1, 1, 1)
        }

        test("two Treasures become two of each") {
            cast("Test Two Treasures").counts() shouldBe Triple(2, 2, 2)
        }

        test("two Manufactors make three times that many of each") {
            cast("Test One Clue", manufactors = 2).counts() shouldBe Triple(3, 3, 3)
        }

        test("other tokens are untouched") {
            val game = cast("Test One Soldier")
            game.findPermanents("Soldier Token").size shouldBe 1
            game.counts() shouldBe Triple(0, 0, 0)
        }
    }
}
