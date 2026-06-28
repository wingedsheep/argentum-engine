package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Stuck in Summoner's Sanctum (FIN #76) — {2}{U} Enchantment — Aura.
 *
 * "Flash
 *  Enchant artifact or creature
 *  When this Aura enters, tap enchanted permanent.
 *  Enchanted permanent doesn't untap during its controller's untap step and its activated
 *  abilities can't be activated."
 *
 * Focuses on the new behavior: the activation lock scoped to just the Aura's host via
 * `PreventActivatedAbilities(GameObjectFilter.Permanent.attachedToBySource())`. The
 * "doesn't untap" half reuses the engine's DOESNT_UNTAP flag (covered by Charmed Sleep).
 */
class StuckInSummonersSanctumScenarioTest : ScenarioTestBase() {

    private val elvesManaAbilityId =
        cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities.first().id

    init {
        test("enchanted permanent's activated abilities can't be activated") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                .withCardAttachedTo(1, "Stuck in Summoner's Sanctum", "Llanowar Elves")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val elves = game.findPermanent("Llanowar Elves")!!
            val elvesActivation = game.getLegalActions(2).find {
                val a = it.action
                a is ActivateAbility && a.sourceId == elves && a.abilityId == elvesManaAbilityId
            }
            withClue("Stuck in Summoner's Sanctum should lock the enchanted creature's mana ability") {
                elvesActivation shouldBe null
            }
        }

        test("the same creature's ability is available when not enchanted (control)") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val elves = game.findPermanent("Llanowar Elves")!!
            val elvesActivation = game.getLegalActions(2).find {
                val a = it.action
                a is ActivateAbility && a.sourceId == elves && a.abilityId == elvesManaAbilityId
            }
            withClue("Without the Aura, Llanowar Elves' mana ability should be available") {
                (elvesActivation != null) shouldBe true
            }
        }
    }
}
