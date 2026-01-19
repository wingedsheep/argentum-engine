package com.wingedsheep.rulesengine.ecs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank

class EntityIdTest : FunSpec({

    context("generation") {
        test("generate creates unique IDs") {
            val id1 = EntityId.generate()
            val id2 = EntityId.generate()

            id1 shouldNotBe id2
        }

        test("generate creates valid UUID strings") {
            val id = EntityId.generate()
            id.value.shouldNotBeBlank()
        }
    }

    context("creation") {
        test("of creates EntityId from string") {
            val id = EntityId.of("test-id")
            id.value shouldBe "test-id"
        }

        test("toString returns value") {
            val id = EntityId.of("test-id")
            id.toString() shouldBe "test-id"
        }
    }

    context("equality") {
        test("same value equals") {
            val id1 = EntityId.of("same")
            val id2 = EntityId.of("same")

            id1 shouldBe id2
        }

        test("different value not equals") {
            val id1 = EntityId.of("one")
            val id2 = EntityId.of("two")

            id1 shouldNotBe id2
        }
    }
})
