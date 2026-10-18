package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Shimian Night Stalker — {3}{B}{B} 4/4 Creature — Nightstalker.
 *  - {B}, {T}: All damage that would be dealt to you this turn by target attacking creature is
 *    dealt to this creature instead.
 *
 * Pinned: only the targeted attacker's damage moves, the shield lasts the whole turn rather than
 * one instance, and it ends if the Night Stalker leaves the battlefield.
 */
class ShimianNightStalkerScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy { cardRegistry.getCard("Shimian Night Stalker")!!.activatedAbilities[0].id }

    private fun attackWithCourserAndLions(): TestGame {
        val game = scenario()
            .withPlayers("Attacker", "Defender")
            .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false)
            .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
            .withCardOnBattlefield(2, "Shimian Night Stalker")
            .withLandsOnBattlefield(2, "Swamp", 1)
            .withCardInHand(1, "Stoke the Flames")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withLifeTotal(2, 20)
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()
        game.declareAttackers(mapOf("Centaur Courser" to 2, "Savannah Lions" to 2)).error shouldBe null
        game.passPriority() // the attacker passes; the defender gets priority in declare attackers
        return game
    }

    private fun TestGame.activateOn(attackerName: String) {
        val stalker = findPermanent("Shimian Night Stalker")!!
        execute(
            ActivateAbility(player2Id, stalker, abilityId, listOf(ChosenTarget.Permanent(findPermanent(attackerName)!!)))
        ).error shouldBe null
        resolveStack()
    }

    init {
        context("Shimian Night Stalker") {
            test("the targeted attacker's combat damage to you is dealt to the Night Stalker; the other attacker's isn't") {
                val game = attackWithCourserAndLions()
                game.activateOn("Centaur Courser")

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                val stalker = game.findPermanent("Shimian Night Stalker")!!
                withClue("only the 1/1 Lions' damage reaches the defender") { game.getLifeTotal(2) shouldBe 19 }
                withClue("the Courser's 3 went to the Night Stalker") {
                    game.state.getEntity(stalker)?.get<DamageComponent>()?.amount shouldBe 3
                }
            }

            test("if the Night Stalker has left the battlefield, the damage is dealt to you normally") {
                val game = attackWithCourserAndLions()
                game.activateOn("Centaur Courser")

                withClue("the attacker kills the Night Stalker before damage") {
                    game.state.priorityPlayerId shouldBe game.player1Id
                    game.castSpell(1, "Stoke the Flames", game.findPermanent("Shimian Night Stalker")!!).error shouldBe null
                    game.resolveStack()
                    game.isInGraveyard(2, "Shimian Night Stalker") shouldBe true
                }
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Courser's 3 and Lions' 1 both reach the defender") { game.getLifeTotal(2) shouldBe 16 }
            }
        }
    }
}
