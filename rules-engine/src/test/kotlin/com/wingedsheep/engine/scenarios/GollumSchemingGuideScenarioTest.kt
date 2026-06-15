package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gollum, Scheming Guide — "Whenever Gollum attacks, look at the top two cards of your library, put
 * them back in any order, then choose land or nonland. An opponent guesses whether the top card of
 * your library is the chosen kind. Reveal that card. If they guessed right, remove Gollum from
 * combat. Otherwise, you draw a card and Gollum can't be blocked this turn."
 *
 * Exercises the new opponent-guess primitive (Gap 38): controller chooses a framing kind, an
 * opponent guesses the actual land/nonland kind of the top card, the card is revealed and compared,
 * and the matching branch resolves. Both outcomes are proven:
 *  - guess matches the actual top kind → Gollum removed from combat;
 *  - guess wrong → controller draws a card and Gollum becomes unblockable this turn.
 */
class GollumSchemingGuideScenarioTest : FunSpec({

    /** Land vs nonland are encoded as option index 0 = Land, 1 = Nonland. */
    val LAND = 0
    val NONLAND = 1

    /**
     * Drives Gollum's attack trigger to the point where the reveal/branch resolves.
     *
     * @param topCardIsLand whether the top card (after reorder) is a land.
     * @param controllerChoiceIndex controller's framing land/nonland pick.
     * @param opponentGuessIndex opponent's guess (this is what's compared to reality).
     */
    data class AttackResult(
        val driver: GameTestDriver,
        val gollum: EntityId,
        val attacker: EntityId,
        val handBefore: Int,
    )

    fun runAttack(
        topCardIsLand: Boolean,
        controllerChoiceIndex: Int,
        opponentGuessIndex: Int
    ): AttackResult {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val gollum = driver.putCreatureOnBattlefield(attacker, "Gollum, Scheming Guide")
        driver.removeSummoningSickness(gollum)

        // Seed the top two cards of the library: one land (Swamp) and one nonland (Grizzly Bears).
        // putCardOnTopOfLibrary prepends, so the LAST call ends up on top before the reorder.
        val land = driver.putCardOnTopOfLibrary(attacker, "Swamp")
        val nonland = driver.putCardOnTopOfLibrary(attacker, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = driver.getHandSize(attacker)
        driver.declareAttackers(attacker, listOf(gollum), defender)
        driver.bothPass()

        // 1. Look at top two, put back in any order: put the desired card on top.
        val reorder = driver.pendingDecision as ReorderLibraryDecision
        val desiredTop = if (topCardIsLand) land else nonland
        val other = if (topCardIsLand) nonland else land
        driver.submitOrderedResponse(attacker, listOf(desiredTop, other))

        // 2. Controller chooses land or nonland (framing).
        val controllerChoice = driver.pendingDecision as ChooseOptionDecision
        controllerChoice.playerId shouldBe attacker
        driver.submitDecision(attacker, OptionChosenResponse(controllerChoice.id, controllerChoiceIndex))

        // 3. Opponent guesses land or nonland about the top card.
        val guess = driver.pendingDecision as ChooseOptionDecision
        guess.playerId shouldBe defender
        driver.submitDecision(defender, OptionChosenResponse(guess.id, opponentGuessIndex))

        return AttackResult(driver, gollum, attacker, handBefore)
    }

    test("opponent guesses right (top is land, guesses land) -> Gollum removed from combat") {
        val r = runAttack(
            topCardIsLand = true,
            controllerChoiceIndex = LAND,
            opponentGuessIndex = LAND
        )

        // Correct guess: Gollum is removed from combat, no draw, not unblockable.
        r.driver.state.getEntity(r.gollum)?.has<AttackingComponent>() shouldBe false
        r.driver.getHandSize(r.attacker) shouldBe r.handBefore
        r.driver.state.projectedState.hasKeyword(r.gollum, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
    }

    test("opponent guesses wrong (top is land, guesses nonland) -> draw a card and Gollum unblockable") {
        val r = runAttack(
            topCardIsLand = true,
            controllerChoiceIndex = LAND,
            opponentGuessIndex = NONLAND
        )

        // Wrong guess: Gollum stays attacking, controller drew a card, Gollum is unblockable this turn.
        r.driver.state.getEntity(r.gollum)?.has<AttackingComponent>() shouldBe true
        r.driver.getHandSize(r.attacker) shouldBe r.handBefore + 1
        r.driver.state.projectedState.hasKeyword(r.gollum, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
    }

    test("opponent guesses right when top is nonland (guesses nonland) -> Gollum removed from combat") {
        val r = runAttack(
            topCardIsLand = false,
            controllerChoiceIndex = NONLAND,
            opponentGuessIndex = NONLAND
        )

        r.driver.state.getEntity(r.gollum)?.has<AttackingComponent>() shouldBe false
        r.driver.getHandSize(r.attacker) shouldBe r.handBefore
        r.driver.state.projectedState.hasKeyword(r.gollum, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
    }
})
