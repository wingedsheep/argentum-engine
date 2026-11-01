package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.pendingQuestionIsPrivate
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.view.ClientEvent
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.RevealFirstDrawEachTurn
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * `RevealFirstDrawEachTurn(optional = true)` — "You may reveal the first card you draw each turn as
 * you draw it" (God-Eternal Kefnet).
 *
 * The question is asked as the card is drawn: before any later card of the same draw instruction
 * (Kefnet's ruling: "Multiple card draws are always treated as a sequence of individual card draws
 * … Only the first card drawn this way may be revealed"), once per optional source, and only on the
 * turn's first draw. Saying yes is that source's reveal, so only its "whenever you reveal … this
 * way" trigger fires (CR 607.1, linked abilities); saying no leaves a mandatory reveal by another
 * source untouched. A miracle reveal of the same card is asked after it.
 */
class OptionalFirstDrawRevealTest : FunSpec({

    val optionalRevealer = card("Test Optional Revealer") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility { ability = RevealFirstDrawEachTurn(optional = true) }
        triggeredAbility {
            trigger = Triggers.you.revealsFirstDraw(GameObjectFilter.Land)
            effect = Effects.GainLife(2)
        }
    }
    val mandatoryRevealer = card("Test Mandatory Revealer") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility { ability = RevealFirstDrawEachTurn() }
        triggeredAbility {
            trigger = Triggers.you.revealsFirstDraw(GameObjectFilter.Land)
            effect = Effects.GainLife(5)
        }
    }
    val drawOne = card("Test Draw One") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(1) }
    }
    val drawThree = card("Test Draw Three") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(3) }
    }
    val miracleCard = card("Test Miracle Spell") {
        manaCost = "{5}{U}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
        keywordAbility(KeywordAbility.miracle("{U}"))
    }

    // Starting player's first turn: no draw-step draw, so a spell's draw is the turn's first.
    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(optionalRevealer, mandatoryRevealer, drawOne, drawThree, miracleCard)
        )
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.drain() {
        var guard = 0
        while (guard++ < 30 && state.stack.isNotEmpty() && pendingDecision == null) bothPass()
    }

    fun GameTestDriver.reveals(): List<CardRevealedFromDrawEvent> =
        events.filterIsInstance<CardRevealedFromDrawEvent>()

    fun GameTestDriver.revealQuestion(): YesNoDecision? =
        (pendingDecision as? YesNoDecision)?.takeIf { it.prompt.contains("Test Optional Revealer") }

    fun GameTestDriver.cast(player: EntityId, name: String) {
        castSpell(player, putCardInHand(player, name))
        drain()
    }

    test("yes reveals the card for this source and fires its trigger") {
        val driver = newDriver()
        val me = driver.player1
        val source = driver.putPermanentOnBattlefield(me, "Test Optional Revealer")
        val forest = driver.putCardOnTopOfLibrary(me, "Forest")
        val life = driver.getLifeTotal(me)

        driver.cast(me, "Test Draw One")

        val question = driver.revealQuestion()!!
        question.playerId shouldBe me
        question.prompt shouldBe "Reveal Forest with Test Optional Revealer?"
        question.context.sourceId shouldBe source
        withClue("the source is public, so the question is too") {
            driver.state.pendingQuestionIsPrivate shouldBe false
        }
        withClue("the card is already in hand as it is asked") {
            driver.getHand(me).contains(forest) shouldBe true
        }

        driver.submitYesNo(me, true)
        val reveal = driver.reveals().single()
        reveal.cardEntityId shouldBe forest
        reveal.revealedBy shouldBe listOf(source)
        withClue("the table's reveal overlay names the revealing permanent as its source card") {
            val shown = ClientEventTransformer.transform(listOf(reveal), driver.getOpponent(me), driver.state)
                .filterIsInstance<ClientEvent.CardsRevealed>().single()
            shown.cardNames shouldBe listOf("Forest")
            shown.source shouldBe "Test Optional Revealer"
        }
        driver.drain()
        driver.getLifeTotal(me) shouldBe life + 2
    }

    test("no reveals nothing and fires nothing") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Test Optional Revealer")
        driver.putCardOnTopOfLibrary(me, "Forest")
        val life = driver.getLifeTotal(me)

        driver.cast(me, "Test Draw One")
        driver.submitYesNo(me, false)
        driver.drain()

        driver.reveals().shouldBeEmpty()
        driver.state.stack.shouldBeEmpty()
        driver.getLifeTotal(me) shouldBe life
    }

    test("a multi-card draw asks about the first card before drawing the rest, and only once") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Test Optional Revealer")
        val forest = driver.putCardOnTopOfLibrary(me, "Forest")
        val handBefore = driver.getHand(me).size
        val libBefore = driver.state.getLibrary(me).size

        driver.cast(me, "Test Draw Three")

        driver.revealQuestion()!!
        withClue("only the first of the three has been drawn when the question is asked") {
            driver.state.getLibrary(me).size shouldBe libBefore - 1
        }
        driver.submitYesNo(me, true)
        driver.reveals().single().cardEntityId shouldBe forest
        withClue("the rest of the instruction draws on without asking again") {
            driver.revealQuestion() shouldBe null
            driver.state.getLibrary(me).size shouldBe libBefore - 3
            // +3 drawn, the cast spell came from and left the hand.
            driver.getHand(me).size shouldBe handBefore + 3
        }
    }

    test("a later draw in the same turn is not asked about") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Test Optional Revealer")

        driver.cast(me, "Test Draw One")
        driver.submitYesNo(me, false)
        driver.drain()

        driver.cast(me, "Test Draw One")
        driver.revealQuestion() shouldBe null
        driver.reveals().shouldBeEmpty()
    }

    test("the draw-step draw is asked about, then the draw step goes on") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        val forest = driver.putCardOnTopOfLibrary(opponent, "Forest")
        driver.putPermanentOnBattlefield(opponent, "Test Optional Revealer")

        var guard = 0
        while (guard++ < 60 && driver.revealQuestion() == null) {
            driver.passPriority(driver.state.priorityPlayerId ?: error("no priority at ${driver.state.step}"))
        }
        driver.state.step shouldBe Step.DRAW
        driver.revealQuestion()!!.playerId shouldBe opponent

        driver.submitYesNo(opponent, true)
        driver.reveals().single().cardEntityId shouldBe forest
        withClue("the draw step goes on: the trigger is put on the stack, the active player gets priority") {
            driver.state.stack.size shouldBe 1
            driver.state.priorityPlayerId shouldBe opponent
        }
    }

    test("declining the optional reveal leaves another source's mandatory reveal and trigger alone") {
        val driver = newDriver()
        val me = driver.player1
        val mandatory = driver.putPermanentOnBattlefield(me, "Test Mandatory Revealer")
        driver.putPermanentOnBattlefield(me, "Test Optional Revealer")
        val forest = driver.putCardOnTopOfLibrary(me, "Forest")
        val life = driver.getLifeTotal(me)

        driver.cast(me, "Test Draw One")

        withClue("the mandatory reveal happened as the card was drawn") {
            driver.reveals().single().revealedBy shouldBe listOf(mandatory)
        }
        driver.submitYesNo(me, false)
        driver.drain()

        driver.reveals().map { it.cardEntityId } shouldBe listOf(forest)
        withClue("only the mandatory source's trigger fired (+5, not +7)") {
            driver.getLifeTotal(me) shouldBe life + 5
        }
    }

    test("two optional sources are asked one after the other, each linked to its own trigger") {
        val driver = newDriver()
        val me = driver.player1
        val first = driver.putPermanentOnBattlefield(me, "Test Optional Revealer")
        val second = driver.putPermanentOnBattlefield(me, "Test Optional Revealer")
        driver.putCardOnTopOfLibrary(me, "Forest")
        val life = driver.getLifeTotal(me)

        driver.cast(me, "Test Draw One")
        val asked = mutableListOf(driver.revealQuestion()!!.context.sourceId)
        driver.submitYesNo(me, true)
        asked += driver.revealQuestion()!!.context.sourceId
        driver.submitYesNo(me, false)
        driver.drain()

        asked.toSet() shouldBe setOf(first, second)
        driver.reveals().single().revealedBy shouldBe listOf(asked.first())
        driver.getLifeTotal(me) shouldBe life + 2
    }

    test("a miracle card is asked about the optional reveal first, then its miracle") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Test Optional Revealer")
        driver.putCardOnTopOfLibrary(me, "Test Miracle Spell")

        driver.cast(me, "Test Draw One")
        driver.revealQuestion()!!
        driver.submitYesNo(me, true)

        val miracle = driver.pendingDecision as YesNoDecision
        miracle.prompt shouldBe "Reveal Test Miracle Spell for its miracle?"
        driver.state.pendingQuestionIsPrivate shouldBe true
    }
})
