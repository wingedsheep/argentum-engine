package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.OnBlock
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * Tests for Jareth, Leonine Titan.
 *
 * Jareth, Leonine Titan: {3}{W}{W}{W}
 * Legendary Creature — Cat Giant
 * 4/7
 * Whenever Jareth, Leonine Titan blocks, it gets +7/+7 until end of turn.
 * {W}: Jareth gains protection from the color of your choice until end of turn.
 */
class JarethLeonineTitanTest : FunSpec({

    val protectionAbilityId = AbilityId(UUID.randomUUID().toString())

    val JarethLeonineTitan = CardDefinition.creature(
        name = "Jareth, Leonine Titan",
        manaCost = ManaCost.parse("{3}{W}{W}{W}"),
        subtypes = setOf(Subtype("Cat"), Subtype("Giant")),
        power = 4,
        toughness = 7,
        oracleText = "Whenever Jareth, Leonine Titan blocks, it gets +7/+7 until end of turn.\n{W}: Jareth gains protection from the color of your choice until end of turn.",
        script = CardScript.permanent(
            ActivatedAbility(
                id = protectionAbilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{W}")),
                effect = Effects.ChooseColorAndGrantProtectionToTarget(EffectTarget.Self)
            ),
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = OnBlock(selfOnly = true),
                    effect = ModifyStatsEffect(7, 7, EffectTarget.Self)
                )
            )
        ),
        supertypes = setOf(Supertype.LEGENDARY)
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(JarethLeonineTitan))
        return driver
    }

    test("Jareth gets +7/+7 when blocking") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        // Put Jareth on player 2's battlefield (defender)
        val jareth = driver.putCreatureOnBattlefield(player2, "Jareth, Leonine Titan")
        driver.removeSummoningSickness(jareth)

        // Put attacker on player 1's battlefield (Centaur Courser is 3/3)
        val attacker = driver.putCreatureOnBattlefield(player1, "Centaur Courser")
        driver.removeSummoningSickness(attacker)

        // Base stats should be 4/7
        projector.getProjectedPower(driver.state, jareth) shouldBe 4
        projector.getProjectedToughness(driver.state, jareth) shouldBe 7

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with Centaur Courser
        driver.declareAttackers(player1, listOf(attacker), player2)
        driver.bothPass()

        // Block with Jareth
        driver.declareBlockers(player2, mapOf(jareth to listOf(attacker)))

        // Block trigger fires and goes on stack. Both pass to resolve.
        driver.bothPass()

        // Jareth should now be 11/14 (+7/+7 from block trigger)
        projector.getProjectedPower(driver.state, jareth) shouldBe 11
        projector.getProjectedToughness(driver.state, jareth) shouldBe 14

        // Advance through combat - Centaur Courser (3/3) dies, Jareth survives
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Jareth should still be alive
        driver.findPermanent(player2, "Jareth, Leonine Titan") shouldNotBe null

        // Centaur Courser should be dead
        driver.findPermanent(player1, "Centaur Courser") shouldBe null
    }

    test("Jareth gains protection from chosen color via activated ability") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!

        // Put Jareth on the battlefield
        val jareth = driver.putCreatureOnBattlefield(player1, "Jareth, Leonine Titan")
        driver.removeSummoningSickness(jareth)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana to activate the ability
        driver.giveMana(player1, Color.WHITE, 1)

        // Activate protection ability
        val result = driver.submit(
            ActivateAbility(
                playerId = player1,
                sourceId = jareth,
                abilityId = protectionAbilityId
            )
        )
        result.isSuccess shouldBe true

        // Ability goes on stack, both pass to resolve
        driver.bothPass()

        // Should have a color choice decision
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()

        val decision = driver.pendingDecision as ChooseColorDecision

        // Choose red
        driver.submitDecision(player1, ColorChosenResponse(decision.id, Color.RED))

        // Jareth should now have protection from red
        val projected = projector.project(driver.state)
        projected.hasKeyword(jareth, "PROTECTION_FROM_RED") shouldBe true
    }

    test("Jareth's protection prevents damage from chosen color") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        // Put Jareth on player 2's battlefield
        val jareth = driver.putCreatureOnBattlefield(player2, "Jareth, Leonine Titan")
        driver.removeSummoningSickness(jareth)

        // Give player 2 white mana and activate protection from red
        driver.giveMana(player2, Color.WHITE, 1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Pass priority to player 2
        driver.passPriority(player1)

        // Activate protection from red
        driver.submit(
            ActivateAbility(
                playerId = player2,
                sourceId = jareth,
                abilityId = protectionAbilityId
            )
        )

        // Both pass to resolve the ability
        driver.bothPass()

        // Choose red
        val decision = driver.pendingDecision as ChooseColorDecision
        driver.submitDecision(player2, ColorChosenResponse(decision.id, Color.RED))

        // Jareth should have protection from red
        val projected = projector.project(driver.state)
        projected.hasKeyword(jareth, "PROTECTION_FROM_RED") shouldBe true

        // Now player 1 casts Lightning Bolt targeting Jareth
        val bolt = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt, listOf(jareth))

        // Both pass to resolve
        driver.bothPass()

        // Jareth should still be on the battlefield (damage prevented by protection)
        driver.findPermanent(player2, "Jareth, Leonine Titan") shouldNotBe null
    }

    test("Jareth's block bonus wears off at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        // Put Jareth on player 2's battlefield
        val jareth = driver.putCreatureOnBattlefield(player2, "Jareth, Leonine Titan")
        driver.removeSummoningSickness(jareth)

        // Put a small attacker (Grizzly Bears is 2/2)
        val attacker = driver.putCreatureOnBattlefield(player1, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack
        driver.declareAttackers(player1, listOf(attacker), player2)
        driver.bothPass()

        // Block with Jareth
        driver.declareBlockers(player2, mapOf(jareth to listOf(attacker)))

        // Trigger resolves
        driver.bothPass()

        // Jareth should be 11/14
        projector.getProjectedPower(driver.state, jareth) shouldBe 11
        projector.getProjectedToughness(driver.state, jareth) shouldBe 14

        // Pass through combat and to next turn's upkeep
        driver.passPriorityUntil(Step.UPKEEP)

        // Bonus should have worn off - back to 4/7
        projector.getProjectedPower(driver.state, jareth) shouldBe 4
        projector.getProjectedToughness(driver.state, jareth) shouldBe 7
    }
})
