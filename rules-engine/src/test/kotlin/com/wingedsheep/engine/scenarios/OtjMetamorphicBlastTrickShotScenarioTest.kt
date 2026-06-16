package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.MetamorphicBlast
import com.wingedsheep.mtg.sets.definitions.otj.cards.TrickShot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for OTJ batch (schemes/spells): Metamorphic Blast (Spree) and Trick Shot.
 *
 * Metamorphic Blast exercises the per-mode Spree shape plus [Effects.BecomeCreature]
 * setting base 0/1 + Rabbit type + white color; and the draw-two mode.
 * Trick Shot exercises a two-target damage spell whose second target is an optional
 * "other" creature **token** (the new `GameObjectFilter.token()` filter modifier).
 */
class OtjMetamorphicBlastTrickShotScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + MetamorphicBlast + TrickShot)
        return driver
    }

    // ---------------------------------------------------------------------
    // Metamorphic Blast
    // ---------------------------------------------------------------------

    test("Metamorphic Blast mode 1 makes target a white Rabbit with base power/toughness 0/1") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2 Zombie

        val spell = driver.putCardInHand(player, "Metamorphic Blast")
        driver.giveMana(player, Color.BLUE, 1) // {U} base
        driver.giveColorlessMana(player, 1)     // {1} for mode 0
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bear)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bear)))
            )
        )
        driver.bothPass()

        // Base P/T set to 0/1, type set to Rabbit, color set to white.
        projector.getProjectedPower(driver.state, bear) shouldBe 0
        projector.getProjectedToughness(driver.state, bear) shouldBe 1
        val projected = projector.project(driver.state)
        projected.hasSubtype(bear, "Rabbit") shouldBe true
        projected.hasSubtype(bear, "Zombie") shouldBe false
        projected.getColors(bear) shouldBe setOf("WHITE")
    }

    test("Metamorphic Blast mode 2 makes target player draw two cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val handBefore = driver.getHandSize(player)
        val spell = driver.putCardInHand(player, "Metamorphic Blast")
        driver.giveMana(player, Color.BLUE, 1) // {U} base
        driver.giveColorlessMana(player, 3)     // {3} for mode 1
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(player)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(player)))
            )
        )
        driver.bothPass()

        // handBefore captured before adding the Blast: +1 (Blast) -1 (cast) +2 (drawn) = +2.
        driver.getHandSize(player) shouldBe handBefore + 2
    }

    test("Metamorphic Blast both modes: transform a creature AND draw two cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2
        val handBefore = driver.getHandSize(player)

        val spell = driver.putCardInHand(player, "Metamorphic Blast")
        driver.giveMana(player, Color.BLUE, 1) // {U} base
        driver.giveColorlessMana(player, 4)     // {1} + {3}
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bear), ChosenTarget.Player(player)),
                chosenModes = listOf(0, 1),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(bear)),
                    listOf(ChosenTarget.Player(player))
                )
            )
        )
        driver.bothPass()

        projector.getProjectedPower(driver.state, bear) shouldBe 0
        projector.getProjectedToughness(driver.state, bear) shouldBe 1
        // +1 (Blast added) -1 (cast) +2 (drawn) = +2
        driver.getHandSize(player) shouldBe handBefore + 2
    }

    // ---------------------------------------------------------------------
    // Trick Shot
    // ---------------------------------------------------------------------

    test("Trick Shot deals 6 to target creature and 2 to a creature token") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val realCreature = driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2 nontoken
        val tokenCreature = driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2
        driver.addComponent(tokenCreature, TokenComponent) // mark as a token

        val spell = driver.putCardInHand(player, "Trick Shot")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 4) // {4}{R}
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(
                    ChosenTarget.Permanent(realCreature),
                    ChosenTarget.Permanent(tokenCreature)
                )
            )
        )
        driver.bothPass()

        // 6 damage kills the 2/2; 2 damage kills the 2/2 token. No "Black Creature" left.
        driver.findPermanent(opponent, "Black Creature") shouldBe null
    }

    test("Trick Shot can be cast with only the mandatory target (omit the optional token)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val realCreature = driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2

        val spell = driver.putCardInHand(player, "Trick Shot")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 4)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(realCreature))
            )
        )
        driver.bothPass()

        // 6 damage kills the 2/2.
        driver.findPermanent(opponent, "Black Creature") shouldBe null
    }

    test("Trick Shot cannot choose a nontoken creature as its second target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val first = driver.putCreatureOnBattlefield(opponent, "Black Creature")
        val secondNonToken = driver.putCreatureOnBattlefield(opponent, "Artifact Creature") // 2/2, not a token

        val spell = driver.putCardInHand(player, "Trick Shot")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 4)
        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(
                    ChosenTarget.Permanent(first),
                    ChosenTarget.Permanent(secondNonToken)
                )
            )
        )
    }
})
