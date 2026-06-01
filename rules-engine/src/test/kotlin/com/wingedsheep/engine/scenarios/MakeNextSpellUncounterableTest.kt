package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for the one-shot "next spell can't be countered" rider
 * ([com.wingedsheep.sdk.scripting.effects.MakeNextSpellUncounterableEffect]).
 *
 * Mistrise Village (CHK): Land — "{U}, {T}: The next spell you cast this turn can't be countered."
 *
 * Unlike Domri's duration-based [GrantSpellsCantBeCountered] (every matching spell this turn), this
 * protects only the *next* matching spell cast, then is consumed.
 */
class MakeNextSpellUncounterableTest : FunSpec({

    // Mistrise Village: "{U}, {T}: The next spell you cast this turn can't be countered."
    val mistriseVillage = card("Mistrise Village") {
        typeLine = "Land"
        activatedAbility {
            cost = AbilityCost.Composite(listOf(AbilityCost.Mana(ManaCost.parse("{U}")), AbilityCost.Tap))
            effect = Effects.MakeNextSpellUncounterable()
        }
    }
    val mistriseAbilityId = mistriseVillage.activatedAbilities.first().id

    // A test land that only protects the next *creature* spell, to exercise the spellFilter.
    val creatureOnlyVillage = card("Creature Village") {
        typeLine = "Land"
        activatedAbility {
            cost = AbilityCost.Composite(listOf(AbilityCost.Mana(ManaCost.parse("{U}")), AbilityCost.Tap))
            effect = Effects.MakeNextSpellUncounterable(GameObjectFilter.Creature)
        }
    }
    val creatureOnlyAbilityId = creatureOnlyVillage.activatedAbilities.first().id

    val testBear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )
    val secondBear = CardDefinition.creature(
        name = "Second Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(mistriseVillage, creatureOnlyVillage, testBear, secondBear)
        )
        return driver
    }

    /** Activate a village's "{U}, {T}" ability and resolve it, leaving a pending rider. */
    fun activateVillage(driver: GameTestDriver, player: EntityId, village: EntityId, abilityId: AbilityId) {
        driver.giveMana(player, Color.BLUE, 1)
        driver.submit(ActivateAbility(playerId = player, sourceId = village, abilityId = abilityId)).isSuccess shouldBe true
        driver.bothPass() // resolve the ability → adds the pending rider
    }

    test("the next spell you cast can't be countered") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val village = driver.putLandOnBattlefield(active, "Mistrise Village")
        val bear = driver.putCardInHand(active, "Test Bear")
        val counterspell = driver.putCardInHand(opponent, "Counterspell")

        activateVillage(driver, active, village, mistriseAbilityId)

        // Cast the protected spell.
        driver.giveMana(active, Color.GREEN, 2)
        driver.castSpell(active, bear)
        driver.stackSize shouldBe 1
        driver.passPriority(active)

        // Opponent tries to counter it.
        driver.giveMana(opponent, Color.BLUE, 2)
        val top = driver.getTopOfStack()!!
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(top)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.stackSize shouldBe 2

        // Counterspell resolves but the bear can't be countered.
        driver.bothPass()
        driver.stackSize shouldBe 1
        driver.getTopOfStackName() shouldBe "Test Bear"

        // Bear resolves and enters the battlefield.
        driver.bothPass()
        driver.findPermanent(active, "Test Bear") shouldNotBe null
    }

    test("only the next spell is protected; a second spell can be countered") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val village = driver.putLandOnBattlefield(active, "Mistrise Village")
        val bear = driver.putCardInHand(active, "Test Bear")
        val bear2 = driver.putCardInHand(active, "Second Bear")
        val counterspell = driver.putCardInHand(opponent, "Counterspell")

        activateVillage(driver, active, village, mistriseAbilityId)

        // First spell consumes the rider and resolves uncountered.
        driver.giveMana(active, Color.GREEN, 2)
        driver.castSpell(active, bear)
        driver.bothPass() // no one counters; it resolves
        driver.findPermanent(active, "Test Bear") shouldNotBe null
        driver.state.pendingUncounterableSpells.shouldBeEmpty()

        // Second spell is no longer protected.
        driver.giveMana(active, Color.GREEN, 2)
        driver.castSpell(active, bear2)
        driver.passPriority(active)
        driver.giveMana(opponent, Color.BLUE, 2)
        val top = driver.getTopOfStack()!!
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(top)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()
        driver.stackSize shouldBe 0
        driver.findPermanent(active, "Second Bear") shouldBe null // countered
    }

    test("an unused rider is cleared at the start of the next turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val village = driver.putLandOnBattlefield(active, "Mistrise Village")
        activateVillage(driver, active, village, mistriseAbilityId)
        driver.state.pendingUncounterableSpells shouldNotBe emptyList<Any>()

        // Pass through to the opponent's turn — the unused rider should be gone.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.pendingUncounterableSpells.shouldBeEmpty()
    }

    test("a creature-only rider ignores a noncreature spell and protects the next creature spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val village = driver.putLandOnBattlefield(active, "Creature Village")
        val bolt = driver.putCardInHand(active, "Lightning Bolt") // instant, noncreature
        val bear = driver.putCardInHand(active, "Test Bear")
        val counterspell = driver.putCardInHand(opponent, "Counterspell")

        activateVillage(driver, active, village, creatureOnlyAbilityId)

        // Cast a noncreature spell first — it must NOT consume the creature-only rider.
        driver.giveMana(active, Color.RED, 1)
        driver.castSpellWithTargets(active, bolt, listOf(ChosenTarget.Player(opponent)))
        driver.state.pendingUncounterableSpells shouldNotBe emptyList<Any>()
        driver.bothPass() // resolve the bolt (creature spells need an empty stack to cast)

        // Now cast the creature spell — the rider matches and protects it.
        driver.giveMana(active, Color.GREEN, 2)
        driver.castSpell(active, bear)
        driver.state.pendingUncounterableSpells.shouldBeEmpty()
        driver.passPriority(active)

        driver.giveMana(opponent, Color.BLUE, 2)
        val top = driver.getTopOfStack()!!
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(top)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        // Counterspell resolves; the bear can't be countered and stays on the stack.
        driver.bothPass()
        driver.getTopOfStackName() shouldBe "Test Bear"
    }
})
