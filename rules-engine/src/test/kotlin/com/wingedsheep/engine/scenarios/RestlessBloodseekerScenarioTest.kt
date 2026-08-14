package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Restless Bloodseeker // Bloodsoaked Reveler (VOW #128).
 *
 *   Front — Restless Bloodseeker (1/3) — At the beginning of your end step, if you gained life this
 *           turn, create a Blood token. Sacrifice two Blood tokens: Transform. Sorcery speed.
 *   Back  — Bloodsoaked Reveler (3/3) — same end-step Blood. {4}{B}: Each opponent loses 2 life and
 *           you gain 2 life.
 *
 * Exercises the "sacrifice two Blood tokens" transform, the intervening-if end-step Blood trigger
 * (fires only when life was gained this turn), and the back face's drain-2 activated ability.
 */
class RestlessBloodseekerScenarioTest : ScenarioTestBase() {

    init {
        context("Restless Bloodseeker") {

            test("sacrificing two Blood tokens transforms it into Bloodsoaked Reveler") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Restless Bloodseeker", summoningSickness = false)
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val seeker = game.findPermanent("Restless Bloodseeker")!!
                val bloods = game.findPermanents("Blood")
                bloods.size shouldBe 2
                val abilityId = cardRegistry.getCard("Restless Bloodseeker")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = seeker,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = bloods),
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("both Blood tokens were sacrificed to pay the cost") {
                    game.findPermanents("Blood").size shouldBe 0
                }
                withClue("transformed into Bloodsoaked Reveler (3/3)") {
                    game.state.getEntity(seeker)!!.get<CardComponent>()!!.name shouldBe "Bloodsoaked Reveler"
                    game.state.projectedState.getPower(seeker) shouldBe 3
                    game.state.projectedState.getToughness(seeker) shouldBe 3
                }
            }

            test("end step creates a Blood token only when you gained life this turn") {
                // Traveling Minister ({T}: pump a creature, gain 1 life) provides the life gain.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Restless Bloodseeker", summoningSickness = false)
                    .withCardOnBattlefield(1, "Traveling Minister", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val minister = game.findPermanent("Traveling Minister")!!
                val seeker = game.findPermanent("Restless Bloodseeker")!!
                val ministerAbility = cardRegistry.getCard("Traveling Minister")!!.activatedAbilities.first().id

                // Tap the Minister to gain 1 life (targeting the Bloodseeker for the pump).
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = minister,
                        abilityId = ministerAbility,
                        targets = listOf(ChosenTarget.Permanent(seeker)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("gained life this turn → a Blood token is created at end step") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }

            test("end step creates no Blood token when no life was gained") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Restless Bloodseeker", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("no life gained → no Blood token") {
                    game.findPermanents("Blood").size shouldBe 0
                }
            }

            test("the Reveler's {4}{B} drains each opponent for 2 and gains you 2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodsoaked Reveler", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val reveler = game.findPermanent("Bloodsoaked Reveler")!!
                val drainAbility = cardRegistry.getCard("Bloodsoaked Reveler")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = reveler, abilityId = drainAbility)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("opponent loses 2 (20 -> 18)") { game.getLifeTotal(2) shouldBe 18 }
                withClue("you gain 2 (20 -> 22)") { game.getLifeTotal(1) shouldBe 22 }
            }
        }
    }
}
