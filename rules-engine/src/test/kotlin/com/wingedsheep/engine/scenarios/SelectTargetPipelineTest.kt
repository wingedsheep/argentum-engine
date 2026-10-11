package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tests for SelectTargetEffect — mid-resolution pipeline targeting.
 *
 * Uses an inline test sorcery "Pipeline Bolt" that:
 * 1. SelectTargetEffect chooses a creature at resolution time
 * 2. DealDamageEffect deals 3 damage to the chosen creature via PipelineTarget
 *
 * This verifies the full pipeline: executor → decision → continuation → resume →
 * collection propagation → damage resolution.
 */
class SelectTargetPipelineTest : FunSpec({

    // A sorcery with NO cast-time target — targeting happens during resolution
    val PipelineBolt = CardDefinition.sorcery(
        name = "Pipeline Bolt",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Choose a creature. Deal 3 damage to it.",
        script = CardScript.spell(
            effect = CompositeEffect(
                listOf(
                    SelectTargetEffect(
                        requirement = TargetObject(filter = TargetFilter.Creature),
                        storeAs = "chosen"
                    ),
                    DealDamageEffect(
                        amount = 3,
                        target = EffectTarget.PipelineTarget("chosen")
                    )
                )
            )
            // No TargetRequirement — no cast-time targeting
        )
    )

    val OptionalPipelineBolt = CardDefinition.sorcery(
        name = "Optional Pipeline Bolt",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Choose up to one creature. Deal 3 damage to it.",
        script = CardScript.spell(
            effect = CompositeEffect(
                listOf(
                    SelectTargetEffect(
                        requirement = TargetObject(filter = TargetFilter.Creature, optional = true),
                        storeAs = "chosen"
                    ),
                    DealDamageEffect(
                        amount = 3,
                        target = EffectTarget.PipelineTarget("chosen")
                    )
                )
            )
        )
    )

    // "Choose a creature, then a different creature" — the second pick excludes the first.
    val TwinPipelineBolt = CardDefinition.sorcery(
        name = "Twin Pipeline Bolt",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Choose a creature, then choose a second creature. Deal 3 damage to each.",
        script = CardScript.spell(
            effect = CompositeEffect(
                listOf(
                    SelectTargetEffect(
                        requirement = TargetObject(filter = TargetFilter.Creature),
                        storeAs = "first"
                    ),
                    SelectTargetEffect(
                        requirement = TargetObject(filter = TargetFilter.Creature),
                        storeAs = "second",
                        excluding = listOf("first")
                    ),
                    DealDamageEffect(amount = 3, target = EffectTarget.PipelineTarget("first")),
                    DealDamageEffect(amount = 3, target = EffectTarget.PipelineTarget("second"))
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PipelineBolt, OptionalPipelineBolt, TwinPipelineBolt))
        return driver
    }

    /** Advance to precombat main phase, play a Mountain, then return caster/opponent IDs. */
    fun setupForCast(driver: GameTestDriver): Pair<EntityId, EntityId> {
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase so we can cast sorceries
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Play a Mountain to pay for Pipeline Bolt
        val mountain = driver.findCardInHand(caster, "Mountain")!!
        driver.playLand(caster, mountain)

        return Pair(caster, opponent)
    }

    test("single legal target is auto-selected and dealt damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val (caster, opponent) = setupForCast(driver)

        // Put a single creature on the opponent's side
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Add Pipeline Bolt directly to hand (avoid flaky random draw)
        val boltId = driver.putCardInHand(caster, "Pipeline Bolt")
        val castResult = driver.castSpell(caster, boltId)
        castResult.outcome shouldBe Outcome.Done

        // Spell is on the stack — resolve it
        driver.bothPass()

        // With only one legal creature, SelectTargetEffect auto-selects.
        // DealDamageEffect deals 3 to the 2/2 Bear → it dies.
        driver.assertInGraveyard(opponent, "Grizzly Bears")
    }

    test("single legal target is still offered when selecting it is optional") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val (caster, opponent) = setupForCast(driver)
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val boltId = driver.putCardInHand(caster, "Optional Pipeline Bolt")
        driver.castSpell(caster, boltId)
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.targetRequirements.single().minTargets shouldBe 0

        driver.submitTargetSelection(caster, emptyList())
        driver.getCreatures(opponent).shouldContain(bear)
    }

    test("multiple legal targets presents decision and deals damage to chosen") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val (caster, opponent) = setupForCast(driver)

        // Put two creatures on the opponent's side
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val centaur = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // Add Pipeline Bolt directly to hand (avoid flaky random draw)
        val boltId = driver.putCardInHand(caster, "Pipeline Bolt")
        driver.castSpell(caster, boltId)

        // Spell is on the stack — resolve it
        driver.bothPass()

        // Multiple legal targets → ChooseTargetsDecision during resolution
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.playerId shouldBe caster

        // Choose the Grizzly Bears (2/2 — will die to 3 damage)
        driver.submitTargetSelection(caster, listOf(bear))

        // Bear dies, Centaur Courser (3/3) survives
        driver.assertInGraveyard(opponent, "Grizzly Bears")
        driver.getCreatures(opponent).size shouldBe 1
    }

    test("no legal targets skips gracefully — spell resolves but does nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val (caster, opponent) = setupForCast(driver)

        // No creatures on the battlefield at all

        // Add Pipeline Bolt directly to hand (avoid flaky random draw)
        val boltId = driver.putCardInHand(caster, "Pipeline Bolt")
        driver.castSpell(caster, boltId)

        // Spell is on the stack — resolve it
        driver.bothPass()

        // SelectTargetEffect finds no legal targets → stores empty collection
        // DealDamageEffect resolves with empty PipelineTarget → does nothing
        // No crash, no pending decision — spell just fizzles gracefully
        driver.pendingDecision shouldBe null
        driver.assertLifeTotal(caster, 20)
        driver.assertLifeTotal(opponent, 20)
    }

    test("caster's own creatures are also legal targets") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val (caster, _) = setupForCast(driver)

        // Put a creature on the caster's side only
        driver.putCreatureOnBattlefield(caster, "Grizzly Bears")

        // Add Pipeline Bolt directly to hand (avoid flaky random draw)
        val boltId = driver.putCardInHand(caster, "Pipeline Bolt")
        driver.castSpell(caster, boltId)

        // Resolve
        driver.bothPass()

        // Auto-selects caster's own bear (only legal target)
        driver.assertInGraveyard(caster, "Grizzly Bears")
    }

    test("excluding drops an earlier pick, across the pause, from a later choice") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val (caster, opponent) = setupForCast(driver)
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val elves = driver.putCreatureOnBattlefield(opponent, "Llanowar Elves")
        val goblin = driver.putCreatureOnBattlefield(opponent, "Goblin Guide")

        driver.castSpell(caster, driver.putCardInHand(caster, "Twin Pipeline Bolt"))
        driver.bothPass()

        val first = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        first.legalTargets[0]!!.toSet() shouldBe setOf(bear, elves, goblin)
        driver.submitTargetSelection(caster, listOf(bear))

        val second = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        second.legalTargets[0]!!.toSet() shouldBe setOf(elves, goblin)
        driver.submitTargetSelection(caster, listOf(elves))

        driver.assertInGraveyard(opponent, "Grizzly Bears")
        driver.assertInGraveyard(opponent, "Llanowar Elves")
        driver.getCreatures(opponent) shouldBe listOf(goblin)
    }

    test("excluding the only candidate leaves the later choice empty instead of re-picking it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val (caster, opponent) = setupForCast(driver)
        val force = driver.putCreatureOnBattlefield(opponent, "Force of Nature") // 5/5

        driver.castSpell(caster, driver.putCardInHand(caster, "Twin Pipeline Bolt"))
        driver.bothPass()

        // One creature: the first pick is forced, the second finds nobody — 3 damage, not a lethal 6.
        driver.pendingDecision shouldBe null
        driver.getCreatures(opponent) shouldBe listOf(force)
    }
})
