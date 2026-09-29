package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Crawling Chorus (ONE #8) — {W} 1/1 Phyrexian Horror, toxic 1.
 *
 * "When this creature dies, create a 1/1 colorless Phyrexian Mite artifact creature token with
 *  toxic 1 and 'This token can't block.'"
 *
 * Proof card for the shared `PredefinedTokens.PhyrexianMite`: the Mite is a predefined token, so
 * its printed toxic 1 has to ride onto the minted token the way a card's does, and its "can't
 * block" has to be the token's own static ability.
 */
class CrawlingChorusScenarioTest : ScenarioTestBase() {

    private fun poison(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    init {
        context("the dies trigger") {
            test("creates a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Crawling Chorus")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chorus = game.findPermanent("Crawling Chorus")!!
                game.castSpell(1, "Shock", chorus).error shouldBe null
                game.resolveStack()

                game.findPermanent("Crawling Chorus") shouldBe null
                val mite = game.findPermanent("Phyrexian Mite")
                withClue("the dies trigger minted a Mite") { mite shouldNotBe null }
                val miteId = mite!!

                val projected = game.state.projectedState
                withClue("a 1/1 colorless Phyrexian Mite artifact creature token") {
                    game.state.getEntity(miteId)!!.has<TokenComponent>() shouldBe true
                    projected.getController(miteId) shouldBe game.player1Id
                    projected.getPower(miteId) shouldBe 1
                    projected.getToughness(miteId) shouldBe 1
                    projected.isCreature(miteId) shouldBe true
                    projected.hasType(miteId, "ARTIFACT") shouldBe true
                    projected.hasSubtype(miteId, "Phyrexian") shouldBe true
                    projected.hasSubtype(miteId, "Mite") shouldBe true
                    projected.getColors(miteId) shouldBe emptySet()
                }
                withClue("its printed toxic 1 survived the predefined-token path") {
                    projected.getKeywords(miteId).contains("TOXIC_1") shouldBe true
                    projected.hasKeyword(miteId, Keyword.TOXIC) shouldBe true
                }
            }
        }

        context("the Phyrexian Mite token") {
            test("its combat damage gives the defending player a poison counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Phyrexian Mite", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Phyrexian Mite" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.getLifeTotal(2) shouldBe 19
                poison(game, game.player2Id) shouldBe 1
            }

            test("can't block") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Crawling Chorus")
                    .withCardOnBattlefield(2, "Phyrexian Mite", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Crawling Chorus" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Phyrexian Mite" to listOf("Crawling Chorus"))).error shouldNotBe null
            }
        }
    }
}
