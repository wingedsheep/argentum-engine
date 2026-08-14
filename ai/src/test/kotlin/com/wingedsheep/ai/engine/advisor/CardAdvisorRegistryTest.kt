package com.wingedsheep.ai.engine.advisor

import com.wingedsheep.ai.engine.advisor.modules.BloomburrowAdvisorModule
import com.wingedsheep.ai.engine.advisor.modules.OnslaughtAdvisorModule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * The registry is a single map keyed by card name, so two advisors claiming the
 * same card silently drop one of them — including its `evaluateCast` when the
 * winner only overrides `respondToDecision`. That is exactly how BLB's board
 * wipes lost their "only wipe when behind" logic. Registration must fail loudly.
 */
class CardAdvisorRegistryTest : FunSpec({

    test("register throws when two advisors claim the same card") {
        val first = object : CardAdvisor {
            override val cardNames = setOf("Shock")
        }
        val second = object : CardAdvisor {
            override val cardNames = setOf("Shock", "Lightning Bolt")
        }

        val registry = CardAdvisorRegistry()
        registry.register(first)

        val error = shouldThrow<IllegalArgumentException> { registry.register(second) }
        error.message shouldContain "Shock"
    }

    test("every shipped advisor module registers without collisions") {
        // One registry for all modules: a card claimed by two *sets* is just as broken
        // as one claimed twice within a set.
        val registry = CardAdvisorRegistry()
        listOf(BloomburrowAdvisorModule(), OnslaughtAdvisorModule()).forEach { it.register(registry) }

        // Spot-check the cards that regressed: both must still reach an advisor.
        registry.getAdvisor("Starfall Invocation") shouldNotBe null
        registry.getAdvisor("Wildfire Howl") shouldNotBe null
        registry.getAdvisor("Valley Rally") shouldNotBe null
    }
})
