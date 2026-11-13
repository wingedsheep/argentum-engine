package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Brutal Expulsion (BFZ #200) — {2}{U}{R} Instant, devoid.
 *
 *   Choose one or both —
 *   • Return target spell or creature to its owner's hand.
 *   • Brutal Expulsion deals 2 damage to target creature or planeswalker. If that creature or
 *     planeswalker would die this turn, exile it instead.
 *
 * Covers what the script's shape decides: both modes together, the exile mark landing on a
 * planeswalker (Touch of the Void deliberately does not mark one), and both modes naming the same
 * creature — the bounce resolves first, so the second mode must not damage or mark the card in
 * hand (CR 400.7).
 */
class BrutalExpulsionScenarioTest : ScenarioTestBase() {

    private fun TestGame.castExpulsion(modes: List<Int>, modeTargets: List<List<ChosenTarget>>) {
        val spell = state.getHand(player1Id).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Brutal Expulsion"
        }
        val cast = execute(
            CastSpell(
                player1Id,
                spell,
                modeTargets.flatten(),
                chosenModes = modes,
                modeTargetsOrdered = modeTargets,
            )
        )
        withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
        if (hasPendingDecision()) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        context("Brutal Expulsion") {

            test("both modes — one creature is bounced, the other dies to the damage and is exiled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Brutal Expulsion")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castExpulsion(
                    modes = listOf(0, 1),
                    modeTargets = listOf(
                        listOf(ChosenTarget.Permanent(courser)),
                        listOf(ChosenTarget.Permanent(bears)),
                    ),
                )

                game.isInHand(2, "Centaur Courser") shouldBe true
                withClue("the Bears died to the 2 damage and were exiled instead") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
            }

            test("a planeswalker it only wounds is exiled when it dies later this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Brutal Expulsion")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Jace, the Mind Sculptor")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, the Mind Sculptor")!!
                game.castExpulsion(
                    modes = listOf(1),
                    modeTargets = listOf(listOf(ChosenTarget.Permanent(jace))),
                )
                withClue("Jace (3 loyalty) survives 2 damage") {
                    game.isOnBattlefield("Jace, the Mind Sculptor") shouldBe true
                }

                game.castSpell(1, "Lightning Bolt", jace).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("a planeswalker dying this turn is exiled too — \"dies\" isn't creature-only") {
                    game.isOnBattlefield("Jace, the Mind Sculptor") shouldBe false
                    game.isInGraveyard(2, "Jace, the Mind Sculptor") shouldBe false
                    game.isInExile(2, "Jace, the Mind Sculptor") shouldBe true
                }
            }

            test("both modes on the same creature — the bounced card is neither damaged nor marked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Brutal Expulsion")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castExpulsion(
                    modes = listOf(0, 1),
                    modeTargets = listOf(
                        listOf(ChosenTarget.Permanent(bears)),
                        listOf(ChosenTarget.Permanent(bears)),
                    ),
                )

                game.isInHand(2, "Grizzly Bears") shouldBe true
                withClue("the card in hand is a new object: no damage and no exile-on-death mark follow it") {
                    game.state.getEntity(bears)?.get<DamageComponent>() shouldBe null
                    game.state.floatingEffects.none {
                        bears in it.effect.affectedEntities &&
                            it.effect.modification is SerializableModification.ExileOnDeath
                    } shouldBe true
                }
            }
        }
    }
}
