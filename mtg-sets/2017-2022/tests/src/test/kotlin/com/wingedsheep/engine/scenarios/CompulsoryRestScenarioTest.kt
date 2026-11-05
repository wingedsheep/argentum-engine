package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Compulsory Rest (AKH #9) — {1}{W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature can't attack or block.
 *   Enchanted creature has "{2}, Sacrifice this creature: You gain 2 life."
 */
class CompulsoryRestScenarioTest : ScenarioTestBase() {

    private val grantedAbilityId by lazy {
        cardRegistry.requireCard("Compulsory Rest")
            .staticAbilities.filterIsInstance<GrantActivatedAbility>().first().ability.id
    }

    init {
        context("Compulsory Rest") {

            test("the enchanted creature can't attack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(2, "Compulsory Rest", "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldNotBe null
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            }

            test("the enchanted creature's controller sacrifices it to gain 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Compulsory Rest", "Hill Giant")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = game.findPermanent("Hill Giant")!!,
                        abilityId = grantedAbilityId,
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Hill Giant was sacrificed") { game.isInGraveyard(2, "Hill Giant") shouldBe true }
                withClue("its controller, not the Aura's, gains 2") {
                    game.getLifeTotal(2) shouldBe 22
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
