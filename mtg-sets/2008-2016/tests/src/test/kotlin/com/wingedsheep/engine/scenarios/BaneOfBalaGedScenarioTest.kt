package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bane of Bala Ged (BFZ #1) — {7} Creature — Eldrazi 7/5.
 *
 *   Whenever this creature attacks, defending player exiles two permanents they control.
 *
 * The defending player — not the attacker — picks the two, from their own permanents only; and with
 * fewer than two permanents, whatever they control is exiled without a choice (ruling).
 */
class BaneOfBalaGedScenarioTest : ScenarioTestBase() {

    init {
        context("Bane of Bala Ged attack trigger") {

            test("defending player chooses two of their own permanents to exile") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bane of Bala Ged", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Siege Mastodon")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Bane of Bala Ged" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the defending player makes the choice") {
                    game.state.pendingDecision shouldNotBe null
                    game.state.pendingDecision?.playerId shouldBe game.player2Id
                }
                game.selectCards(
                    listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Plains")!!)
                ).error shouldBe null
                game.resolveStack()

                withClue("the two chosen permanents are exiled") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInExile(2, "Plains") shouldBe true
                }
                withClue("the unchosen permanent stays") {
                    game.isOnBattlefield("Siege Mastodon") shouldBe true
                }
                withClue("the attacker's own permanents are untouched") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.isOnBattlefield("Bane of Bala Ged") shouldBe true
                }
            }

            test("a defending player with only one permanent exiles it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bane of Bala Ged", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Bane of Bala Ged" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the lone permanent is exiled") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
                game.isOnBattlefield("Bane of Bala Ged") shouldBe true
            }
        }
    }
}
