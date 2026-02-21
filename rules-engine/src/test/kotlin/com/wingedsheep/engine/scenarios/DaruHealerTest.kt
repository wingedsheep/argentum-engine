package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.PreventNextDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for Daru Healer.
 *
 * Daru Healer: {2}{W}
 * Creature — Human Cleric
 * 1/2
 * {T}: Prevent the next 1 damage that would be dealt to any target this turn.
 * Morph {W}
 */
class DaruHealerTest : FunSpec({

    val healerAbilityId = AbilityId(UUID.randomUUID().toString())

    val DaruHealer = CardDefinition.creature(
        name = "Daru Healer",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric")),
        power = 1,
        toughness = 2,
        oracleText = "{T}: Prevent the next 1 damage that would be dealt to any target this turn.",
        script = CardScript.permanent(
            ActivatedAbility(
                id = healerAbilityId,
                cost = AbilityCost.Tap,
                effect = PreventNextDamageEffect(
                    amount = DynamicAmount.Fixed(1),
                    target = EffectTarget.BoundVariable("target")
                ),
                targetRequirement = AnyTarget(id = "target")
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

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DaruHealer, HillGiant))
        return driver
    }

    test("prevents 1 damage to target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val healer = driver.putCreatureOnBattlefield(activePlayer, "Daru Healer")
        val target = driver.putCreatureOnBattlefield(activePlayer, "Hill Giant")
        driver.removeSummoningSickness(healer)

        // Activate healer's ability targeting the Hill Giant
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = healer,
                abilityId = healerAbilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Deal 3 damage via Lightning Bolt - shield prevents 1, 2 gets through
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(target)))
        driver.bothPass()

        val damage = driver.state.getEntity(target)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 2
    }

    test("prevents 1 damage to target player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val healer = driver.putCreatureOnBattlefield(activePlayer, "Daru Healer")
        driver.removeSummoningSickness(healer)

        // Activate healer's ability targeting the opponent
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = healer,
                abilityId = healerAbilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Deal 3 damage to opponent via Lightning Bolt - shield prevents 1, so 2 gets through
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("shield is consumed after one use") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val healer = driver.putCreatureOnBattlefield(activePlayer, "Daru Healer")
        driver.removeSummoningSickness(healer)

        // Activate healer's ability targeting the opponent
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = healer,
                abilityId = healerAbilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // First bolt: 3 damage, shield prevents 1, 2 gets through
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt1 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt1, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18

        // Second bolt: full 3 damage, no shield remaining
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt2 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt2, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 15
    }
})
