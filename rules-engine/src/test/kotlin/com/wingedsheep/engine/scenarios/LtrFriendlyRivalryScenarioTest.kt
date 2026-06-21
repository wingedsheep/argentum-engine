package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.FriendlyRivalry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Friendly Rivalry {R}{G} Instant (LTR canonical).
 *
 * "Target creature you control and up to one other target legendary creature you control each
 * deal damage equal to their power to target creature you don't control."
 *
 * Three target requirements:
 *  - index 0: a creature you control,
 *  - index 1: up to one *other* legendary creature you control,
 *  - index 2: a creature you don't control.
 *
 * The word "other" in index 1 means *other than the index-0 creature* (CR 601.2c) — the same
 * permanent must not be able to fill both the "creature you control" and the "other legendary
 * creature you control" slots, which would otherwise let a single creature deal its power twice.
 */
class LtrFriendlyRivalryScenarioTest : FunSpec({

    // A legendary creature so it is eligible for the "other legendary creature you control" slot.
    val legendaryBruiser = CardDefinition.creature(
        name = "Test Legendary Bruiser",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 4,
        toughness = 4,
        supertypes = setOf(Supertype.LEGENDARY)
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + FriendlyRivalry + legendaryBruiser)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("the same legendary creature cannot fill both the 'creature you control' and 'other legendary' slots") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // One legendary creature you control, and a target the opponent controls.
        val myLegend = driver.putCreatureOnBattlefield(me, "Test Legendary Bruiser") // 4/4 legendary
        val theirCreature = driver.putCreatureOnBattlefield(opp, "Force of Nature")   // 5/5

        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)
        val spell = driver.putCardInHand(me, "Friendly Rivalry")

        // Attempt to choose the SAME creature for target 0 and target 1. This is illegal —
        // "other" must differ from the creature chosen for index 0 — so the cast must be rejected.
        val result = driver.castSpellWithTargets(
            me,
            spell,
            targets = listOf(
                ChosenTarget.Permanent(myLegend),
                ChosenTarget.Permanent(myLegend),
                ChosenTarget.Permanent(theirCreature)
            )
        )

        result.error shouldNotBe null
    }

    test("two distinct creatures each deal their power to the opponent's creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // index 0: any creature you control (3/3). index 1: a different legendary creature (4/4).
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")        // 3/3
        val myLegend = driver.putCreatureOnBattlefield(me, "Test Legendary Bruiser") // 4/4 legendary
        val theirCreature = driver.putCreatureOnBattlefield(opp, "Force of Nature")  // 5/5

        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)
        val spell = driver.putCardInHand(me, "Friendly Rivalry")

        driver.castSpellWithTargets(
            me,
            spell,
            targets = listOf(
                ChosenTarget.Permanent(courser),
                ChosenTarget.Permanent(myLegend),
                ChosenTarget.Permanent(theirCreature)
            )
        ).error shouldBe null
        driver.bothPass()

        // 3 + 4 = 7 damage to a 5/5 → it dies; both of my creatures are unharmed.
        driver.getPermanents(opp).contains(theirCreature) shouldBe false
        driver.getPermanents(me).contains(courser) shouldBe true
        driver.getPermanents(me).contains(myLegend) shouldBe true
    }
})
