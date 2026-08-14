package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Talion, the Kindly Lord (WOE #215).
 *
 * {2}{U}{B} Legendary Creature — Faerie Noble, 3/4, flying.
 * "As Talion enters, choose a number between 1 and 10."
 * "Whenever an opponent casts a spell with mana value, power, or toughness equal to the chosen
 *  number, that player loses 2 life and you draw a card."
 *
 * Exercises the three new dynamic equality predicates (`ManaValueEqualsDynamic`,
 * `PowerEqualsDynamic`, `ToughnessEqualsDynamic`) reading the as-enters choice through
 * `DynamicAmount.CastChoice(ChoiceSlot.CHOSEN_NUMBER)`. Each characteristic gets its own hit, plus
 * a miss, plus the "opponents only" scope.
 *
 * Each hit is isolated to one characteristic so a bug in any single predicate is visible: Lightning
 * Bolt `{R}` (mana value 1, no power/toughness at all), Juggernaut `{4}` 5/3 (only its power is 5),
 * Wall of Wood `{G}` 0/3 (only its toughness is 3), and Grizzly Bears `{1}{G}` 2/2 for the
 * all-three-at-once and the miss.
 *
 * Talion is cast rather than placed, because the number choice is an *as-enters* replacement — a
 * permanent dropped straight onto the battlefield never runs it and would read the number as unset.
 */
class TalionTheKindlyLordScenarioTest : ScenarioTestBase() {

    init {
        /**
         * Player 2 casts Talion (choosing [number]) on their turn, then the turn passes so player 1
         * — Talion's opponent — is active and can cast into the trigger.
         */
        fun talionInPlay(number: Int, opponentHand: List<String>): TestGame {
            val builder = scenario()
                .withPlayers("Caster", "TalionPlayer")
                .withCardInHand(2, "Talion, the Kindly Lord")
                .withLandsOnBattlefield(2, "Island", 3)
                .withLandsOnBattlefield(2, "Swamp", 1)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            opponentHand.forEach { builder.withCardInHand(1, it) }
            // Stock both libraries so no one decks across the turn change.
            repeat(20) {
                builder.withCardInLibrary(1, "Forest")
                builder.withCardInLibrary(2, "Island")
            }
            val game = builder.build()

            game.castSpell(2, "Talion, the Kindly Lord").error shouldBe null
            game.resolveStack()
            withClue("entering Talion prompts the number choice") {
                game.hasPendingDecision() shouldBe true
            }
            game.chooseNumber(number)
            game.resolveStack()
            withClue("Talion resolved onto the battlefield") {
                game.isOnBattlefield("Talion, the Kindly Lord") shouldBe true
            }

            // Hand the turn to player 1, Talion's opponent. We are already in player 2's precombat
            // main, so step through upkeeps until the active player flips, then settle in the main
            // phase (the Shapeshifter-test idiom for crossing a turn boundary).
            var guard = 0
            while (guard++ < 8) {
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                if (game.state.activePlayerId == game.player1Id) break
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            }
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()
            withClue("player 1 is now the active player") {
                game.state.activePlayerId shouldBe game.player1Id
            }
            return game
        }

        fun life(game: TestGame, playerId: com.wingedsheep.sdk.model.EntityId): Int =
            game.state.getEntity(playerId)!!.get<LifeTotalComponent>()!!.life

        /**
         * Have the opponent cast [cardName] and assert whether Talion's ability fired: on a hit the
         * caster loses 2 life and Talion's controller draws, on a miss neither happens.
         */
        fun castAndExpect(game: TestGame, cardName: String, expectTrigger: Boolean) {
            val casterLifeBefore = life(game, game.player1Id)
            val talionHandBefore = game.handSize(2)

            game.castSpell(1, cardName).error shouldBe null
            game.resolveStack()

            val expectedLife = if (expectTrigger) casterLifeBefore - 2 else casterLifeBefore
            val expectedHand = if (expectTrigger) talionHandBefore + 1 else talionHandBefore
            withClue("caster's life after casting $cardName") {
                life(game, game.player1Id) shouldBe expectedLife
            }
            withClue("Talion controller's hand after $cardName") {
                game.handSize(2) shouldBe expectedHand
            }
        }

        context("Talion, the Kindly Lord") {

            test("mana value alone equal to the chosen number triggers it") {
                // Wall of Wood is {G} 0/3: with 1 chosen, power (0) and toughness (3) both miss,
                // so only ManaValueEqualsDynamic can carry the trigger.
                val game = talionInPlay(number = 1, opponentHand = listOf("Wall of Wood"))
                castAndExpect(game, "Wall of Wood", expectTrigger = true)
            }

            test("a noncreature spell is judged on mana value alone") {
                // Lightning Bolt is {R} with no power or toughness at all: the two P/T predicates
                // must answer false rather than treating a missing characteristic as 0.
                val game = talionInPlay(number = 1, opponentHand = listOf("Lightning Bolt"))
                val casterLifeBefore = life(game, game.player1Id)
                val talionHandBefore = game.handSize(2)

                // Bolt has to have a target; point it at Talion's controller so the caster's own
                // life total stays a clean read of the drain.
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                life(game, game.player1Id) shouldBe casterLifeBefore - 2
                game.handSize(2) shouldBe talionHandBefore + 1
            }

            test("a noncreature spell whose mana value misses does nothing") {
                val game = talionInPlay(number = 4, opponentHand = listOf("Lightning Bolt"))
                val casterLifeBefore = life(game, game.player1Id)
                val talionHandBefore = game.handSize(2)

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                life(game, game.player1Id) shouldBe casterLifeBefore
                game.handSize(2) shouldBe talionHandBefore
            }

            test("power alone equal to the chosen number triggers it") {
                // Juggernaut is {4} 5/3: with 5 chosen, mana value (4) and toughness (3) both miss,
                // so only PowerEqualsDynamic can carry the trigger.
                val game = talionInPlay(number = 5, opponentHand = listOf("Juggernaut"))
                castAndExpect(game, "Juggernaut", expectTrigger = true)
            }

            test("toughness alone equal to the chosen number triggers it") {
                // Wall of Wood is {G} 0/3: with 3 chosen, mana value (1) and power (0) both miss,
                // so only ToughnessEqualsDynamic can carry the trigger.
                val game = talionInPlay(number = 3, opponentHand = listOf("Wall of Wood"))
                castAndExpect(game, "Wall of Wood", expectTrigger = true)
            }

            test("matching on several characteristics at once still triggers only once") {
                // Grizzly Bears {1}{G} 2/2 — mana value, power and toughness are all 2, but the
                // ability is one trigger on one spell-cast event, not three.
                val game = talionInPlay(number = 2, opponentHand = listOf("Grizzly Bears"))
                castAndExpect(game, "Grizzly Bears", expectTrigger = true)
            }

            test("a spell matching none of the three characteristics does nothing") {
                val game = talionInPlay(number = 7, opponentHand = listOf("Grizzly Bears"))
                castAndExpect(game, "Grizzly Bears", expectTrigger = false)
            }

            test("Talion's own controller casting a matching spell does not trigger it") {
                val builder = scenario()
                    .withPlayers("TalionPlayer", "Opponent")
                    .withCardInHand(1, "Talion, the Kindly Lord")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(20) {
                    builder.withCardInLibrary(1, "Forest")
                    builder.withCardInLibrary(2, "Island")
                }
                val game = builder.build()

                game.castSpell(1, "Talion, the Kindly Lord").error shouldBe null
                game.resolveStack()
                game.chooseNumber(2)
                game.resolveStack()

                val lifeBefore = life(game, game.player1Id)
                val handBefore = game.handSize(1)

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("no self-drain") { life(game, game.player1Id) shouldBe lifeBefore }
                withClue("no draw — only the Bears left hand") {
                    game.handSize(1) shouldBe handBefore - 1
                }
            }
        }
    }
}
