package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Angelic Protector (TMP) — "Flying / Whenever this creature becomes the target of a spell or
 * ability, this creature gets +0/+3 until end of turn."
 *
 * The trigger goes on the stack above whatever targeted it and resolves first (the 2009-10-01
 * ruling), so a Shock aimed at the 2/2 meets a 2/5. It fires for abilities as well as spells, and
 * the bonus wears off at end of turn.
 */
class AngelicProtectorScenarioTest : ScenarioTestBase() {

    init {
        context("becomes the target of a spell") {
            test("the +0/+3 resolves before the spell, so Shock doesn't kill it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Angelic Protector")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val protector = game.findPermanent("Angelic Protector")
                protector.shouldNotBeNull()

                game.castSpell(2, "Shock", protector).error shouldBe null
                game.resolveStack()

                withClue("the trigger resolved first, making it a 2/5 that survives 2 damage") {
                    game.isOnBattlefield("Angelic Protector") shouldBe true
                    game.state.projectedState.getPower(protector) shouldBe 2
                    game.state.projectedState.getToughness(protector) shouldBe 5
                }
            }
        }

        context("becomes the target of an ability") {
            test("also triggers, and the bonus ends at end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Angelic Protector")
                    .withCardOnBattlefield(2, "Prodigal Sorcerer")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val protector = game.findPermanent("Angelic Protector")
                protector.shouldNotBeNull()
                val tim = game.findPermanent("Prodigal Sorcerer")!!
                val abilityId = cardRegistry.getCard("Prodigal Sorcerer")!!
                    .script.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = tim,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, protector))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("an ability targeting it triggers the +0/+3 too") {
                    game.isOnBattlefield("Angelic Protector") shouldBe true
                    game.state.projectedState.getToughness(protector) shouldBe 5
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("the bonus lasted only until end of turn") {
                    game.isOnBattlefield("Angelic Protector") shouldBe true
                    game.state.projectedState.getToughness(protector) shouldBe 2
                }
            }
        }
    }
}
