package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * To the Slaughter (SOI #139) — {2}{B} Instant.
 *
 *   Target player sacrifices a creature or planeswalker of their choice.
 *   Delirium — If there are four or more card types among cards in your graveyard, instead that
 *   player sacrifices a creature and a planeswalker of their choice.
 *
 * Covers the plain edict, the delirium pair (picked one at a time, sacrificed together), a board
 * with no planeswalker losing just one creature, and the ruling that a creature planeswalker can't
 * answer both halves when another creature is there to be the other one.
 */
class ToTheSlaughterScenarioTest : ScenarioTestBase() {

    init {
        context("To the Slaughter") {

            test("without delirium the target player sacrifices one creature or planeswalker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "To the Slaughter")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Liliana Vess")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana Vess")!!

                game.castSpellTargetingPlayer(1, "To the Slaughter", 2).error shouldBe null
                game.resolveStack()

                game.getPendingDecision()!!.playerId shouldBe game.player2Id
                game.selectCards(listOf(liliana)).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(2, "Liliana Vess") shouldBe true
                game.findPermanents("Grizzly Bears").size shouldBe 1
            }

            test("with delirium the target player sacrifices a creature and a planeswalker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "To the Slaughter")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Liliana Vess")
                    .withCardOnBattlefield(2, "Jace Beleren")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val jace = game.findPermanent("Jace Beleren")!!

                game.castSpellTargetingPlayer(1, "To the Slaughter", 2).error shouldBe null
                game.resolveStack()

                withClue("the target player makes the first pick") {
                    val decision = game.getPendingDecision()!!
                    decision.playerId shouldBe game.player2Id
                    decision.prompt shouldContain "to sacrifice"
                }
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("nothing is sacrificed until both picks are in") {
                    game.state.getBattlefield().contains(bears) shouldBe true
                }
                withClue("a creature was picked, so the second pick is a planeswalker") {
                    val decision = game.getPendingDecision()!!
                    decision.playerId shouldBe game.player2Id
                    decision.prompt shouldContain "planeswalker to sacrifice"
                }
                game.selectCards(listOf(jace)).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Jace Beleren") shouldBe true
                game.findPermanents("Hill Giant").size shouldBe 1
                game.findPermanents("Liliana Vess").size shouldBe 1
            }

            test("with delirium a player with no planeswalker sacrifices only one creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "To the Slaughter")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Lava Spike")
                    .withCardInGraveyard(1, "Ornithopter")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpellTargetingPlayer(1, "To the Slaughter", 2).error shouldBe null
                game.resolveStack()

                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.findPermanents("Hill Giant").size shouldBe 1
            }

            test("a creature planeswalker picked first still costs its controller another creature") {
                // Gideon Blackblade is a creature (that's still a planeswalker) on his controller's turn.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "To the Slaughter")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Lava Spike")
                    .withCardInGraveyard(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Gideon Blackblade")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gideon = game.findPermanent("Gideon Blackblade")!!
                game.state.projectedState.isCreature(gideon) shouldBe true

                game.castSpellTargetingPlayer(1, "To the Slaughter", 1).error shouldBe null
                game.resolveStack()

                game.getPendingDecision()!!.playerId shouldBe game.player1Id
                game.selectCards(listOf(gideon)).error shouldBe null
                game.resolveStack()

                withClue("Gideon can't be both halves, so Hill Giant goes too") {
                    game.hasPendingDecision() shouldBe false
                    game.isInGraveyard(1, "Gideon Blackblade") shouldBe true
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
            }
        }
    }
}
