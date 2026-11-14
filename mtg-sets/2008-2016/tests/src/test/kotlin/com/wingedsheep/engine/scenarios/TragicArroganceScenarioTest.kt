package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tragic Arrogance (ORI #38) — {3}{W}{W} Sorcery.
 *
 * "For each player, you choose from among the permanents that player controls an artifact, a
 *  creature, an enchantment, and a planeswalker. Then each player sacrifices all other nonland
 *  permanents they control."
 *
 * Unlike Mythos of Snapdax ("from among the nonland permanents"), the choice is from among *all*
 * the permanents a player controls, so an artifact land can fill the artifact slot — sparing nothing
 * extra, since lands are never sacrificed, but leaving the player's other artifact to die. The caster
 * makes every choice, the opponent's included.
 */
class TragicArroganceScenarioTest : ScenarioTestBase() {

    init {
        test("you choose for the opponent; an artifact land can be the artifact, and lands survive") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Tragic Arrogance")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(2, "Seat of the Synod")
                .withCardOnBattlefield(2, "Sol Ring")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val seat = game.findPermanent("Seat of the Synod")!!
            val solRing = game.findPermanent("Sol Ring")!!
            val giant = game.findPermanent("Hill Giant")!!
            val courser = game.findPermanent("Centaur Courser")!!

            game.castSpell(1, "Tragic Arrogance").error shouldBe null
            game.resolveStack()

            withClue("you choose the opponent's artifact, with the artifact land offered") {
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                decision.playerId shouldBe game.player1Id
                decision.options shouldContainExactlyInAnyOrder listOf(seat, solRing)
            }
            game.selectCards(listOf(seat)).error shouldBe null

            withClue("and the opponent's creature") {
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                decision.playerId shouldBe game.player1Id
                decision.options shouldContainExactlyInAnyOrder listOf(giant, courser)
            }
            game.selectCards(listOf(courser)).error shouldBe null
            game.resolveStack()

            withClue("the unchosen nonland permanents are sacrificed") {
                game.isInGraveyard(2, "Sol Ring") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
            withClue("the chosen creature and every land stay") {
                listOf("Seat of the Synod", "Centaur Courser", "Plains")
                    .forEach { withClue(it) { game.isOnBattlefield(it) shouldBe true } }
            }
        }
    }
}
