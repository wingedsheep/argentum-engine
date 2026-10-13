package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jhoira, Ageless Innovator (DMU) — {U}{R} 2/3 Legendary Creature — Human Artificer.
 *
 * "{T}: Put two ingenuity counters on Jhoira, then you may put an artifact card with mana value X
 *  or less from your hand onto the battlefield, where X is the number of ingenuity counters on
 *  Jhoira."
 *
 * Covers: X counts the counters just placed (a fresh Jhoira allows mana value 2, not 3); the "may"
 * decline; and the ruling that a Jhoira removed in response gets no counters but still lets you put
 * an artifact, with X read from the counters she last had on the battlefield.
 */
class JhoiraAgelessInnovatorScenarioTest : ScenarioTestBase() {

    private val artifactMv2 = card("Jhoira Test Gadget Two") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "A test gadget."
    }
    private val artifactMv3 = card("Jhoira Test Gadget Three") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = "A test gadget."
    }

    private val jhoiraName = "Jhoira, Ageless Innovator"

    private fun TestGame.ingenuity(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.INGENUITY) ?: 0

    private fun TestGame.activateJhoira(jhoira: EntityId) {
        val abilityId = cardRegistry.getCard(jhoiraName)!!.activatedAbilities.single().id
        execute(ActivateAbility(playerId = player1Id, sourceId = jhoira, abilityId = abilityId))
            .error shouldBe null
    }

    private fun TestGame.inZone(id: EntityId, zone: Zone): Boolean =
        id in state.getZone(ZoneKey(player1Id, zone))

    init {
        cardRegistry.register(artifactMv2)
        cardRegistry.register(artifactMv3)

        context("Jhoira, Ageless Innovator") {

            test("two counters land first, so X = 2: the mv-2 artifact is offered and the mv-3 one is not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, jhoiraName, summoningSickness = false)
                    .withCardInHand(1, "Jhoira Test Gadget Two")
                    .withCardInHand(1, "Jhoira Test Gadget Three")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jhoira = game.findPermanent(jhoiraName)!!
                val mv2 = game.findCardsInHand(1, "Jhoira Test Gadget Two").single()
                val mv3 = game.findCardsInHand(1, "Jhoira Test Gadget Three").single()

                game.activateJhoira(jhoira)
                game.resolveStack()

                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("only the artifact with mana value <= 2 is eligible") {
                    decision.options shouldBe listOf(mv2)
                    decision.minSelections shouldBe 0
                }
                game.selectCards(listOf(mv2)).error shouldBe null
                game.resolveStack()

                game.ingenuity(jhoira) shouldBe 2
                withClue("the chosen artifact was put onto the battlefield") {
                    game.inZone(mv2, Zone.BATTLEFIELD) shouldBe true
                }
                withClue("the mv-3 artifact stays in hand") {
                    game.inZone(mv3, Zone.HAND) shouldBe true
                }
            }

            test("declining keeps the artifact in hand but the counters stay") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, jhoiraName, summoningSickness = false)
                    .withCardInHand(1, "Jhoira Test Gadget Two")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jhoira = game.findPermanent(jhoiraName)!!
                val mv2 = game.findCardsInHand(1, "Jhoira Test Gadget Two").single()

                game.activateJhoira(jhoira)
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.ingenuity(jhoira) shouldBe 2
                game.inZone(mv2, Zone.HAND) shouldBe true
            }

            test("Jhoira killed in response: no new counters, X uses her last-known count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, jhoiraName, summoningSickness = false)
                    .withCardInHand(1, "Jhoira Test Gadget Two")
                    .withCardInHand(1, "Jhoira Test Gadget Three")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jhoira = game.findPermanent(jhoiraName)!!
                // Two ingenuity counters from an earlier activation.
                game.state = game.state.updateEntity(jhoira) {
                    it.with(CountersComponent().withAdded(CounterType.INGENUITY, 2))
                }
                val mv2 = game.findCardsInHand(1, "Jhoira Test Gadget Two").single()
                val mv3 = game.findCardsInHand(1, "Jhoira Test Gadget Three").single()

                game.activateJhoira(jhoira)
                game.castSpell(1, "Murder", jhoira).error shouldBe null
                game.resolveStack()

                withClue("Jhoira died to Murder before her ability resolved") {
                    game.isOnBattlefield(jhoiraName) shouldBe false
                }
                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("X is her last-known 2 counters — not 0, and not 4 (no counters were added)") {
                    decision.options shouldBe listOf(mv2)
                }
                game.selectCards(listOf(mv2)).error shouldBe null
                game.resolveStack()

                game.inZone(mv2, Zone.BATTLEFIELD) shouldBe true
                game.inZone(mv3, Zone.HAND) shouldBe true
            }
        }
    }
}
