package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Oculus Whelp — 3/2 flier; "As long as you control a transformed permanent, this creature has
 * 'When this creature dies, draw a card.'"
 *
 * Smoldering Werewolf ({4}{R}{R}: transform) supplies the transformed permanent. The granted
 * ability is a leaves-the-battlefield trigger, so it looks back to the state before the death
 * (CR 603.10a) — the printed ruling: dying alongside your transformed permanents still draws.
 */
class OculusWhelpScenarioTest : ScenarioTestBase() {

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    private fun TestGame.transformWerewolf() {
        val wolf = findPermanent("Smoldering Werewolf")!!
        val abilityId = cardRegistry.getCard("Smoldering Werewolf")!!.activatedAbilities.first().id
        execute(ActivateAbility(playerId = player1Id, sourceId = wolf, abilityId = abilityId)).error shouldBe null
        payIfAsked()
        resolveStack()
        findPermanent("Erupting Dreadwolf") shouldBe wolf
    }

    private fun board(spell: String, lands: String, landCount: Int) = scenario()
        .withPlayers("Player", "Opponent")
        // The Werewolf first, so a one-at-a-time move takes it before the Whelp — the simultaneity
        // tests below would pass by iteration order otherwise.
        .withCardOnBattlefield(1, "Smoldering Werewolf", summoningSickness = false)
        .withCardOnBattlefield(1, "Oculus Whelp")
        .withLandsOnBattlefield(1, "Mountain", 6)
        .withLandsOnBattlefield(1, lands, landCount)
        .withCardInHand(1, spell)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Oculus Whelp") {

            test("dies while you control a transformed permanent — draws a card") {
                val game = board("Lightning Bolt", "Mountain", 1)
                game.transformWerewolf()
                val hand = game.handSize(1)

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Oculus Whelp")!!).error shouldBe null
                game.payIfAsked()
                game.resolveStack()

                game.isInGraveyard(1, "Oculus Whelp") shouldBe true
                withClue("Bolt left the hand (-1), the granted dies trigger drew (+1)") {
                    game.handSize(1) shouldBe hand
                }
            }

            test("dies with only a front-face double-faced permanent — no draw") {
                val game = board("Lightning Bolt", "Mountain", 1)
                val hand = game.handSize(1)

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Oculus Whelp")!!).error shouldBe null
                game.payIfAsked()
                game.resolveStack()

                game.isInGraveyard(1, "Oculus Whelp") shouldBe true
                withClue("a front-face DFC is not a transformed permanent, so no ability was granted") {
                    game.handSize(1) shouldBe hand - 1
                }
            }

            test("destroyed alongside the transformed permanent by one effect — still draws (CR 603.10a)") {
                val game = board("Wrath of God", "Plains", 4)
                game.transformWerewolf()
                val hand = game.handSize(1)

                game.castSpell(1, "Wrath of God").error shouldBe null
                game.payIfAsked()
                game.resolveStack()

                game.isInGraveyard(1, "Oculus Whelp") shouldBe true
                game.findPermanent("Erupting Dreadwolf") shouldBe null
                withClue("the look-back sees the Dreadwolf still transformed on the battlefield") {
                    game.handSize(1) shouldBe hand
                }
            }

            test("dies to state-based actions alongside the transformed permanent — still draws") {
                val game = board("Languish", "Swamp", 4)
                game.transformWerewolf()
                val hand = game.handSize(1)

                game.castSpell(1, "Languish").error shouldBe null
                game.payIfAsked()
                game.resolveStack()

                game.isInGraveyard(1, "Oculus Whelp") shouldBe true
                game.findPermanent("Erupting Dreadwolf") shouldBe null
                withClue("one SBA pass is one simultaneous event; its deaths look back to the pass start") {
                    game.handSize(1) shouldBe hand
                }
            }
        }
    }
}
