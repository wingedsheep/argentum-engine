package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Abandon the Post (MID #127) — "Up to two target creatures can't block this turn.
 * Flashback {3}{R}"
 */
class AbandonThePostScenarioTest : ScenarioTestBase() {

    init {
        context("Abandon the Post") {

            test("both targeted creatures can't block this turn; an untargeted one still can") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Abandon the Post")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Wall of Wood")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Abandon the Post"
                }
                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        listOf(
                            ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!),
                            ChosenTarget.Permanent(game.findPermanent("Wall of Wood")!!),
                        )
                    )
                ).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Hill Giant was targeted and can't block") {
                    game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
                }
                withClue("Wall of Wood was targeted and can't block") {
                    game.declareBlockers(mapOf("Wall of Wood" to listOf("Grizzly Bears"))).error shouldNotBe null
                }
                withClue("Savannah Lions wasn't targeted and may block") {
                    game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).error shouldBe null
                }
            }

            test("flashback from the graveyard with a single target, then the card is exiled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Abandon the Post")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellFromGraveyard(1, "Abandon the Post", game.findPermanent("Hill Giant")).error shouldBe null
                game.resolveStack()

                withClue("flashback exiles the card on resolution") {
                    game.isInExile(1, "Abandon the Post") shouldBe true
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
            }
        }
    }
}
