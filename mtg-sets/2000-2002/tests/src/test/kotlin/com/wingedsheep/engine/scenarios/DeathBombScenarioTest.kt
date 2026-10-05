package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Death Bomb — {3}{B} Instant. "As an additional cost to cast this spell, sacrifice a creature.
 * Destroy target nonblack creature. It can't be regenerated. Its controller loses 2 life."
 *
 * The card used to *gain* life for the destroyed creature; the target's controller loses it.
 */
class DeathBombScenarioTest : ScenarioTestBase() {

    init {
        context("Death Bomb") {
            test("destroys the nonblack creature and its controller loses 2 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Death Bomb")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fodder = game.findPermanent("Grizzly Bears")!!
                val victim = game.findPermanent("Hill Giant")!!
                val spell = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Death Bomb"
                }
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        spell,
                        listOf(ChosenTarget.Permanent(victim)),
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
                    )
                )
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Both creatures are gone") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("The target's controller loses 2 life, the caster none") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
