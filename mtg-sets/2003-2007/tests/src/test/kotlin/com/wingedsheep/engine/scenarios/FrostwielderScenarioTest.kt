package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.Frostwielder
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Frostwielder (CHK #167) — "{T}: This creature deals 1 damage to any target. If a creature dealt
 * damage by this creature this turn would die, exile it instead."
 *
 * The replacement reaches a creature that dies to *another* source later in the turn, and — per the
 * card's ruling — only while Frostwielder is still on the battlefield.
 */
class FrostwielderScenarioTest : ScenarioTestBase() {

    private val pingAbility = Frostwielder.activatedAbilities.single().id

    init {
        fun base() = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Frostwielder")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.ping() {
            execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = findPermanent("Frostwielder")!!,
                    abilityId = pingAbility,
                    targets = listOf(ChosenTarget.Permanent(findPermanent("Grizzly Bears")!!)),
                )
            ).error shouldBe null
            resolveStack()
        }

        context("Frostwielder") {
            test("a creature it pinged earlier is exiled when another source kills it") {
                val game = base().withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1).build()

                game.ping()
                withClue("1 damage doesn't kill a 2/2") { game.isOnBattlefield("Grizzly Bears") shouldBe true }

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            }

            test("a creature it never damaged dies normally") {
                val game = base().withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1).build()

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("once Frostwielder has left the battlefield the replacement no longer applies") {
                val game = base().withCardsInHand(1, "Shock", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2).build()

                game.ping()
                game.castSpell(1, "Shock", game.findPermanent("Frostwielder")!!).error shouldBe null
                game.resolveStack()
                withClue("Frostwielder died") { game.isInGraveyard(1, "Frostwielder") shouldBe true }

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }
    }
}
