package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.PendingSpellCopy
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies that a pending spell-copy (CopyNextSpellCastEffect / CopyEachSpellCastEffect) honours its
 * parameterized [PendingSpellCopy.spellFilter] rather than the previously-hardcoded instant/sorcery
 * gate (SDK quality audit item #8).
 *
 *  - default filter (instant or sorcery) copies an instant cast
 *  - a creature filter ignores an instant cast (entry not consumed)
 *  - a creature filter matches a creature spell (entry consumed)
 */
class CopySpellCastFilterTest : FunSpec({

    fun stormCopyTriggers(driver: GameTestDriver) =
        driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.effect is StormCopyEffect }

    test("default instant-or-sorcery filter copies an instant and is consumed") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.putLandOnBattlefield(caster, "Mountain")
        driver.replaceState(
            driver.state.copy(
                pendingSpellCopies = listOf(
                    PendingSpellCopy(
                        controllerId = caster,
                        copies = 1,
                        sourceId = caster,
                        sourceName = "Test Source",
                        spellFilter = GameObjectFilter.InstantOrSorcery
                    )
                )
            )
        )

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpell(caster, bolt, listOf(opponent)).isSuccess shouldBe true

        stormCopyTriggers(driver).size shouldBe 1
        driver.state.pendingSpellCopies.size shouldBe 0
    }

    test("creature filter ignores an instant cast and is not consumed") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.putLandOnBattlefield(caster, "Mountain")
        driver.replaceState(
            driver.state.copy(
                pendingSpellCopies = listOf(
                    PendingSpellCopy(
                        controllerId = caster,
                        copies = 1,
                        sourceId = caster,
                        sourceName = "Test Source",
                        spellFilter = GameObjectFilter.Creature
                    )
                )
            )
        )

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpell(caster, bolt, listOf(opponent)).isSuccess shouldBe true

        stormCopyTriggers(driver).size shouldBe 0
        driver.state.pendingSpellCopies.size shouldBe 1
    }

    test("creature filter matches a creature spell and is consumed") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!

        repeat(3) { driver.putLandOnBattlefield(caster, "Forest") }
        driver.replaceState(
            driver.state.copy(
                pendingSpellCopies = listOf(
                    PendingSpellCopy(
                        controllerId = caster,
                        copies = 1,
                        sourceId = caster,
                        sourceName = "Test Source",
                        spellFilter = GameObjectFilter.Creature
                    )
                )
            )
        )

        val centaur = driver.putCardInHand(caster, "Centaur Courser")
        driver.castSpell(caster, centaur).isSuccess shouldBe true

        driver.state.pendingSpellCopies.size shouldBe 0
    }
})
