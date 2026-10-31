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
 * Mythos of Snapdax (IKO #24) — {2}{W}{W} Sorcery.
 *
 * "Each player chooses an artifact, a creature, an enchantment, and a planeswalker from among the
 *  nonland permanents they control, then sacrifices the rest. If {B}{R} was spent to cast this
 *  spell, you choose the permanents for each player instead."
 *
 * Player 1 controls Ornithopter (artifact creature), Grizzly Bears and Glorious Anthem. Ornithopter
 * is the only artifact, so it's kept as the artifact without a prompt and is still offered as the
 * creature. Player 2 controls Sol Ring, Seat of the Synod (an artifact land — never a candidate,
 * never sacrificed), Hill Giant and Centaur Courser; only the creature choices need a prompt, and
 * the {B}{R} bonus decides who answers them. (The Seat sits with player 2 so it can't be tapped to
 * pay for the Mythos and muddy which colours were spent.)
 */
class MythosOfSnapdaxScenarioTest : ScenarioTestBase() {

    private fun scenarioWith(vararg lands: Pair<String, Int>) = scenario()
        .withPlayers()
        .withCardInHand(1, "Mythos of Snapdax")
        .withCardOnBattlefield(1, "Ornithopter")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Glorious Anthem")
        .withCardOnBattlefield(2, "Sol Ring")
        .withCardOnBattlefield(2, "Seat of the Synod")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Centaur Courser")
        .apply { lands.forEach { (land, n) -> withLandsOnBattlefield(1, land, n) } }
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("each player keeps one of each type of their own choosing and sacrifices the rest") {
            val game = scenarioWith("Plains" to 4)
            val ornithopter = game.findPermanent("Ornithopter")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val courser = game.findPermanent("Centaur Courser")!!

            game.castSpell(1, "Mythos of Snapdax").error shouldBe null
            game.resolveStack()

            withClue("you choose first: your creature, with the artifact creature still offered") {
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                decision.playerId shouldBe game.player1Id
                decision.options shouldContainExactlyInAnyOrder listOf(ornithopter, bears)
            }
            game.selectCards(listOf(ornithopter))

            withClue("then the opponent picks their own creature") {
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                decision.playerId shouldBe game.player2Id
                decision.options shouldContainExactlyInAnyOrder listOf(giant, courser)
            }
            game.selectCards(listOf(courser))
            game.resolveStack()

            withClue("the unchosen creatures are sacrificed") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
            withClue("the artifact creature survives as both picks; the artifact land is untouched") {
                listOf("Ornithopter", "Glorious Anthem", "Seat of the Synod", "Sol Ring", "Centaur Courser")
                    .forEach { withClue(it) { game.isOnBattlefield(it) shouldBe true } }
            }
        }

        test("with {B}{R} spent, you choose the permanents for each player") {
            val game = scenarioWith("Plains" to 2, "Swamp" to 1, "Mountain" to 1)
            val ornithopter = game.findPermanent("Ornithopter")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val courser = game.findPermanent("Centaur Courser")!!

            game.castSpell(1, "Mythos of Snapdax").error shouldBe null
            game.resolveStack()

            withClue("you choose your own creature") {
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                decision.playerId shouldBe game.player1Id
                decision.options shouldContainExactlyInAnyOrder listOf(ornithopter, bears)
            }
            game.selectCards(listOf(bears))

            withClue("and you also choose the opponent's creature") {
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                decision.playerId shouldBe game.player1Id
                decision.options shouldContainExactlyInAnyOrder listOf(giant, courser)
            }
            game.selectCards(listOf(giant))
            game.resolveStack()

            withClue("Ornithopter stays as the artifact even though Bears was the creature pick") {
                game.isOnBattlefield("Ornithopter") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
            withClue("the opponent's unchosen creature is sacrificed") {
                game.isInGraveyard(2, "Centaur Courser") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.isOnBattlefield("Sol Ring") shouldBe true
            }
        }
    }
}
