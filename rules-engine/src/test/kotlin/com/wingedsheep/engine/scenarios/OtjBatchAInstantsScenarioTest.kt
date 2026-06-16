package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.CaughtInTheCrossfire
import com.wingedsheep.mtg.sets.definitions.otj.cards.ExplosiveDerailment
import com.wingedsheep.mtg.sets.definitions.otj.cards.ThunderSalvo
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for OTJ batch A instants: Thunder Salvo, Explosive Derailment,
 * Caught in the Crossfire.
 *
 * Thunder Salvo exercises the dynamic damage amount `2 + other spells you've cast
 * this turn`. The Spree cards reuse the modal-with-per-mode-additional-cost path
 * (covered structurally by ModalPerModeAdditionalCostTest / ModalMinChooseCountTest);
 * here we prove the card-specific effects: ranged damage / destroy-artifact and the
 * outlaw / non-outlaw group damage.
 */
class OtjBatchAInstantsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + ThunderSalvo + ExplosiveDerailment + CaughtInTheCrossfire
        )
        return driver
    }

    // ---------------------------------------------------------------------
    // Thunder Salvo
    // ---------------------------------------------------------------------

    test("Thunder Salvo with no other spells cast deals 2 damage (kills a 2/2)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2

        val salvo = driver.putCardInHand(player, "Thunder Salvo")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 1)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = salvo,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        )
        driver.bothPass()

        // 2 damage to a 2/2 -> dies.
        driver.findPermanent(opponent, "Black Creature") shouldBe null
    }

    test("Thunder Salvo deals 2 + other spells cast this turn (3 after one other spell)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // A 3-toughness creature survives 2 damage but dies to 3.
        val giant = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        // Cast one other spell first this turn (a 0-cost-ish instant resolved fully).
        val otherSpell = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = otherSpell,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        driver.bothPass()

        // Now Thunder Salvo: X = 2 + 1 other spell = 3 -> kills the 3/3.
        val salvo = driver.putCardInHand(player, "Thunder Salvo")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 1)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = salvo,
                targets = listOf(ChosenTarget.Permanent(giant))
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
    }

    // ---------------------------------------------------------------------
    // Explosive Derailment (Spree)
    // ---------------------------------------------------------------------

    test("Explosive Derailment damage mode deals 4 to target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val giant = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        val spell = driver.putCardInHand(player, "Explosive Derailment")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2) // {R} base + {2} for the damage mode
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(giant)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(giant)))
            )
        )
        driver.bothPass()

        // 4 damage to a 3/3 -> dies.
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
    }

    test("Explosive Derailment destroy mode destroys target artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val artifact = driver.putPermanentOnBattlefield(opponent, "Artifact Creature")

        val spell = driver.putCardInHand(player, "Explosive Derailment")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2) // {R} base + {2} for the destroy mode
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(artifact)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(artifact)))
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Artifact Creature") shouldBe null
    }

    test("Explosive Derailment both modes: 4 damage to a creature AND destroy an artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val giant = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        val artifact = driver.putPermanentOnBattlefield(opponent, "Artifact Creature")

        val spell = driver.putCardInHand(player, "Explosive Derailment")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 4) // {R} base + {2} + {2}
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(giant), ChosenTarget.Permanent(artifact)),
                chosenModes = listOf(0, 1),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(giant)),
                    listOf(ChosenTarget.Permanent(artifact))
                )
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.findPermanent(opponent, "Artifact Creature") shouldBe null
    }

    // ---------------------------------------------------------------------
    // Caught in the Crossfire (Spree)
    // ---------------------------------------------------------------------

    test("Caught in the Crossfire outlaw mode hits only outlaws") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Ragavan, Nimble Pilferer is a 2/1 Pirate (outlaw); Black Creature is a 2/2 non-outlaw.
        val ragavan = driver.putCreatureOnBattlefield(opponent, "Ragavan, Nimble Pilferer")
        driver.putCreatureOnBattlefield(opponent, "Black Creature")

        val spell = driver.putCardInHand(player, "Caught in the Crossfire")
        driver.giveMana(player, Color.RED, 2)
        driver.giveColorlessMana(player, 1) // {R}{R} base + {1} outlaw mode
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(0)
            )
        )
        driver.bothPass()

        // 2 damage to outlaws only: Ragavan (2/1) dies, Black Creature (2/2) survives.
        driver.findPermanent(opponent, "Ragavan, Nimble Pilferer") shouldBe null
        driver.findPermanent(opponent, "Black Creature") shouldNotBe null
    }

    test("Caught in the Crossfire non-outlaw mode hits only non-outlaws") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Ragavan, Nimble Pilferer") // 2/1 Pirate
        driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2 non-outlaw

        val spell = driver.putCardInHand(player, "Caught in the Crossfire")
        driver.giveMana(player, Color.RED, 2)
        driver.giveColorlessMana(player, 1) // {R}{R} base + {1} non-outlaw mode
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(1)
            )
        )
        driver.bothPass()

        // 2 damage to non-outlaws only: Black Creature (2/2) dies, Ragavan survives.
        driver.findPermanent(opponent, "Black Creature") shouldBe null
        driver.findPermanent(opponent, "Ragavan, Nimble Pilferer") shouldNotBe null
    }

    test("Caught in the Crossfire both modes: 2 damage to every creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Ragavan, Nimble Pilferer") // outlaw
        driver.putCreatureOnBattlefield(opponent, "Black Creature") // non-outlaw

        val spell = driver.putCardInHand(player, "Caught in the Crossfire")
        driver.giveMana(player, Color.RED, 2)
        driver.giveColorlessMana(player, 2) // {R}{R} base + {1} + {1}
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(0, 1)
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Ragavan, Nimble Pilferer") shouldBe null
        driver.findPermanent(opponent, "Black Creature") shouldBe null
    }
})
