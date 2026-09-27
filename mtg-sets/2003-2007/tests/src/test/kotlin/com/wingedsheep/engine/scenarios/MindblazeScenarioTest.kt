package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.Mindblaze
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mindblaze (CHK #180) — "Choose a nonland card name and a number greater than 0. Target player
 * reveals their library. If that library contains exactly the chosen number of cards with the
 * chosen name, Mindblaze deals 8 damage to that player. Then that player shuffles."
 */
class MindblazeScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Mindblaze)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Cast at [target], choose [number], then name [name]; returns whether the target shuffled. */
    fun GameTestDriver.castAndChoose(target: EntityId, number: Int, name: String): Boolean {
        giveMana(player1, Color.RED, 6)
        val spell = putCardInHand(player1, "Mindblaze")
        val cast = castSpellWithTargets(player1, spell, listOf(ChosenTarget.Player(target)))
        withClue(cast.error ?: "casting Mindblaze failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()

        val numberDecision = pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        withClue("the caster chooses the number") { numberDecision.playerId shouldBe player1 }
        withClue("the number must be greater than 0") { numberDecision.minValue shouldBe 1 }
        submitDecision(player1, NumberChosenResponse(numberDecision.id, number))

        val nameDecision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("the caster names the card") { nameDecision.playerId shouldBe player1 }
        nameDecision.options shouldNotContain "Mountain"
        val result = submitDecision(player1, OptionChosenResponse(nameDecision.id, nameDecision.options.indexOf(name)))
        return result.events.any { it is LibraryShuffledEvent && it.playerId == target }
    }

    test("deals 8 damage when the library holds exactly the chosen number of named cards") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        d.putCardOnTopOfLibrary(d.player2, "Savannah Lions")

        val shuffled = d.castAndChoose(d.player2, number = 2, name = "Centaur Courser")

        d.getLifeTotal(d.player2) shouldBe 12
        withClue("that player shuffles") { shuffled shouldBe true }
        d.getGraveyardCardNames(d.player1) shouldContain "Mindblaze"
    }

    test("no damage when the count differs from the chosen number, but the library is still shuffled") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")

        val shuffled = d.castAndChoose(d.player2, number = 1, name = "Centaur Courser")

        d.getLifeTotal(d.player2) shouldBe 20
        withClue("that player shuffles regardless") { shuffled shouldBe true }
    }

    test("no damage when the named card is absent from the library") {
        val d = driver()
        d.putCardInHand(d.player2, "Centaur Courser")

        d.castAndChoose(d.player2, number = 1, name = "Centaur Courser")

        withClue("only the library counts, not the hand") { d.getLifeTotal(d.player2) shouldBe 20 }
    }

    test("can target its own controller") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player1, "Savannah Lions")

        d.castAndChoose(d.player1, number = 1, name = "Savannah Lions")

        d.getLifeTotal(d.player1) shouldBe 12
        d.getLifeTotal(d.player2) shouldBe 20
    }
})
