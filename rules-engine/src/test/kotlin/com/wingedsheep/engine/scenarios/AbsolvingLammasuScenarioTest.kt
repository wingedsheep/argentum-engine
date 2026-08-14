package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.AbsolvingLammasu
import com.wingedsheep.mtg.sets.definitions.mkm.cards.RepeatOffender
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Absolving Lammasu — "When this creature enters, all suspected creatures are no longer suspected."
 *
 * Covers the new `Effects.NoLongerSuspected` vocabulary (CR 701.60c). Three things have to hold and
 * only the first is obvious:
 *
 *  1. the designation comes off — `ProjectedState.isSuspected` goes false;
 *  2. the menace and can't-block that `Effects.Suspect` applied alongside it come off *too*, since
 *     they exist only for as long as the creature is suspected. Those are separate floating effects
 *     with no back-reference to the status, so a naive "clear the status" implementation would leave
 *     a creature that isn't suspected but still can't block;
 *  3. it is symmetric — "all suspected creatures" is every creature on the battlefield, so an
 *     opponent's suspect is absolved as readily as your own.
 *
 * Repeat Offender is the suspect source on both sides: its `{2}{B}` ability suspects it when it
 * isn't already.
 */
class AbsolvingLammasuScenarioTest : FunSpec({

    val offenderAbility = RepeatOffender.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AbsolvingLammasu)
        driver.registerCard(RepeatOffender)
        return driver
    }

    /**
     * Hand priority to [player] without advancing the step. Priority ends up wherever the last
     * resolution left it, and a player can only act while holding it — with an empty stack a single
     * pass just moves it across, so this is safe to call unconditionally.
     */
    fun handPriorityTo(driver: GameTestDriver, player: EntityId) {
        driver.priorityPlayer?.takeIf { it != player }?.let { driver.passPriority(it) }
    }

    /** Put a Repeat Offender under [player] and activate it once so it becomes suspected. */
    fun suspectedOffender(driver: GameTestDriver, player: EntityId): EntityId {
        val offender = driver.putCreatureOnBattlefield(player, "Repeat Offender")
        driver.giveMana(player, Color.BLACK, 3)
        handPriorityTo(driver, player)
        driver.submitSuccess(ActivateAbility(player, offender, offenderAbility))
        driver.bothPass()
        StateProjector().project(driver.state).isSuspected(offender) shouldBe true
        return offender
    }

    test("entering absolves every suspected creature, on both sides, keyword grants included") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = suspectedOffender(driver, active)
        val theirs = suspectedOffender(driver, opponent)

        val lammasu = driver.putCardInHand(active, "Absolving Lammasu")
        driver.giveMana(active, Color.WHITE, 5)
        handPriorityTo(driver, active)
        driver.castSpellWithTargets(active, lammasu, emptyList()).error shouldBe null
        driver.bothPass() // resolve the creature spell; the enters trigger goes on the stack
        driver.bothPass() // resolve the trigger

        val projected = StateProjector().project(driver.state)
        listOf("yours" to mine, "the opponent's" to theirs).forEach { (whose, offender) ->
            withClue("$whose Offender is no longer suspected") {
                projected.isSuspected(offender) shouldBe false
            }
            withClue("$whose Offender loses the menace the suspect granted") {
                projected.hasKeyword(offender, Keyword.MENACE) shouldBe false
            }
            withClue("$whose Offender can block again") {
                projected.cantBlock(offender) shouldBe false
            }
        }
    }

    test("an unsuspected board is untouched — absolving is a no-op, not a keyword wipe") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Menace from a printed keyword, not from a suspect — it must survive.
        val menacing = driver.putCreatureOnBattlefield(active, "Goblin Trailblazer")
        val plain = driver.putCreatureOnBattlefield(active, "Grizzly Bears")

        val lammasu = driver.putCardInHand(active, "Absolving Lammasu")
        driver.giveMana(active, Color.WHITE, 5)
        driver.castSpellWithTargets(active, lammasu, emptyList()).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        val projected = StateProjector().project(driver.state)
        withClue("nothing was suspected, so nothing changed") {
            projected.isSuspected(plain) shouldBe false
            projected.cantBlock(plain) shouldBe false
        }
        withClue("menace a creature has from its own printed text is not a suspect and stays") {
            projected.hasKeyword(menacing, Keyword.MENACE) shouldBe true
        }
    }
})
