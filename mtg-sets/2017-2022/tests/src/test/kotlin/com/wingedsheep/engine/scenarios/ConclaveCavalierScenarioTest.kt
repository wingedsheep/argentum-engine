package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe

/**
 * Conclave Cavalier (GRN #161) — {G}{G}{W}{W} 4/4 Centaur Knight.
 *
 *   Vigilance
 *   When this creature dies, create two 2/2 green and white Elf Knight creature tokens with vigilance.
 */
class ConclaveCavalierScenarioTest : ScenarioTestBase() {

    init {
        val slay = card("Slay") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell {
                val c = target(TargetFilter.Creature)
                effect = Effects.Destroy(c)
            }
        }
        cardRegistry.register(listOf(slay))

        test("dies: create two 2/2 Elf Knight tokens with vigilance") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Conclave Cavalier")
                .withCardInHand(1, "Slay")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cavalier = game.findPermanent("Conclave Cavalier")!!
            game.state.projectedState.hasKeyword(cavalier, Keyword.VIGILANCE) shouldBe true

            game.castSpell(1, "Slay", targetId = cavalier).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Conclave Cavalier") shouldBe true
            val knights = game.findPermanents("Elf Knight Token")
            knights.size shouldBe 2
            knights.forEach { knight ->
                game.state.projectedState.getPower(knight) shouldBe 2
                game.state.projectedState.getToughness(knight) shouldBe 2
                game.state.projectedState.hasKeyword(knight, Keyword.VIGILANCE) shouldBe true
            }
        }
    }
}
