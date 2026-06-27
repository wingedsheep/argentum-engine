package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.LeylineOfResonance
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Leyline of Resonance {2}{R}{R} — Enchantment.
 * "Whenever you cast an instant or sorcery spell that targets only a single creature you control,
 *  copy that spell. You may choose new targets for the copy."
 *
 * "Targets only a single creature you control" is composed from two existing primitives:
 * `SpellCastPredicate.TargetsMatching(Creature.youControl())` (at least one chosen target is a
 * creature you control) plus `Conditions.TriggeringSpellHasSingleTarget` (exactly one target) —
 * together meaning the spell's one and only target is a creature you control.
 *
 * These tests pin that composition by checking whether the copy trigger
 * ([CopyTargetSpellEffect]) lands on the stack on cast. Lightning Bolt is the probe: an
 * instant with a single "any target" requirement, so retargeting it across creature-you-control,
 * creature-you-don't-control, and a player exercises every arm of the filter.
 */
class LeylineOfResonanceScenarioTest : FunSpec({

    fun copyTriggers(driver: GameTestDriver) =
        driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.effect is CopyTargetSpellEffect }

    fun setup(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LeylineOfResonance))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Leyline of Resonance")
        return Triple(driver, you, opponent)
    }

    test("fires when you cast an instant targeting a single creature you control") {
        val (driver, you, _) = setup()
        val myCreature = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")

        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Permanent(myCreature))).error shouldBe null

        copyTriggers(driver).size shouldBe 1
    }

    test("does NOT fire when the spell targets a creature you don't control") {
        val (driver, you, opponent) = setup()
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")

        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Permanent(theirCreature))).error shouldBe null

        copyTriggers(driver).size shouldBe 0
    }

    test("does NOT fire when the spell targets a player rather than a creature") {
        val (driver, you, opponent) = setup()
        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")

        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null

        copyTriggers(driver).size shouldBe 0
    }
})
