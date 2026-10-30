package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.numberChoice
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Haktos the Unscarred (THB #218).
 *
 * "Haktos attacks each combat if able. / As Haktos enters, choose 2, 3, or 4 at random. / Haktos
 *  has protection from each mana value other than the chosen number."
 *
 * The number is drawn, not asked: nobody gets a decision, on the cast path or when Haktos is put
 * onto the battlefield by an effect. Blaze's X is set from the drawn number so the spell of exactly
 * that mana value can target it (ruling: an X spell uses its chosen X), while Lightning Bolt
 * (mana value 1) never can, and a mana-value-1 creature can't block it.
 */
class HaktosTheUnscarredScenarioTest : ScenarioTestBase() {

    private val haktos = "Haktos the Unscarred"

    private fun TestGame.chosenNumber(id: EntityId): Int =
        state.getEntity(id)?.numberChoice(ChoiceSlot.CHOSEN_NUMBER) ?: error("Haktos has no number")

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, haktos)
        .withCardInHand(1, "Lightning Bolt")
        .withCardInHand(1, "Blaze")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withLandsOnBattlefield(1, "Mountain", 7)
        .withCardOnBattlefield(2, "Savannah Lions", summoningSickness = false)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("cast: the number is drawn with no decision, and only a spell of that mana value can target it") {
            val game = board().build()
            game.castSpell(1, haktos).error shouldBe null
            game.resolveStack()
            withClue("no player chooses the number") { game.getPendingDecision() shouldBe null }
            val id = game.findPermanent(haktos) ?: error("Haktos didn't enter")
            val number = game.chosenNumber(id)
            number shouldBeIn listOf(2, 3, 4)

            withClue("Lightning Bolt (mana value 1) is never the number") {
                game.castSpell(1, "Lightning Bolt", id).error shouldNotBe null
            }
            withClue("Blaze with X=${number - 1} has mana value $number") {
                game.castXSpell(1, "Blaze", number - 1, id).error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield(haktos) shouldBe false
        }

        test("put onto the battlefield by an effect: the number is drawn there too") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, haktos)
                .withCardInHand(1, "Zombify")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val card = game.findCardsInGraveyard(1, haktos).single()
            game.castSpellTargetingGraveyardCard(1, "Zombify", listOf(card)).error shouldBe null
            game.resolveStack()
            game.getPendingDecision() shouldBe null
            val id = game.findPermanent(haktos) ?: error("Haktos didn't return")
            game.chosenNumber(id) shouldBeIn listOf(2, 3, 4)
        }

        test("a Clone copying Haktos draws its own number, with no decision") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, haktos)
                .withCardInHand(1, "Clone")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Clone").error shouldBe null
            game.resolveStack()
            // Clone's "you may have it enter as a copy" — pick the opponent's Haktos.
            game.selectCards(listOf(game.findPermanents(haktos).single())).error shouldBe null
            game.resolveStack()
            game.getPendingDecision() shouldBe null
            val copy = game.findPermanents(haktos).single { game.state.getEntity(it)?.let { e ->
                e.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId == game.player1Id } == true }
            game.chosenNumber(copy) shouldBeIn listOf(2, 3, 4)
        }

        test("it attacks each combat, and a creature of another mana value can't block it") {
            val game = board().build()
            game.castSpell(1, haktos).error shouldBe null
            game.resolveStack()

            // Next turn of ours: Haktos is no longer summoning sick and must attack.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.state.activePlayerId shouldBe game.player1Id
            withClue("attacks each combat if able") {
                game.declareAttackers(emptyMap()).error shouldNotBe null
            }
            game.declareAttackers(mapOf(haktos to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Savannah Lions (mana value 1) can't block it") {
                game.declareBlockers(mapOf("Savannah Lions" to listOf(haktos))).error shouldNotBe null
            }
        }
    }
}
