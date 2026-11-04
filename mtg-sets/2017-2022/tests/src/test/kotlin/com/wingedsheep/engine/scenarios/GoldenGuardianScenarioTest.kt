package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rix.cards.GoldenGuardian
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Golden Guardian // Gold-Forge Garrison (RIX #179).
 *
 *   Front — Defender. {2}: This creature fights another target creature you control. When this
 *           creature dies this turn, return it to the battlefield transformed under your control.
 *   Back  — {T}: Add two mana of any one color. {4}, {T}: Create a 4/4 colorless Golem artifact
 *           creature token.
 *
 * Pins: dying in the fight returns the Guardian as the land; the rider lasts the whole turn, so
 * dying to something else after a survived fight also returns it; and the land makes Golems.
 */
class GoldenGuardianScenarioTest : ScenarioTestBase() {

    private val fightAbility = GoldenGuardian.activatedAbilities.single().id

    private fun TestGame.activateFight(target: String) {
        val guardian = findPermanent("Golden Guardian")!!
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = guardian,
                abilityId = fightAbility,
                targets = listOf(ChosenTarget.Permanent(findPermanent(target)!!)),
            )
        ).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        context("Golden Guardian") {

            test("dying in the fight returns it transformed into Gold-Forge Garrison") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Golden Guardian", summoningSickness = false)
                    .withCardOnBattlefield(1, "Colossal Dreadmaw", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateFight("Colossal Dreadmaw")

                withClue("the Guardian left the graveyard on its land face") {
                    game.isInGraveyard(1, "Golden Guardian") shouldBe false
                    game.isOnBattlefield("Golden Guardian") shouldBe false
                    val garrison = game.findPermanent("Gold-Forge Garrison").shouldNotBeNull()
                    game.state.projectedState.getController(garrison) shouldBe game.player1Id
                    game.state.projectedState.isCreature(garrison) shouldBe false
                }
                withClue("the Dreadmaw survived 4 damage") {
                    game.isOnBattlefield("Colossal Dreadmaw") shouldBe true
                }
            }

            test("dying later in the turn to another cause still returns it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Golden Guardian", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateFight("Grizzly Bears")
                withClue("the Guardian survived the fight") {
                    game.isOnBattlefield("Golden Guardian") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                val guardian = game.findPermanent("Golden Guardian")!!
                game.castSpell(1, "Lightning Bolt", targetId = guardian).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInGraveyard(1, "Golden Guardian") shouldBe false
                game.findPermanent("Gold-Forge Garrison").shouldNotBeNull()
            }

            test("Gold-Forge Garrison makes a 4/4 colorless Golem artifact creature token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Golden Guardian", summoningSickness = false)
                    .withCardOnBattlefield(1, "Colossal Dreadmaw", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateFight("Colossal Dreadmaw")
                val garrison = game.findPermanent("Gold-Forge Garrison").shouldNotBeNull()
                val makeGolem = GoldenGuardian.backFace!!.activatedAbilities.first { !it.isManaAbility }.id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = garrison, abilityId = makeGolem)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val golems = game.findPermanents("Golem Token")
                golems shouldHaveSize 1
                val projected = game.state.projectedState
                projected.getPower(golems.single()) shouldBe 4
                projected.getToughness(golems.single()) shouldBe 4
                projected.isCreature(golems.single()) shouldBe true
                projected.hasType(golems.single(), "ARTIFACT") shouldBe true
            }
        }
    }
}
