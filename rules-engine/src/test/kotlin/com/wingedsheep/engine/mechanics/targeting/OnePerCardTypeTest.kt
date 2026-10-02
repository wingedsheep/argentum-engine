package com.wingedsheep.engine.mechanics.targeting

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The "up to one target … of each card type" pairing rule: every chosen object must be matched to a
 * different card type it has (a bipartite matching, not "no two share a type").
 */
class OnePerCardTypeTest : FunSpec({

    val artifactCreature = setOf("ARTIFACT", "CREATURE")
    val creature = setOf("CREATURE")
    val artifact = setOf("ARTIFACT")
    val instant = setOf("INSTANT")

    test("empty and single selections are always legal") {
        OnePerCardType.canAssignDistinct(emptyList()) shouldBe true
        OnePerCardType.canAssignDistinct(listOf(artifactCreature)) shouldBe true
    }

    test("two objects of only the same type can't both be chosen") {
        OnePerCardType.canAssignDistinct(listOf(creature, creature)) shouldBe false
    }

    test("a multi-typed object takes whichever slot is left free") {
        OnePerCardType.canAssignDistinct(listOf(artifactCreature, creature)) shouldBe true
        OnePerCardType.canAssignDistinct(listOf(creature, artifactCreature)) shouldBe true
        OnePerCardType.canAssignDistinct(listOf(artifactCreature, artifactCreature)) shouldBe true
        OnePerCardType.canAssignDistinct(listOf(artifactCreature, artifact, instant)) shouldBe true
    }

    test("a multi-typed object can't fill two slots at once") {
        OnePerCardType.canAssignDistinct(listOf(artifactCreature, artifact, creature)) shouldBe false
        OnePerCardType.canAssignDistinct(listOf(artifactCreature, artifactCreature, creature)) shouldBe false
    }

    test("an object with no card type can't be chosen") {
        OnePerCardType.canAssignDistinct(listOf(emptySet(), instant)) shouldBe false
    }

    test("the selection cap is the largest matching, not the count of types present") {
        OnePerCardType.maxDistinctAssignment(emptyList()) shouldBe 0
        // Three artifact creatures span two types, so only two can be kept.
        OnePerCardType.maxDistinctAssignment(listOf(artifactCreature, artifactCreature, artifactCreature)) shouldBe 2
        // One artifact creature spans two types but is a single card.
        OnePerCardType.maxDistinctAssignment(listOf(artifactCreature)) shouldBe 1
        OnePerCardType.maxDistinctAssignment(listOf(creature, artifactCreature, creature, instant)) shouldBe 3
    }
})
