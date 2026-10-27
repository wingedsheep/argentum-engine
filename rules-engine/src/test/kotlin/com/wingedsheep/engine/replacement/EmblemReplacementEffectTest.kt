package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.player.CreatePermanentEmblemExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LifeLossFloor
import com.wingedsheep.sdk.scripting.effects.CreatePermanentEmblemEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Replacement effects held by an emblem (`CreatePermanentEmblem(ownedReplacementEffects = …)`):
 * an emblem is in no zone the battlefield scan sees (CR 114.1), yet its abilities function like a
 * permanent's, controlled by the player who got it (CR 114.2).
 *
 * Pinned through the emblem Serra the Benevolent's −6 makes — "If you control a creature, damage
 * that would reduce your life total to less than 1 reduces it to 1 instead." — and that card's
 * rulings: it changes only the result of damage (lifelink still sees the full damage), not "you
 * lose life" effects; it doesn't stop a life total already under 1 from dropping further; and in
 * Two-Headed Giant it guards the shared team total whichever teammate is dealt the damage.
 */
class EmblemReplacementEffectTest : FunSpec({

    val floorEmblem = Effects.CreatePermanentEmblem(
        ownedReplacementEffects = listOf(
            LifeLossFloor(
                floor = 1,
                restrictions = listOf(Conditions.YouControl(GameObjectFilter.Creature)),
                appliesTo = EventPattern.LifeLossEvent(player = Player.You),
            )
        ),
        emblemDescription = "If you control a creature, damage that would reduce your life total to less than 1 reduces it to 1 instead.",
    ) as CreatePermanentEmblemEffect

    val loseFiveLife = CardDefinition.sorcery(
        name = "Drain Five",
        manaCost = ManaCost.parse("{B}"),
        oracleText = "Target player loses 5 life.",
        script = CardScript.spell(
            effect = LoseLifeEffect(5, EffectTarget.BoundVariable("target")),
            TargetPlayer(id = "target"),
        ),
    )

    val lifelinkBrute = CardDefinition.creature(
        name = "Lifelink Brute",
        manaCost = ManaCost.parse("{4}{W}"),
        subtypes = setOf(Subtype("Ox")),
        power = 5,
        toughness = 5,
        oracleText = "Lifelink",
        keywords = setOf(Keyword.LIFELINK),
    )

    fun GameState.withEmblem(controller: EntityId): GameState =
        CreatePermanentEmblemExecutor().execute(this, floorEmblem, EffectContext(sourceId = null, controllerId = controller)).state

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(loseFiveLife, lifelinkBrute))
        initMirrorMatch(deck = Deck.of("Mountain" to 20, "Swamp" to 20), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bolt(caster: EntityId, target: EntityId) {
        giveMana(caster, Color.RED, 1)
        val bolt = putCardInHand(caster, "Lightning Bolt")
        castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Player(target))).error shouldBe null
        bothPass()
    }

    test("the emblem's floor holds lethal damage at 1 with no battlefield source") {
        val driver = driver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Savannah Lions")
        driver.replaceState(driver.state.withEmblem(you))
        driver.setLifeTotal(you, 3)

        driver.bolt(you, you)

        driver.assertLifeTotal(you, 1)
    }

    test("without a creature the emblem's condition fails and damage applies in full") {
        val driver = driver()
        val you = driver.activePlayer!!
        driver.replaceState(driver.state.withEmblem(you))
        driver.setLifeTotal(you, 3)

        driver.bolt(you, you)

        driver.assertLifeTotal(you, 0)
    }

    test("the emblem guards only its controller") {
        val driver = driver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(you, "Savannah Lions")
        driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        driver.replaceState(driver.state.withEmblem(you))
        driver.setLifeTotal(opponent, 3)

        driver.bolt(you, opponent)

        driver.assertLifeTotal(opponent, 0)
    }

    test("'you lose life' effects aren't damage and ignore the floor") {
        val driver = driver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Savannah Lions")
        driver.replaceState(driver.state.withEmblem(you))
        driver.setLifeTotal(you, 3)

        driver.giveMana(you, Color.BLACK, 1)
        val drain = driver.putCardInHand(you, "Drain Five")
        driver.castSpellWithTargets(you, drain, listOf(ChosenTarget.Player(you))).error shouldBe null
        driver.bothPass()

        driver.assertLifeTotal(you, -2)
    }

    test("the damage is still dealt in full: lifelink gains the whole amount") {
        val driver = driver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        driver.replaceState(driver.state.withEmblem(opponent))
        driver.setLifeTotal(opponent, 2)
        driver.putCreatureOnBattlefield(you, "Lifelink Brute")
        driver.removeSummoningSickness(driver.findPermanent(you, "Lifelink Brute")!!)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(driver.findPermanent(you, "Lifelink Brute")!!), opponent).error shouldBe null
        driver.passPriorityUntil(Step.END_COMBAT)

        driver.assertLifeTotal(opponent, 1)
        driver.assertLifeTotal(you, 25)
    }

    test("a life total already under the floor drops further") {
        val driver = driver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val state = driver.state.withEmblem(you)
        val evaluator = PredicateEvaluator(cardRegistry = null)

        DamageUtils.applyLifeLossFloors(state, you, currentLife = 0, amount = 3, predicateEvaluator = evaluator) shouldBe 3
        DamageUtils.applyLifeLossFloors(state, you, currentLife = 1, amount = 3, predicateEvaluator = evaluator) shouldBe 0
        DamageUtils.applyLifeLossFloors(state, you, currentLife = 5, amount = 9, predicateEvaluator = evaluator) shouldBe 4
    }

    test("Two-Headed Giant: the floor guards the team total when the teammate is dealt the damage") {
        val driver = GameTestDriver().apply { registerCards(TestCards.all) }
        val (p1, p2, p3, _) = driver.initMultiplayer(
            decks = List(4) { Deck.of("Mountain" to 40) },
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        driver.putCreatureOnBattlefield(p3, "Savannah Lions")
        val state = driver.state.withEmblem(p1)
        val evaluator = PredicateEvaluator(cardRegistry = null)

        withClue("teammate dealt damage — the emblem controller's creature is what counts") {
            DamageUtils.applyLifeLossFloors(state, p2, currentLife = 3, amount = 5, predicateEvaluator = evaluator) shouldBe 2
        }
        withClue("an opponent isn't guarded") {
            DamageUtils.applyLifeLossFloors(state, p3, currentLife = 3, amount = 5, predicateEvaluator = evaluator) shouldBe 5
        }
    }

    test("the emblem's replacement is listed among the active replacements, controlled by its owner") {
        val driver = driver()
        val you = driver.activePlayer!!
        ActiveReplacements.all(driver.state).filter { it.effect is LifeLossFloor }.shouldBeEmpty()

        val active = ActiveReplacements.all(driver.state.withEmblem(you)).single { it.effect is LifeLossFloor }

        active.controllerId shouldBe you
        active.granted shouldBe false
    }
})
