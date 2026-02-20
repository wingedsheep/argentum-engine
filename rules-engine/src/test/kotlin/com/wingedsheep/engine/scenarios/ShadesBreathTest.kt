package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.ChangeGroupColorEffect
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityToGroupEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.SetGroupCreatureSubtypesEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Shade's Breath (ONS).
 *
 * {1}{B} Instant
 * Until end of turn, each creature you control becomes a black Shade and gains
 * "{B}: This creature gets +1/+1 until end of turn."
 */
class ShadesBreathTest : FunSpec({

    val ShadesBreath = CardDefinition.instant(
        name = "Shade's Breath",
        manaCost = ManaCost.parse("{1}{B}"),
        oracleText = "Until end of turn, each creature you control becomes a black Shade and gains \"{B}: This creature gets +1/+1 until end of turn.\"",
        script = CardScript.spell(
            effect = SetGroupCreatureSubtypesEffect(
                subtypes = setOf("Shade")
            ) then ChangeGroupColorEffect(
                colors = setOf("BLACK")
            ) then GrantActivatedAbilityToGroupEffect(
                ability = ActivatedAbility(
                    id = AbilityId.generate(),
                    cost = AbilityCost.Mana(ManaCost.parse("{B}")),
                    effect = ModifyStatsEffect(
                        powerModifier = 1,
                        toughnessModifier = 1,
                        target = EffectTarget.Self,
                        duration = Duration.EndOfTurn
                    )
                )
            )
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ShadesBreath))
        return driver
    }

    test("Shade's Breath makes creatures into black Shades") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Verify base creature type is Bear
        val baseCard = driver.state.getEntity(bears)!!.get<CardComponent>()!!
        baseCard.typeLine.subtypes.map { it.value } shouldBe listOf("Bear")

        // Cast Shade's Breath
        val spell = driver.putCardInHand(activePlayer, "Shade's Breath")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Creature type should be changed to Shade in projected state
        val projected = projector.project(driver.state)
        projected.getSubtypes(bears) shouldBe setOf("Shade")

        // Creature should be black in projected state
        projected.getColors(bears) shouldBe setOf("BLACK")
    }

    test("Shade's Breath grants activated ability to pump") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Cast Shade's Breath
        val spell = driver.putCardInHand(activePlayer, "Shade's Breath")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // The creature should have a granted activated ability
        val grants = driver.state.grantedActivatedAbilities.filter { it.entityId == bears }
        grants.size shouldBe 1

        // Activate the granted ability: {B}: +1/+1
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = bears,
                abilityId = grants[0].ability.id
            )
        )
        driver.bothPass()

        // Bears should now be 3/3
        projector.getProjectedPower(driver.state, bears) shouldBe 3
        projector.getProjectedToughness(driver.state, bears) shouldBe 3
    }

    test("Shade's Breath does not affect opponent's creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on both sides
        val myBears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Cast Shade's Breath
        val spell = driver.putCardInHand(activePlayer, "Shade's Breath")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // My creature should be a Shade
        val projected = projector.project(driver.state)
        projected.getSubtypes(myBears) shouldBe setOf("Shade")

        // Opponent's creature should still be a Bear
        projected.getSubtypes(theirBears) shouldBe setOf("Bear")

        // Only my creature should have the granted ability
        val myGrants = driver.state.grantedActivatedAbilities.filter { it.entityId == myBears }
        val theirGrants = driver.state.grantedActivatedAbilities.filter { it.entityId == theirBears }
        myGrants.size shouldBe 1
        theirGrants.size shouldBe 0
    }

    test("Shade's Breath affects multiple creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two creatures on the battlefield
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")

        // Cast Shade's Breath
        val spell = driver.putCardInHand(activePlayer, "Shade's Breath")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Both creatures should be Shades
        val projected = projector.project(driver.state)
        projected.getSubtypes(bears) shouldBe setOf("Shade")
        projected.getSubtypes(lions) shouldBe setOf("Shade")

        // Both should have the granted ability
        val grants = driver.state.grantedActivatedAbilities
        grants.filter { it.entityId == bears }.size shouldBe 1
        grants.filter { it.entityId == lions }.size shouldBe 1
    }
})
