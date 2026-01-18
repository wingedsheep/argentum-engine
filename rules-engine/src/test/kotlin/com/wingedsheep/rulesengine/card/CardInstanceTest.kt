package com.wingedsheep.rulesengine.card

import com.wingedsheep.rulesengine.core.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
class CardInstanceTest : FunSpec({

    val dragonDefinition = CardDefinition.creature(
        name = "Alabaster Dragon",
        manaCost = ManaCost.parse("{4}{W}{W}"),
        subtypes = setOf(Subtype.DRAGON),
        power = 4,
        toughness = 4,
        keywords = setOf(Keyword.FLYING)
    )

    val goblinDefinition = CardDefinition.creature(
        name = "Raging Goblin",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype.GOBLIN),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.HASTE)
    )

    context("creation") {
        test("creates instance with unique id") {
            val instance1 = CardInstance.create(dragonDefinition, "player1")
            val instance2 = CardInstance.create(dragonDefinition, "player1")

            instance1.id.value.isNotEmpty().shouldBeTrue()
            instance2.id.value.isNotEmpty().shouldBeTrue()
            instance1.id shouldBe instance1.id // same instance has same id
        }

        test("new instance has default state") {
            val instance = CardInstance.create(dragonDefinition, "player1")

            instance.ownerId shouldBe "player1"
            instance.controllerId shouldBe "player1"
            instance.isTapped.shouldBeFalse()
            instance.summoningSickness.shouldBeTrue()
            instance.damageMarked shouldBe 0
            instance.counters shouldBe emptyMap()
        }
    }

    context("power and toughness") {
        test("currentPower returns base power for unmodified creature") {
            val instance = CardInstance.create(dragonDefinition, "player1")
            instance.currentPower shouldBe 4
        }

        test("currentToughness returns base toughness for unmodified creature") {
            val instance = CardInstance.create(dragonDefinition, "player1")
            instance.currentToughness shouldBe 4
        }

        test("power modifier affects current power") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .modifyPower(2)
            instance.currentPower shouldBe 6
        }

        test("toughness modifier affects current toughness") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .modifyToughness(-1)
            instance.currentToughness shouldBe 3
        }

        test("+1/+1 counters increase power and toughness") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .addCounter(CounterType.PLUS_ONE_PLUS_ONE, 2)
            instance.currentPower shouldBe 6
            instance.currentToughness shouldBe 6
        }

        test("-1/-1 counters decrease toughness") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .addCounter(CounterType.MINUS_ONE_MINUS_ONE, 1)
            instance.currentToughness shouldBe 3
        }
    }

    context("damage") {
        test("dealing damage marks it on creature") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .dealDamage(2)
            instance.damageMarked shouldBe 2
            instance.effectiveToughness shouldBe 2
        }

        test("hasLethalDamage when effective toughness is 0 or less") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .dealDamage(4)
            instance.hasLethalDamage.shouldBeTrue()
        }

        test("clearDamage removes all damage") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .dealDamage(3)
                .clearDamage()
            instance.damageMarked shouldBe 0
        }
    }

    context("tapping") {
        test("tap sets tapped to true") {
            val instance = CardInstance.create(dragonDefinition, "player1").tap()
            instance.isTapped.shouldBeTrue()
        }

        test("untap sets tapped to false") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .tap()
                .untap()
            instance.isTapped.shouldBeFalse()
        }
    }

    context("summoning sickness") {
        test("new creature has summoning sickness") {
            val instance = CardInstance.create(dragonDefinition, "player1")
            instance.summoningSickness.shouldBeTrue()
        }

        test("removeSummoningSickness clears it") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .removeSummoningSickness()
            instance.summoningSickness.shouldBeFalse()
        }
    }

    context("canAttack") {
        test("untapped creature without summoning sickness can attack") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .removeSummoningSickness()
            instance.canAttack.shouldBeTrue()
        }

        test("tapped creature cannot attack") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .removeSummoningSickness()
                .tap()
            instance.canAttack.shouldBeFalse()
        }

        test("creature with summoning sickness cannot attack") {
            val instance = CardInstance.create(dragonDefinition, "player1")
            instance.canAttack.shouldBeFalse()
        }

        test("creature with haste can attack despite summoning sickness") {
            val instance = CardInstance.create(goblinDefinition, "player1")
            instance.canAttack.shouldBeTrue()
        }
    }

    context("keywords") {
        test("inherits keywords from definition") {
            val instance = CardInstance.create(dragonDefinition, "player1")
            instance.hasKeyword(Keyword.FLYING).shouldBeTrue()
        }

        test("addKeyword adds temporary keyword") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .addKeyword(Keyword.TRAMPLE)
            instance.hasKeyword(Keyword.TRAMPLE).shouldBeTrue()
            instance.hasKeyword(Keyword.FLYING).shouldBeTrue()
        }

        test("removeKeyword removes keyword") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .removeKeyword(Keyword.FLYING)
            instance.hasKeyword(Keyword.FLYING).shouldBeFalse()
        }
    }

    context("counters") {
        test("addCounter adds counters") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .addCounter(CounterType.PLUS_ONE_PLUS_ONE, 3)
            instance.counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 3
        }

        test("addCounter stacks with existing counters") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .addCounter(CounterType.PLUS_ONE_PLUS_ONE, 2)
                .addCounter(CounterType.PLUS_ONE_PLUS_ONE, 1)
            instance.counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 3
        }

        test("removeCounter removes counters") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .addCounter(CounterType.PLUS_ONE_PLUS_ONE, 3)
                .removeCounter(CounterType.PLUS_ONE_PLUS_ONE, 1)
            instance.counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 2
        }

        test("removeCounter removes entry when reaching zero") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .addCounter(CounterType.PLUS_ONE_PLUS_ONE, 1)
                .removeCounter(CounterType.PLUS_ONE_PLUS_ONE, 1)
            instance.counters.containsKey(CounterType.PLUS_ONE_PLUS_ONE).shouldBeFalse()
        }
    }

    context("controller") {
        test("changeController updates controller") {
            val instance = CardInstance.create(dragonDefinition, "player1")
                .changeController("player2")
            instance.ownerId shouldBe "player1"
            instance.controllerId shouldBe "player2"
        }
    }
})
