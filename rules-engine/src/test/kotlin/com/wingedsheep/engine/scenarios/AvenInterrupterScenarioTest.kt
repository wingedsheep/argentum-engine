package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.AvenInterrupter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Aven Interrupter (OTJ) — {1}{W}{W} 2/2 Bird Rogue, Flash, Flying.
 *
 * "When this creature enters, exile target spell. It becomes plotted."
 * "Spells your opponents cast from graveyards or from exile cost {2} more to cast."
 *
 * The ETB ability uses the new [com.wingedsheep.sdk.scripting.effects.ExileTargetSpellEffect]
 * (a non-counter that exiles even uncounterable spells and plots the card for its owner). The
 * tax is a [com.wingedsheep.sdk.scripting.ModifySpellCost] with the new
 * [com.wingedsheep.sdk.scripting.SpellCostTarget.OpponentsCastFromZones] target.
 */
class AvenInterrupterScenarioTest : FunSpec({

    test("ETB exiles the target spell and plots it for its owner — the spell never resolves") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AvenInterrupter)
        driver.initMirrorMatch(deck = com.wingedsheep.sdk.model.Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Opponent casts an instant so it sits on the stack as a target. The active player passes
        // priority first so the opponent (non-active) gets a window to cast at instant speed.
        driver.passPriority(me)
        val oppSpell = driver.putCardInHand(opp, "Lightning Bolt")
        driver.giveMana(opp, Color.RED, 1)
        driver.castSpell(opp, oppSpell, listOf(me)).isSuccess shouldBe true
        driver.getStackSpellNames().contains("Lightning Bolt") shouldBe true

        // Opponent passes priority back so I get a window to respond at instant speed.
        driver.passPriority(opp)

        // I flash in Aven Interrupter; its ETB targets the Lightning Bolt spell.
        val aven = driver.putCardInHand(me, "Aven Interrupter")
        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, aven).isSuccess shouldBe true
        driver.bothPass() // resolve Aven onto the battlefield
        driver.bothPass() // resolve the ETB trigger off the stack

        // Pick the spell to exile (the ETB targets a spell on the stack).
        driver.submitTargetSelection(me, listOf(oppSpell))
        driver.bothPass()

        // The targeted spell is exiled (it did NOT resolve into a creature), plotted for its owner.
        driver.getExile(opp).contains(oppSpell) shouldBe true
        driver.getPermanents(opp).contains(oppSpell) shouldBe false
        val plotted = driver.state.getEntity(oppSpell)?.get<PlottedComponent>()
        plotted shouldNotBe null
        withClue("a plotted card's owner — not the player who plotted it — may cast it later") {
            plotted!!.controllerId shouldBe opp
        }
    }
})

/**
 * The cost tax is verified directly via the [CostCalculator] — it depends on the casting player's
 * relationship to Aven's controller and the zone the spell is cast from, which the calculator reads
 * from `fromZone`.
 */
class AvenInterrupterTaxTest : ScenarioTestBase() {
    init {
        context("Aven Interrupter — opponents' graveyard/exile casts cost {2} more") {

            test("an opponent casting from a graveyard pays {2} more") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aven Interrupter")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .build()

                val calc = CostCalculator(cardRegistry)
                val cost = calc.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Grizzly Bears"),
                    game.player2Id,
                    fromZone = Zone.GRAVEYARD,
                )
                withClue("Grizzly Bears base generic is 1; +2 from Aven = 3") {
                    cost.genericAmount shouldBe 3
                }
            }

            test("an opponent casting from exile pays {2} more") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aven Interrupter")
                    .withCardInExile(2, "Grizzly Bears")
                    .build()

                val calc = CostCalculator(cardRegistry)
                val cost = calc.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Grizzly Bears"),
                    game.player2Id,
                    fromZone = Zone.EXILE,
                )
                cost.genericAmount shouldBe 3
            }

            test("the same opponent's cast from hand is NOT taxed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aven Interrupter")
                    .withCardInHand(2, "Grizzly Bears")
                    .build()

                val calc = CostCalculator(cardRegistry)
                val cost = calc.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Grizzly Bears"),
                    game.player2Id,
                    fromZone = Zone.HAND,
                )
                withClue("hand casts are unaffected") { cost.genericAmount shouldBe 1 }
            }

            test("the controller's own graveyard cast is NOT taxed (opponents only)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aven Interrupter")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .build()

                val calc = CostCalculator(cardRegistry)
                val cost = calc.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Grizzly Bears"),
                    game.player1Id,
                    fromZone = Zone.GRAVEYARD,
                )
                withClue("the tax applies only to the controller's opponents") { cost.genericAmount shouldBe 1 }
            }
        }
    }
}
