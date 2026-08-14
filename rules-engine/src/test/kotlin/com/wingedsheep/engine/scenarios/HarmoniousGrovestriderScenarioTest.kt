package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SpellCounteredEvent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Harmonious Grovestrider — {3}{G}{G} Creature — Beast
 *
 * "Ward {2}
 *  Harmonious Grovestrider's power and toughness are each equal to the number of lands you control."
 *
 * Ward is a *printed keyword ability* on the creature itself. The bug being guarded here: it was
 * authored as `GrantWard(WardCost.Mana("{2}"))`, whose default filter is `Scope.AttachedTo` — the
 * Aura/Equipment shape. On a creature that isn't attached to anything the grant matched no
 * permanent, so the card effectively had no ward at all.
 */
class HarmoniousGrovestriderScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        // TestCards.all spans MtgSetCatalog, so the EOE printing is already registered.
        driver.registerCards(TestCards.all)
        return driver
    }

    test("ward {2} counters an opponent's spell targeting it when they can't pay") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)

        val caster = driver.activePlayer!!
        val controller = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Three lands make the Grovestrider exactly a 3/3 — precisely lethal to Lightning Bolt's
        // 3 damage. That's what makes the survival assertion below mean something: a live
        // Grovestrider can only mean the Bolt never resolved. Don't raise this land count.
        repeat(3) { driver.putLandOnBattlefield(controller, "Forest") }
        val grovestrider = driver.putCreatureOnBattlefield(controller, "Harmonious Grovestrider")

        // The caster has exactly {R} for the Bolt and no way to pay the {2} ward cost.
        driver.giveMana(caster, Color.RED, 1)
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpellWithTargets(
            caster, bolt, listOf(ChosenTarget.Permanent(grovestrider))
        ).isSuccess shouldBe true

        withClue("the ward trigger goes on the stack above the Bolt") {
            driver.stackSize shouldBe 2
        }

        driver.bothPass() // resolve the ward trigger — caster can't pay, so Bolt is countered
        withClue("no payment prompt: the caster has no mana sources left") {
            driver.pendingDecision shouldBe null
        }
        driver.bothPass()

        withClue("the 3/3 outlived a Bolt that would have been exactly lethal") {
            driver.findPermanent(controller, "Harmonious Grovestrider") shouldNotBe null
        }
        // The graveyard alone can't tell the two outcomes apart — a Bolt that resolves ends up
        // there too — so pin the actual counter.
        withClue("ward countered the Bolt rather than it resolving") {
            driver.events.filterIsInstance<SpellCounteredEvent>()
                .map { it.cardName } shouldContain "Lightning Bolt"
        }
        driver.getGraveyardCardNames(caster).contains("Lightning Bolt") shouldBe true
    }

    test("power and toughness each equal the number of lands you control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(2) { driver.putLandOnBattlefield(player, "Forest") }
        val grovestrider = driver.putCreatureOnBattlefield(player, "Harmonious Grovestrider")

        withClue("two lands ⇒ 2/2") {
            val projected = driver.state.projectedState
            projected.getProjectedValues(grovestrider)?.power shouldBe 2
            projected.getProjectedValues(grovestrider)?.toughness shouldBe 2
        }

        repeat(3) { driver.putLandOnBattlefield(player, "Forest") }

        withClue("five lands ⇒ 5/5, recomputed live") {
            val projected = driver.state.projectedState
            projected.getProjectedValues(grovestrider)?.power shouldBe 5
            projected.getProjectedValues(grovestrider)?.toughness shouldBe 5
        }

        withClue("only lands you control count — the opponent's don't") {
            val opponent = driver.getOpponent(player)
            repeat(4) { driver.putLandOnBattlefield(opponent, "Forest") }
            driver.state.projectedState.getProjectedValues(grovestrider)?.power shouldBe 5
        }
    }
})
