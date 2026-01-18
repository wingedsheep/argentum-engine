package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.card.CounterType
import com.wingedsheep.rulesengine.ecs.components.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ComponentContainerTest : FunSpec({

    context("creation") {
        test("empty creates empty container") {
            val container = ComponentContainer.empty()

            container.isEmpty().shouldBeTrue()
            container.size shouldBe 0
        }

        test("of creates container with components") {
            val container = ComponentContainer.of(
                LifeComponent(20),
                TappedComponent
            )

            container.size shouldBe 2
            container.isNotEmpty().shouldBeTrue()
        }

        test("of from list creates container") {
            val components = listOf(LifeComponent(20), TappedComponent)
            val container = ComponentContainer.of(components)

            container.size shouldBe 2
        }
    }

    context("get component") {
        test("get returns component when present") {
            val container = ComponentContainer.of(LifeComponent(20))

            val life = container.get<LifeComponent>()

            life.shouldNotBeNull()
            life.life shouldBe 20
        }

        test("get returns null when not present") {
            val container = ComponentContainer.empty()

            val life = container.get<LifeComponent>()

            life.shouldBeNull()
        }

        test("get with data object component") {
            val container = ComponentContainer.of(TappedComponent)

            val tapped = container.get<TappedComponent>()

            tapped.shouldNotBeNull()
            tapped shouldBe TappedComponent
        }
    }

    context("has component") {
        test("has returns true when present") {
            val container = ComponentContainer.of(LifeComponent(20))

            container.has<LifeComponent>().shouldBeTrue()
        }

        test("has returns false when not present") {
            val container = ComponentContainer.empty()

            container.has<LifeComponent>().shouldBeFalse()
        }

        test("has with data object component") {
            val container = ComponentContainer.of(TappedComponent)

            container.has<TappedComponent>().shouldBeTrue()
            container.has<SummoningSicknessComponent>().shouldBeFalse()
        }
    }

    context("with component") {
        test("with adds new component") {
            val container = ComponentContainer.empty()
                .with(LifeComponent(20))

            container.has<LifeComponent>().shouldBeTrue()
            container.get<LifeComponent>()?.life shouldBe 20
        }

        test("with replaces existing component") {
            val container = ComponentContainer.of(LifeComponent(20))
                .with(LifeComponent(15))

            container.get<LifeComponent>()?.life shouldBe 15
            container.size shouldBe 1
        }

        test("with is immutable") {
            val original = ComponentContainer.of(LifeComponent(20))
            val modified = original.with(TappedComponent)

            original.has<TappedComponent>().shouldBeFalse()
            modified.has<TappedComponent>().shouldBeTrue()
        }
    }

    context("without component") {
        test("without removes component") {
            val container = ComponentContainer.of(LifeComponent(20), TappedComponent)
                .without<TappedComponent>()

            container.has<TappedComponent>().shouldBeFalse()
            container.has<LifeComponent>().shouldBeTrue()
        }

        test("without on missing component does nothing") {
            val container = ComponentContainer.of(LifeComponent(20))
                .without<TappedComponent>()

            container.has<LifeComponent>().shouldBeTrue()
            container.size shouldBe 1
        }

        test("without is immutable") {
            val original = ComponentContainer.of(TappedComponent)
            val modified = original.without<TappedComponent>()

            original.has<TappedComponent>().shouldBeTrue()
            modified.has<TappedComponent>().shouldBeFalse()
        }
    }

    context("all components") {
        test("all returns all components") {
            val container = ComponentContainer.of(
                LifeComponent(20),
                TappedComponent,
                PoisonComponent(5)
            )

            container.all().size shouldBe 3
        }

        test("componentTypes returns type names") {
            val container = ComponentContainer.of(
                LifeComponent(20),
                TappedComponent
            )

            val types = container.componentTypes()

            types.size shouldBe 2
            types.contains("LifeComponent").shouldBeTrue()
            types.contains("TappedComponent").shouldBeTrue()
        }
    }

    context("complex component types") {
        test("CountersComponent") {
            val counters = CountersComponent()
                .add(CounterType.PLUS_ONE_PLUS_ONE, 2)
                .add(CounterType.LOYALTY, 3)

            val container = ComponentContainer.of(counters)

            val retrieved = container.get<CountersComponent>()
            retrieved.shouldNotBeNull()
            retrieved.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            retrieved.getCount(CounterType.LOYALTY) shouldBe 3
        }

        test("PTComponent") {
            val pt = PTComponent(basePower = 2, baseToughness = 3, powerModifier = 1)

            val container = ComponentContainer.of(pt)

            val retrieved = container.get<PTComponent>()
            retrieved.shouldNotBeNull()
            retrieved.currentPower shouldBe 3
            retrieved.currentToughness shouldBe 3
        }
    }
})
