package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Attended Healer (ZNR #6, reprinted in J22) — {3}{W} Creature — Kor Cleric, 2/3.
 *
 *   Whenever you gain life for the first time each turn, create a 1/1 white Cat creature token.
 *   {2}{W}: Another target Cleric gains lifelink until end of turn.
 *
 * Venerable Monk (Human Monk Cleric, "When this creature enters, you gain 2 life") is both the
 * life-gain source and the other Cleric to target.
 */
class AttendedHealerScenarioTest : ScenarioTestBase() {

    init {
        context("Attended Healer's life-gain trigger") {

            test("first life gain this turn creates a Cat; a second gain the same turn does not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Attended Healer", summoningSickness = false)
                    .withCardInHand(1, "Venerable Monk")
                    .withCardInHand(1, "Venerable Monk")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("no Cat before any life is gained") {
                    game.findPermanents("Cat Token").size shouldBe 0
                }

                game.castSpell(1, "Venerable Monk")
                game.resolveStack()

                withClue("the first life gain this turn creates one Cat token") {
                    game.getLifeTotal(1) shouldBe 22
                    game.findPermanents("Cat Token").size shouldBe 1
                }

                game.castSpell(1, "Venerable Monk")
                game.resolveStack()

                withClue("a second life gain the same turn is not the first, so no second Cat") {
                    game.getLifeTotal(1) shouldBe 24
                    game.findPermanents("Cat Token").size shouldBe 1
                }
            }
        }

        context("Attended Healer's activated ability") {

            test("another target Cleric gains lifelink until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Attended Healer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Venerable Monk", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val healer = game.findPermanent("Attended Healer")!!
                val monk = game.findPermanent("Venerable Monk")!!
                val abilityId = cardRegistry.getCard("Attended Healer")!!
                    .script.activatedAbilities[0].id

                game.state.projectedState.hasKeyword(monk, Keyword.LIFELINK.name) shouldBe false

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = healer,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, monk)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the Monk is a Cleric, so it gains lifelink") {
                    game.state.projectedState.hasKeyword(monk, Keyword.LIFELINK.name) shouldBe true
                }
                withClue("the Healer itself is untouched") {
                    game.state.projectedState.hasKeyword(healer, Keyword.LIFELINK.name) shouldBe false
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("lifelink lasts only until end of turn") {
                    game.state.projectedState.hasKeyword(monk, Keyword.LIFELINK.name) shouldBe false
                }
            }

            test("cannot target the Healer itself or a non-Cleric") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Attended Healer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val healer = game.findPermanent("Attended Healer")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Attended Healer")!!
                    .script.activatedAbilities[0].id

                withClue("\"another\" excludes the Healer, though it is a Cleric") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = healer,
                            abilityId = abilityId,
                            targets = listOf(entityIdToChosenTarget(game.state, healer)),
                        )
                    ).error shouldNotBe null
                }
                withClue("Grizzly Bears is not a Cleric") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = healer,
                            abilityId = abilityId,
                            targets = listOf(entityIdToChosenTarget(game.state, bears)),
                        )
                    ).error shouldNotBe null
                }
            }
        }
    }
}
