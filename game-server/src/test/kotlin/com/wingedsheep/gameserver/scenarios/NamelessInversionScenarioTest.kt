package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Nameless Inversion.
 *
 * Card reference:
 * - Nameless Inversion ({1}{B}): Kindred Instant — Shapeshifter
 *   Changeling (This card is every creature type.)
 *   Target creature gets +3/-3 and loses all creature types until end of turn.
 */
class NamelessInversionScenarioTest : ScenarioTestBase() {

    private val toughCreature = CardDefinition.creature(
        name = "Tough Golem",
        manaCost = ManaCost.parse("{3}"),
        subtypes = setOf(Subtype("Golem")),
        power = 2, toughness = 5
    )

    init {
        cardRegistry.register(toughCreature)

        context("Nameless Inversion") {

            test("kills creature with toughness 3 or less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Nameless Inversion")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Glory Seeker")!!

                val castResult = game.castSpell(1, "Nameless Inversion", targetId = target)
                castResult.error shouldBe null

                game.resolveStack()

                // Glory Seeker becomes 5/-1 → dies from 0 or less toughness
                game.isOnBattlefield("Glory Seeker") shouldBe false
            }

            test("creature with high toughness survives with +3/-3 and loses all creature types") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Nameless Inversion")
                    .withCardOnBattlefield(2, "Tough Golem") // 2/5
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Tough Golem")!!

                val castResult = game.castSpell(1, "Nameless Inversion", targetId = target)
                castResult.error shouldBe null

                game.resolveStack()

                // Tough Golem should be 5/2 and survive
                game.isOnBattlefield("Tough Golem") shouldBe true
                val clientState = game.getClientState(1)
                val golemInfo = clientState.cards[target]
                golemInfo shouldNotBe null
                golemInfo!!.power shouldBe 5
                golemInfo.toughness shouldBe 2

                // Should have lost all creature types
                golemInfo.subtypes shouldBe emptySet()
            }
        }
    }
}
