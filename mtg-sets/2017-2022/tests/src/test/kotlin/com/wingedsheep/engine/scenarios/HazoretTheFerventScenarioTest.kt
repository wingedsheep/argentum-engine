package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hazoret the Fervent (AKH #136) — {3}{R} 5/4 Legendary Creature — God.
 *
 *   Indestructible, haste
 *   Hazoret can't attack or block unless you have one or fewer cards in hand.
 *   {2}{R}, Discard a card: Hazoret deals 2 damage to each opponent.
 *
 * Proves the hand-size gate reads the *controller's* hand at declaration time, that the discard
 * cost itself can open the gate, and that blocking is gated too.
 */
class HazoretTheFerventScenarioTest : ScenarioTestBase() {

    init {
        context("Hazoret the Fervent") {

            test("cannot attack with two cards in hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hazoret the Fervent", summoningSickness = false)
                    .withCardsInHand(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                game.declareAttackers(mapOf("Hazoret the Fervent" to 2)).error shouldNotBe null
            }

            test("discarding to its ability deals 2 to each opponent and opens the attack gate") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hazoret the Fervent", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardsInHand(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hazoret = game.findPermanent("Hazoret the Fervent")!!
                val abilityId = cardRegistry.getCard("Hazoret the Fervent")!!
                    .script.activatedAbilities[0].id
                val toDiscard = game.findCardsInHand(1, "Mountain").first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hazoret,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 20
                game.handSize(1) shouldBe 1
                game.findCardsInGraveyard(1, "Mountain").size shouldBe 1

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                withClue("one card in hand satisfies 'one or fewer'") {
                    game.declareAttackers(mapOf("Hazoret the Fervent" to 2)).error shouldBe null
                }
            }

            test("blocking is gated on its controller's hand, not the attacker's") {
                val blocked = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Hazoret the Fervent", summoningSickness = false)
                    .withCardsInHand(2, "Mountain", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                blocked.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                blocked.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                blocked.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                blocked.declareBlockers(mapOf("Hazoret the Fervent" to listOf("Grizzly Bears"))).error shouldNotBe null

                val allowed = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Hazoret the Fervent", summoningSickness = false)
                    .withCardsInHand(2, "Mountain", 1)
                    .withCardsInHand(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                allowed.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                allowed.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                allowed.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                allowed.declareBlockers(mapOf("Hazoret the Fervent" to listOf("Grizzly Bears"))).error shouldBe null
            }
        }
    }
}
