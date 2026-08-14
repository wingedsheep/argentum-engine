package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The Black Arrow (HOB #171) — {3} Legendary Artifact — Equipment.
 *
 * "Flash. When The Black Arrow enters, it deals 1 damage to any target. If a Dragon is dealt damage
 * this way, destroy it. Equipped creature gets +1/+1 and has reach. Equip {1}."
 *
 * The whole card hangs on the Dragon rider: 1 damage is nothing to a 5/5 flier, so the destroy has
 * to fire off the *type* of the damaged permanent, not off lethal damage. These tests pin both
 * sides of that gate — a Dragon dies, a same-sized non-Dragon shrugs it off.
 */
class TheBlackArrowScenarioTest : ScenarioTestBase() {

    init {
        context("The Black Arrow ETB") {

            test("a Dragon dealt the 1 damage is destroyed regardless of its toughness") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Black Arrow")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Shivan Dragon") // 5/5 — survives 1 damage on its own
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "The Black Arrow")
                withClue("Casting The Black Arrow should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack() // Equipment enters → ETB trigger asks for its "any target"

                val dragon = game.findPermanent("Shivan Dragon")!!
                val targeted = game.selectTargets(listOf(dragon))
                withClue("Targeting the Dragon should be legal: ${targeted.error}") { targeted.error shouldBe null }
                game.resolveStack()

                withClue("1 damage is not lethal to a 5/5, but the Dragon rider destroys it anyway") {
                    game.isOnBattlefield("Shivan Dragon") shouldBe false
                    game.isInGraveyard(2, "Shivan Dragon") shouldBe true
                }
            }

            test("a non-Dragon dealt the 1 damage survives") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Black Arrow")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3 — not a Dragon
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "The Black Arrow")
                withClue("Casting The Black Arrow should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                val targeted = game.selectTargets(listOf(giant))
                withClue("Targeting the Hill Giant should be legal: ${targeted.error}") { targeted.error shouldBe null }
                game.resolveStack()

                withClue("Not a Dragon, so only the 1 damage lands — the 3/3 lives") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }

            test("a player dealt the 1 damage just loses a life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Black Arrow")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "The Black Arrow").error shouldBe null
                game.resolveStack()

                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("A player target takes 1 damage and never trips the Dragon rider") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }
        }

        context("The Black Arrow equipped bonus") {

            test("equipped creature gets +1/+1 and reach") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Black Arrow")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2, no reach
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val arrow = game.findPermanent("The Black Arrow")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val equip = cardRegistry.requireCard("The Black Arrow").activatedAbilities.single().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = arrow,
                        abilityId = equip,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("Equipped creature gets +1/+1") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
                withClue("Equipped creature has reach") {
                    game.state.projectedState.hasKeyword(bears, Keyword.REACH) shouldBe true
                }
            }
        }
    }
}
