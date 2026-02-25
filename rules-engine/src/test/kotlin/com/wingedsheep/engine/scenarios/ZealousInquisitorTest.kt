package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.ZealousInquisitor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Zealous Inquisitor's partial damage redirection.
 *
 * Zealous Inquisitor: {2}{W}
 * Creature — Human Cleric
 * 2/2
 * {1}{W}: The next 1 damage that would be dealt to this creature this turn
 * is dealt to target creature instead.
 */
class ZealousInquisitorTest : FunSpec({

    val abilityId = ZealousInquisitor.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("redirects 1 damage from 3 damage bolt - Inquisitor dies, target takes 1") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val inquisitor = driver.putCreatureOnBattlefield(p1, "Zealous Inquisitor")
        driver.removeSummoningSickness(inquisitor)
        val hillGiant = driver.putCreatureOnBattlefield(p2, "Hill Giant")

        // Activate ability targeting opponent's Hill Giant
        driver.giveMana(p1, Color.WHITE, 2)
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = inquisitor,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(hillGiant))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Cast Lightning Bolt at Inquisitor (3 damage)
        driver.giveMana(p1, Color.RED, 1)
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Permanent(inquisitor)))
        driver.bothPass()

        // Inquisitor takes 2 damage (3 - 1 redirected) on a 2/2 = lethal, dies to SBA
        driver.getGraveyardCardNames(p1) shouldContain "Zealous Inquisitor"

        // Hill Giant should take 1 redirected damage
        val hillGiantDamage = driver.state.getEntity(hillGiant)?.get<DamageComponent>()?.amount ?: 0
        hillGiantDamage shouldBe 1

        // Shield should be consumed
        driver.state.floatingEffects.any {
            it.effect.modification is SerializableModification.RedirectNextDamage
        } shouldBe false
    }

    test("redirects all damage when damage is exactly 1") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val inquisitor = driver.putCreatureOnBattlefield(p1, "Zealous Inquisitor")
        driver.removeSummoningSickness(inquisitor)
        val hillGiant = driver.putCreatureOnBattlefield(p2, "Hill Giant")

        // Activate ability targeting Hill Giant
        driver.giveMana(p1, Color.WHITE, 2)
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = inquisitor,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(hillGiant))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Cast Spark Spray at Inquisitor (1 damage)
        driver.giveMana(p1, Color.RED, 1)
        val spray = driver.putCardInHand(p1, "Spark Spray")
        driver.castSpellWithTargets(p1, spray, listOf(ChosenTarget.Permanent(inquisitor)))
        driver.bothPass()

        // Inquisitor should take 0 damage (all redirected)
        val inquisitorDamage = driver.state.getEntity(inquisitor)?.get<DamageComponent>()?.amount ?: 0
        inquisitorDamage shouldBe 0

        // Hill Giant should take 1 redirected damage
        val hillGiantDamage = driver.state.getEntity(hillGiant)?.get<DamageComponent>()?.amount ?: 0
        hillGiantDamage shouldBe 1
    }

    test("multiple activations stack - redirect 2 from a single damage event") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val inquisitor = driver.putCreatureOnBattlefield(p1, "Zealous Inquisitor")
        driver.removeSummoningSickness(inquisitor)
        val hillGiant = driver.putCreatureOnBattlefield(p2, "Hill Giant")

        // Activate ability twice targeting same creature
        driver.giveMana(p1, Color.WHITE, 4)
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = inquisitor,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(hillGiant))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = inquisitor,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(hillGiant))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Two shields should be active
        driver.state.floatingEffects.count {
            it.effect.modification is SerializableModification.RedirectNextDamage
        } shouldBe 2

        // Cast Lightning Bolt at Inquisitor (3 damage)
        driver.giveMana(p1, Color.RED, 1)
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Permanent(inquisitor)))
        driver.bothPass()

        // First shield redirects 1, then remaining 2 hits Inquisitor via recursive call,
        // second shield activates on that 2 and redirects 1 more.
        // So Inquisitor takes 1 damage, Hill Giant takes 2.
        val inquisitorDamage = driver.state.getEntity(inquisitor)?.get<DamageComponent>()?.amount ?: 0
        inquisitorDamage shouldBe 1

        val hillGiantDamage = driver.state.getEntity(hillGiant)?.get<DamageComponent>()?.amount ?: 0
        hillGiantDamage shouldBe 2
    }
})
