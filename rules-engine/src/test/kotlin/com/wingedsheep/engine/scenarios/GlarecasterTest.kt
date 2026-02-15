package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.RedirectNextDamageEffect
import com.wingedsheep.sdk.targeting.AnyTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for Glarecaster and the RedirectNextDamage mechanic.
 *
 * Glarecaster: {4}{W}{W}
 * Creature — Bird Cleric
 * 3/3
 * Flying
 * {5}{W}: The next time damage would be dealt to Glarecaster and/or you this turn,
 * that damage is dealt to any target instead.
 */
class GlarecasterTest : FunSpec({

    val abilityId = AbilityId(UUID.randomUUID().toString())

    val Glarecaster = CardDefinition.creature(
        name = "Glarecaster",
        manaCost = ManaCost.parse("{4}{W}{W}"),
        subtypes = setOf(Subtype("Bird"), Subtype("Cleric")),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\n{5}{W}: The next time damage would be dealt to Glarecaster and/or you this turn, that damage is dealt to any target instead.",
        script = CardScript.permanent(
            ActivatedAbility(
                id = abilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{5}{W}")),
                effect = RedirectNextDamageEffect(
                    protectedTargets = listOf(EffectTarget.Self, EffectTarget.Controller),
                    redirectTo = EffectTarget.ContextTarget(0)
                ),
                targetRequirement = AnyTarget()
            )
        )
    )

    val HillGiant = CardDefinition.creature(
        name = "Hill Giant",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype("Giant")),
        power = 3,
        toughness = 3,
        oracleText = ""
    )

    val BigCreature = CardDefinition.creature(
        name = "Big Creature",
        manaCost = ManaCost.parse("{4}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 5,
        toughness = 5,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Glarecaster, HillGiant, BigCreature))
        return driver
    }

    test("redirect spell damage to player - damage aimed at you goes to opponent's creature instead") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glarecaster = driver.putCreatureOnBattlefield(p1, "Glarecaster")
        driver.removeSummoningSickness(glarecaster)
        val bigCreature = driver.putCreatureOnBattlefield(p2, "Big Creature")

        // P1 activates Glarecaster's ability targeting P2's creature
        driver.giveMana(p1, Color.WHITE, 6)
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = glarecaster,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bigCreature))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Verify shield is active
        driver.state.floatingEffects.any {
            it.effect.modification is SerializableModification.RedirectNextDamage
        } shouldBe true

        // P1 casts Lightning Bolt targeting themselves (to simulate damage to the protected player)
        driver.giveMana(p1, Color.RED, 1)
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Player(p1)))
        driver.bothPass()

        // Damage should be redirected to Big Creature instead of P1
        val p1Life = driver.state.getEntity(p1)?.get<LifeTotalComponent>()?.life ?: 0
        p1Life shouldBe 20 // No damage to P1

        val creatureDamage = driver.state.getEntity(bigCreature)?.get<DamageComponent>()?.amount ?: 0
        creatureDamage shouldBe 3 // Redirected damage
    }

    test("redirect damage to Glarecaster itself - damage aimed at creature goes to chosen target") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glarecaster = driver.putCreatureOnBattlefield(p1, "Glarecaster")
        driver.removeSummoningSickness(glarecaster)
        val bigCreature = driver.putCreatureOnBattlefield(p2, "Big Creature")

        // Activate ability targeting opponent's creature
        driver.giveMana(p1, Color.WHITE, 6)
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = glarecaster,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bigCreature))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // P1 casts Lightning Bolt targeting their own Glarecaster
        driver.giveMana(p1, Color.RED, 1)
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Permanent(glarecaster)))
        driver.bothPass()

        // Damage should be redirected to Big Creature instead of Glarecaster
        val glarecasterDamage = driver.state.getEntity(glarecaster)?.get<DamageComponent>()?.amount ?: 0
        glarecasterDamage shouldBe 0 // No damage to Glarecaster

        val creatureDamage = driver.state.getEntity(bigCreature)?.get<DamageComponent>()?.amount ?: 0
        creatureDamage shouldBe 3 // Redirected damage
    }

    test("one-shot - only first damage is redirected, subsequent damage hits normally") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glarecaster = driver.putCreatureOnBattlefield(p1, "Glarecaster")
        driver.removeSummoningSickness(glarecaster)
        val hillGiant = driver.putCreatureOnBattlefield(p2, "Hill Giant")

        // Activate ability targeting opponent's Hill Giant
        driver.giveMana(p1, Color.WHITE, 6)
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = glarecaster,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(hillGiant))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // First bolt at P1 - should be redirected
        driver.giveMana(p1, Color.RED, 1)
        val bolt1 = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt1, listOf(ChosenTarget.Player(p1)))
        driver.bothPass()

        // Shield should be consumed
        driver.state.floatingEffects.any {
            it.effect.modification is SerializableModification.RedirectNextDamage
        } shouldBe false

        // Second bolt at P1 - hits normally
        driver.giveMana(p1, Color.RED, 1)
        val bolt2 = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt2, listOf(ChosenTarget.Player(p1)))
        driver.bothPass()

        // P1 should have taken only the second bolt
        val p1Life = driver.state.getEntity(p1)?.get<LifeTotalComponent>()?.life ?: 0
        p1Life shouldBe 17 // 20 - 3 from second bolt
    }

    test("redirect damage to opponent player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glarecaster = driver.putCreatureOnBattlefield(p1, "Glarecaster")
        driver.removeSummoningSickness(glarecaster)

        // Activate ability targeting P2 (the opponent player)
        driver.giveMana(p1, Color.WHITE, 6)
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = glarecaster,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(p2))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // P1 casts Lightning Bolt targeting themselves
        driver.giveMana(p1, Color.RED, 1)
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Player(p1)))
        driver.bothPass()

        // Damage should be redirected to P2
        val p1Life = driver.state.getEntity(p1)?.get<LifeTotalComponent>()?.life ?: 0
        p1Life shouldBe 20 // No damage to P1

        val p2Life = driver.state.getEntity(p2)?.get<LifeTotalComponent>()?.life ?: 0
        p2Life shouldBe 17 // 20 - 3 redirected damage
    }

    test("shield expires at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glarecaster = driver.putCreatureOnBattlefield(p1, "Glarecaster")
        driver.removeSummoningSickness(glarecaster)

        // Activate ability targeting P2
        driver.giveMana(p1, Color.WHITE, 6)
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = glarecaster,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(p2))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Shield should be active
        driver.state.floatingEffects.any {
            it.effect.modification is SerializableModification.RedirectNextDamage
        } shouldBe true

        // Pass to cleanup step (EndOfTurn floating effects are removed during cleanup)
        driver.passPriorityUntil(Step.CLEANUP)

        // Shield should be gone
        driver.state.floatingEffects.any {
            it.effect.modification is SerializableModification.RedirectNextDamage
        } shouldBe false
    }
})
