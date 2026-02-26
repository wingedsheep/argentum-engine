package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Uncontrolled Infestation.
 *
 * Uncontrolled Infestation: {1}{R}
 * Enchantment — Aura
 * Enchant nonbasic land
 * When enchanted land becomes tapped, destroy it.
 */
class UncontrolledInfestationTest : FunSpec({

    val UncontrolledInfestation = card("Uncontrolled Infestation") {
        manaCost = "{1}{R}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant nonbasic land\nWhen enchanted land becomes tapped, destroy it."

        auraTarget = TargetPermanent(
            filter = TargetFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.IsLand,
                        CardPredicate.Not(CardPredicate.IsBasicLand)
                    )
                )
            )
        )

        triggeredAbility {
            trigger = Triggers.EnchantedPermanentBecomesTapped
            effect = MoveToZoneEffect(EffectTarget.EnchantedCreature, Zone.GRAVEYARD, byDestruction = true)
        }
    }

    // A nonbasic land that produces {R} for testing.
    // This lets us trigger the enchantment via auto-pay when casting a red spell.
    val TestNonbasicLand = CardDefinition(
        name = "Test Nonbasic Land",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            cardTypes = setOf(CardType.LAND)
        ),
        oracleText = "{T}: Add {R}.",
        script = CardScript.permanent(
            ActivatedAbility(
                id = AbilityId("test-nonbasic-mana"),
                cost = AbilityCost.Tap,
                effect = AddManaEffect(Color.RED),
                targetRequirement = null,
                isManaAbility = true
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UncontrolledInfestation, TestNonbasicLand))
        return driver
    }

    test("enchanted nonbasic land is destroyed when tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a nonbasic land on the battlefield
        val nonbasicLand = driver.putLandOnBattlefield(activePlayer, "Test Nonbasic Land")
        driver.findPermanent(activePlayer, "Test Nonbasic Land") shouldNotBe null

        // Cast Uncontrolled Infestation on the nonbasic land
        val aura = driver.putCardInHand(activePlayer, "Uncontrolled Infestation")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, aura, listOf(nonbasicLand))
        driver.bothPass() // Resolve aura

        // Verify the aura is on the battlefield
        driver.findPermanent(activePlayer, "Uncontrolled Infestation") shouldNotBe null

        // Cast Lightning Bolt (costs {R}) — auto-pay taps the enchanted nonbasic land.
        // CastSpellHandler processes triggers from TappedEvent.
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val opponent = driver.getOpponent(activePlayer)
        driver.castSpell(activePlayer, bolt, listOf(opponent))

        // The triggered ability from the aura should be on the stack (along with the bolt)
        // Stack: Lightning Bolt (bottom), Uncontrolled Infestation trigger (top)
        driver.stackSize shouldBe 2

        // Resolve the trigger first (top of stack)
        driver.bothPass()

        // The nonbasic land should be destroyed
        driver.findPermanent(activePlayer, "Test Nonbasic Land") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Test Nonbasic Land"

        // The aura should also be gone (falls off when enchanted permanent leaves)
        driver.findPermanent(activePlayer, "Uncontrolled Infestation") shouldBe null
    }

    test("cannot enchant basic lands") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a basic land on the battlefield
        val basicLand = driver.putLandOnBattlefield(activePlayer, "Mountain")

        // Try to cast Uncontrolled Infestation on a basic land
        val aura = driver.putCardInHand(activePlayer, "Uncontrolled Infestation")
        driver.giveMana(activePlayer, Color.RED, 2)
        val result = driver.castSpell(activePlayer, aura, listOf(basicLand))
        result.isSuccess shouldBe false
    }

    test("tapping enchanted land before aura is attached does not trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a nonbasic land on the battlefield and tap it first
        val nonbasicLand = driver.putLandOnBattlefield(activePlayer, "Test Nonbasic Land")
        driver.tapPermanent(nonbasicLand)

        // Cast Uncontrolled Infestation on the already-tapped nonbasic land
        val aura = driver.putCardInHand(activePlayer, "Uncontrolled Infestation")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, aura, listOf(nonbasicLand))
        driver.bothPass() // Resolve aura

        // The aura should be on the battlefield, and the land should still be there
        // (attaching to an already-tapped land doesn't trigger)
        driver.findPermanent(activePlayer, "Uncontrolled Infestation") shouldNotBe null
        driver.findPermanent(activePlayer, "Test Nonbasic Land") shouldNotBe null
    }
})
