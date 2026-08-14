package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Clone Saga chapter III — "Choose a card name. Whenever a creature with the chosen name deals
 * combat damage to a player this turn, draw a card." Pins the `bakeChosenValuesIntoTrigger`
 * extension that bakes the chosen card name into a delayed combat-damage trigger's source filter.
 *
 * Exercised in isolation via an inline sorcery that runs the same effect (ChooseCardName →
 * CreateDelayedTrigger over `Creature.namedFromVariable`), so it doesn't need the Saga to advance
 * three chapters.
 */
class TheCloneSagaChapterThreeTest : FunSpec({

    // "Choose a card name. Whenever a creature with the chosen name deals combat damage to a player
    // this turn, draw a card." — the Saga's chapter III effect, as a one-shot sorcery.
    val chapterThree = CardDefinition(
        name = "Chosen Name Draw Test",
        manaCost = ManaCost.parse("{U}"),
        typeLine = TypeLine.parse("Sorcery"),
        oracleText = "",
        script = CardScript.spell(
            effect = Effects.Composite(
                Effects.ChooseCardName(storeAs = "clonedName"),
                CreateDelayedTriggerEffect(
                    trigger = Triggers.dealsDamage(
                        damageType = DamageType.Combat,
                        recipient = RecipientFilter.AnyPlayer,
                        sourceFilter = GameObjectFilter.Creature.namedFromVariable("clonedName"),
                        binding = TriggerBinding.ANY,
                    ),
                    effect = Effects.DrawCards(1),
                    fireOnce = false,
                    expiry = DelayedTriggerExpiry.EndOfTurn,
                ),
            )
        )
    )

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(chapterThree))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("a creature with the chosen name dealing combat damage to a player draws a card") {
        val (driver, you, opponent) = newGame()

        // Attacker: a Savannah Lions we will name with chapter III.
        val lions = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        driver.removeSummoningSickness(lions)

        // Cast the chapter-III sorcery and choose "Savannah Lions".
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.BLUE, 1)
        val spell = driver.putCardInHand(you, "Chosen Name Draw Test")
        driver.castSpell(you, spell)
        settle(driver)
        val pick = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(you, OptionChosenResponse(pick.id, pick.options.indexOf("Savannah Lions")))
        settle(driver)

        // Attack the opponent with the Lions and deal combat damage.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(lions), opponent)
        val handBeforeDamage = driver.getHandSize(you)
        driver.declareNoBlockers(opponent)
        // Advance through the combat-damage step, resolving the chapter-III draw trigger.
        var guard = 0
        while (guard++ < 40 && driver.state.step != Step.POSTCOMBAT_MAIN) {
            if (driver.isPaused) break
            driver.bothPass()
        }
        settle(driver)

        // The delayed chapter-III trigger drew a card when the Lions dealt combat damage.
        driver.getHandSize(you) shouldBe handBeforeDamage + 1
    }
})
