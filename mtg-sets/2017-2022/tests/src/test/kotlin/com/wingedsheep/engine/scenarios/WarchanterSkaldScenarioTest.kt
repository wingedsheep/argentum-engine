package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Warchanter Skald (KHM #381).
 *
 * Whenever this creature becomes tapped, if it's enchanted or equipped, create a 2/1 red Dwarf
 * Berserker creature token.
 *
 * Covers the intervening-if over the becomes-tapped trigger: equipped and enchanted each satisfy
 * it, a bare Skald makes nothing.
 */
class WarchanterSkaldScenarioTest : ScenarioTestBase() {

    init {
        fun attackWithSkald(attachment: String?): TestGame {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Warchanter Skald", summoningSickness = false)
            if (attachment != null) builder.withCardAttachedTo(1, attachment, "Warchanter Skald")
            val game = builder
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Warchanter Skald" to 2)).error shouldBe null
            game.resolveStack()
            return game
        }

        context("Warchanter Skald") {
            test("equipped: becoming tapped by attacking creates a 2/1 Dwarf Berserker") {
                val game = attackWithSkald("Short Sword")
                val tokens = game.findPermanents("Dwarf Berserker Token")
                tokens shouldHaveSize 1
                withClue("The token is a 2/1") {
                    game.state.projectedState.getPower(tokens.single()) shouldBe 2
                    game.state.projectedState.getToughness(tokens.single()) shouldBe 1
                }
            }

            test("enchanted: becoming tapped by attacking creates a token") {
                val game = attackWithSkald("Holy Strength")
                game.findPermanents("Dwarf Berserker Token") shouldHaveSize 1
            }

            test("neither enchanted nor equipped: no token") {
                val game = attackWithSkald(null)
                game.findPermanents("Dwarf Berserker Token").shouldBeEmpty()
            }
        }
    }
}
