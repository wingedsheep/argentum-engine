package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.AbzanCharm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that modal spells support cast-time mode selection.
 * When chosenMode is set on CastSpell, the mode decision is skipped at resolution
 * and the spell's stackText shows which mode was chosen.
 */
class ModalCastTimeModeTest : FunSpec({

    val BigCreature = CardDefinition.creature(
        name = "Big Creature",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = emptySet(),
        power = 4,
        toughness = 4,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BigCreature))
        driver.registerCard(AbzanCharm)
        return driver
    }

    test("Abzan Charm mode 0 - exile creature with power 3 or greater - cast-time mode") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a 4/4 creature
        driver.putCreatureOnBattlefield(opponent, "Big Creature")

        // Give mana and put Abzan Charm in hand
        driver.giveMana(activePlayer, Color.WHITE, 1)
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.GREEN, 1)
        val charm = driver.putCardInHand(activePlayer, "Abzan Charm")

        // Find the target creature
        val creatureId = driver.getCreatures(opponent).first()

        // Cast with mode 0 (exile creature with power 3+) and target pre-selected
        val result = driver.submit(CastSpell(
            playerId = activePlayer,
            cardId = charm,
            targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(creatureId)),
            chosenMode = 0
        ))
        result.isSuccess shouldBe true

        // Verify chosenModes is stored on the spell
        val spellOnStack = driver.state.stack.firstOrNull()?.let { spellId ->
            driver.state.getEntity(spellId)?.get<SpellOnStackComponent>()
        }
        spellOnStack?.chosenModes shouldBe listOf(0)

        // Both pass → spell resolves → mode executes directly (no mode selection decision)
        driver.bothPass()

        // Creature should no longer be on battlefield (exiled)
        driver.findPermanent(opponent, "Big Creature") shouldBe null
    }

    test("Abzan Charm mode 1 - draw two cards and lose 2 life - cast-time mode") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana and put Abzan Charm in hand
        driver.giveMana(activePlayer, Color.WHITE, 1)
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.GREEN, 1)
        val charm = driver.putCardInHand(activePlayer, "Abzan Charm")

        // Cast with mode 1 (draw 2, lose 2 life) — no targets needed
        val result = driver.submit(CastSpell(
            playerId = activePlayer,
            cardId = charm,
            chosenMode = 1
        ))
        result.isSuccess shouldBe true

        // Capture hand size after casting (charm moved to stack)
        val handSizeAfterCast = driver.state.getHand(activePlayer).size

        // Both pass → spell resolves → draws 2 and loses 2 life
        driver.bothPass()

        // Should have drawn 2 cards
        driver.state.getHand(activePlayer).size shouldBe handSizeAfterCast + 2

        // Should have lost 2 life
        driver.assertLifeTotal(activePlayer, 18)
    }

    test("Abzan Charm without chosenMode uses legacy resolution-time mode selection") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.WHITE, 1)
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.GREEN, 1)
        val charm = driver.putCardInHand(activePlayer, "Abzan Charm")

        // Cast without chosenMode — legacy flow
        driver.castSpell(activePlayer, charm)

        // Both pass → spell resolves → should pause for mode selection
        driver.bothPass()

        // Should get a mode selection decision
        val decision = driver.pendingDecision
        (decision is com.wingedsheep.engine.core.ChooseOptionDecision) shouldBe true
    }
})
