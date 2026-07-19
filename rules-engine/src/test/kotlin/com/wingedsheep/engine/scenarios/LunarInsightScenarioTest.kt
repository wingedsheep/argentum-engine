package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fdn.cards.LunarInsight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Lunar Insight (FDN #46) — "Draw a card for each different mana value among
 * nonland permanents you control."
 *
 * Proves the count is over *distinct* mana values (two mana-value-4 permanents fold into one) and
 * that lands are excluded: an artifact (MV 2) plus two MV-4 creatures = two distinct mana values,
 * so the caster draws two cards despite controlling three nonland permanents and several lands.
 */
class LunarInsightScenarioTest : ScenarioTestBase() {

    private val orbMv2 = card("Test Orb Mv2") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }

    private val golemMv4A = card("Test Golem A") {
        manaCost = "{4}"
        typeLine = "Artifact Creature — Golem"
        power = 2
        toughness = 2
    }

    private val golemMv4B = card("Test Golem B") {
        manaCost = "{4}"
        typeLine = "Artifact Creature — Golem"
        power = 2
        toughness = 2
    }

    init {
        cardRegistry.register(LunarInsight)
        cardRegistry.register(orbMv2)
        cardRegistry.register(golemMv4A)
        cardRegistry.register(golemMv4B)

        test("draws one card per distinct mana value among nonland permanents, ignoring lands") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Lunar Insight")
                .withCardOnBattlefield(1, "Test Orb Mv2")
                .withCardOnBattlefield(1, "Test Golem A")
                .withCardOnBattlefield(1, "Test Golem B")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Lunar Insight").error shouldBe null
            game.resolveStack()

            withClue("Distinct mana values {2, 4} among nonland permanents → draw 2 cards") {
                game.handSize(1) shouldBe 2
            }
        }
    }
}
