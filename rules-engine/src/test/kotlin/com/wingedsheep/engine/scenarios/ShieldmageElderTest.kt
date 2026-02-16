package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.targeting.TargetPermanent
import com.wingedsheep.sdk.targeting.TargetSpell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Shieldmage Elder:
 * {5}{W}
 * Creature — Human Cleric Wizard
 * 2/3
 * Tap two untapped Clerics you control: Prevent all damage target creature would deal this turn.
 * Tap two untapped Wizards you control: Prevent all damage target spell would deal this turn.
 */
class ShieldmageElderTest : FunSpec({

    val clericAbilityId = AbilityId("shieldmage-elder-ability-0")
    val wizardAbilityId = AbilityId("shieldmage-elder-ability-1")

    val ShieldmageElder = CardDefinition.creature(
        name = "Shieldmage Elder",
        manaCost = ManaCost.parse("{5}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric"), Subtype("Wizard")),
        power = 2,
        toughness = 3,
        oracleText = "Tap two untapped Clerics you control: Prevent all damage target creature would deal this turn.\nTap two untapped Wizards you control: Prevent all damage target spell would deal this turn.",
        script = CardScript(
            activatedAbilities = listOf(
                ActivatedAbility(
                    id = clericAbilityId,
                    cost = AbilityCost.TapPermanents(2, GameObjectFilter.Creature.withSubtype("Cleric")),
                    targetRequirements = listOf(TargetPermanent(filter = TargetFilter.Creature)),
                    effect = PreventAllDamageDealtByTargetEffect(
                        target = EffectTarget.ContextTarget(0)
                    )
                ),
                ActivatedAbility(
                    id = wizardAbilityId,
                    cost = AbilityCost.TapPermanents(2, GameObjectFilter.Creature.withSubtype("Wizard")),
                    targetRequirements = listOf(TargetSpell()),
                    effect = PreventAllDamageDealtByTargetEffect(
                        target = EffectTarget.ContextTarget(0)
                    )
                )
            )
        )
    )

    val TestWizard = CardDefinition.creature(
        name = "Test Wizard",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = setOf(Subtype("Human"), Subtype("Wizard")),
        power = 1,
        toughness = 1
    )

    val LightningBolt = CardDefinition.instant(
        name = "Lightning Bolt",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Lightning Bolt deals 3 damage to any target.",
        script = CardScript.spell(
            effect = DealDamageEffect(3, EffectTarget.ContextTarget(0)),
            com.wingedsheep.sdk.targeting.AnyTarget()
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ShieldmageElder, TestWizard, LightningBolt))
        return driver
    }

    test("Cleric ability prevents all combat damage target creature would deal") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Set up: Shieldmage Elder + another Cleric for tapping
        val elder = driver.putCreatureOnBattlefield(activePlayer, "Shieldmage Elder")
        val cleric = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")

        // Opponent has an attacking creature
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Activate: tap Elder + Cleric (both are Clerics) to prevent Bears' damage
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = elder,
                abilityId = clericAbilityId,
                targets = listOf(ChosenTarget.Permanent(bears)),
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(elder, cleric)
                )
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability
        driver.bothPass()

        // Move to opponent's turn for them to attack
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Opponent attacks with bears
        driver.declareAttackers(opponent, mapOf(bears to activePlayer))
        driver.bothPass()

        // No blocks
        driver.declareNoBlockers(activePlayer)
        driver.bothPass()

        // After combat damage, active player should still have 20 life (damage prevented)
        driver.getLifeTotal(activePlayer) shouldBe 20
    }

    test("Cleric ability requires two Clerics to activate") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only Shieldmage Elder (a Cleric) and a Wizard that's NOT a Cleric
        val elder = driver.putCreatureOnBattlefield(activePlayer, "Shieldmage Elder")
        val wizard = driver.putCreatureOnBattlefield(activePlayer, "Test Wizard")

        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Try to activate Cleric ability with Elder + Wizard (Wizard is not a Cleric)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = elder,
                abilityId = clericAbilityId,
                targets = listOf(ChosenTarget.Permanent(bears)),
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(elder, wizard)
                )
            )
        )
        result.isSuccess shouldBe false
    }

    test("Wizard ability prevents all damage target spell would deal") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Set up: Shieldmage Elder + Wizard on active player's side
        val elder = driver.putCreatureOnBattlefield(activePlayer, "Shieldmage Elder")
        val wizard = driver.putCreatureOnBattlefield(activePlayer, "Test Wizard")

        // Opponent casts Lightning Bolt targeting active player
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)

        // Pass priority to opponent
        driver.passPriority(activePlayer)

        val castResult = driver.castSpell(opponent, bolt, listOf(activePlayer))
        castResult.isSuccess shouldBe true

        // Opponent has priority after casting - pass to give active player priority
        driver.passPriority(opponent)

        // Now active player has priority with bolt on stack - activate Wizard ability
        val boltOnStack = driver.getTopOfStack()!!

        val activateResult = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = elder,
                abilityId = wizardAbilityId,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(elder, wizard)
                )
            )
        )
        activateResult.isSuccess shouldBe true

        // Resolve the prevention ability first
        driver.bothPass()

        // Now resolve the Lightning Bolt
        driver.bothPass()

        // Damage should be prevented
        driver.getLifeTotal(activePlayer) shouldBe 20
    }
})
