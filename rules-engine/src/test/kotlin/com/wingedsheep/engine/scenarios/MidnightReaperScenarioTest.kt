package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

/**
 * Midnight Reaper — "{2}{B} 3/2. Whenever a nontoken creature you control dies, this creature
 * deals 1 damage to you and you draw a card."
 *
 * The notable bit is that the trigger is ANY-bound over "a nontoken creature you control", so it
 * fires on Midnight Reaper's own death too. It's mandatory (no "may").
 */
class MidnightReaperScenarioTest : ScenarioTestBase() {

    init {
        // {0} sorcery to send a chosen creature to the graveyard on demand.
        val slay = card("Slay") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell {
                val c = target("target creature", Targets.Creature)
                effect = Effects.Destroy(c)
            }
        }
        cardRegistry.register(listOf(slay))

        test("a nontoken creature you control dies: take 1 damage and draw a card") {
            val game = scenario()
                .withPlayers()
                .withLifeTotal(1, 20)
                .withCardOnBattlefield(1, "Midnight Reaper")
                .withCardOnBattlefield(1, "Aegis Turtle")
                .withCardInHand(1, "Slay")
                .withCardInLibrary(1, "Bear Cub")
                .build()

            val turtle = game.findPermanent("Aegis Turtle")!!
            game.castSpell(1, "Slay", targetId = turtle).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 19
            game.isInHand(1, "Bear Cub") shouldBe true
        }

        test("Midnight Reaper's own death triggers it (ANY binding includes itself)") {
            val game = scenario()
                .withPlayers()
                .withLifeTotal(1, 20)
                .withCardOnBattlefield(1, "Midnight Reaper")
                .withCardInHand(1, "Slay")
                .withCardInLibrary(1, "Bear Cub")
                .build()

            val reaper = game.findPermanent("Midnight Reaper")!!
            game.castSpell(1, "Slay", targetId = reaper).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 19
            game.isInHand(1, "Bear Cub") shouldBe true
        }

        test("a creature an opponent controls dying does not trigger it") {
            val game = scenario()
                .withPlayers()
                .withLifeTotal(1, 20)
                .withCardOnBattlefield(1, "Midnight Reaper")
                .withCardOnBattlefield(2, "Aegis Turtle")
                .withCardInHand(1, "Slay")
                .withCardInLibrary(1, "Bear Cub")
                .build()

            val turtle = game.findPermanent("Aegis Turtle")!!
            game.castSpell(1, "Slay", targetId = turtle).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
            game.isInHand(1, "Bear Cub") shouldBe false
        }
    }
}
