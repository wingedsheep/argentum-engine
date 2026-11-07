package com.wingedsheep.gameserver.coverage

import com.wingedsheep.assay.compile.CardCompiler
import com.wingedsheep.assay.compile.CompileResult
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo as shouldBeGreaterThanOrEqual
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo as shouldBeLessThanOrEqual
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration.Companion.seconds

/**
 * The game server's half of the Assay explorer's AI reading panel: the provider `:oracle-assay`
 * cannot build itself, and the per-set share behind Set Completion's "AI 82%" tag.
 */
class AiReadingServiceTest : FunSpec({

    val byName = MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name }

    fun registryOf(vararg names: String) = CardRegistry().apply {
        names.forEach { register(byName[it] ?: error("'$it' is not in the corpus")) }
    }

    test("an implemented card is read from the engine's own definition, ability by ability") {
        val service = AiReadingService(registryOf("Battering Ram"))
        val card = service.implemented("Battering Ram").shouldNotBeNull()
        val view = service.read(card)
        view.readFully shouldBe false
        view.abilities.count { it.readFully } shouldBe 1
        val banding = view.abilities.single { !it.readFully }
        banding.kind shouldBe "TRIGGERED"
        banding.findings.any { it.check == "intent" && !it.read } shouldBe true
        view.flags shouldBe listOf("intent: GrantKeywordEffect")
    }

    test("an unimplemented card Assay compiles is read as a preview of Assay's definition") {
        val service = AiReadingService(CardRegistry())
        service.implemented("Assay Test Bolt") shouldBe null
        val json = """
            {"name": "Assay Test Bolt", "mana_cost": "{R}", "type_line": "Instant",
             "oracle_text": "Assay Test Bolt deals 3 damage to any target.", "layout": "normal"}
        """.trimIndent()
        val compiled = CardCompiler.compile(json) as? CompileResult.Compiled
            ?: error("expected Assay to compile the test card: $json")
        val view = service.read(compiled.definition)
        view.readFully shouldBe true
        val spell = view.abilities.single()
        spell.kind shouldBe "SPELL"
        spell.findings.single { it.check == "polarity" }.reason shouldContain "harmful"
    }

    test("the corpus index answers per card and per set once built, and nothing before") {
        val service = AiReadingService(registryOf("Lightning Bolt", "Battering Ram"))
        eventually(30.seconds) {
            service.corpusReady() shouldBe true
        }
        service.readsFully("Lightning Bolt") shouldBe true
        service.readsFully("Battering Ram") shouldBe false
        service.readsFully("Not A Card") shouldBe null
        val summary = service.setSummary()
        summary.ready shouldBe true
        summary.sets.values.any { it.readFully > 0 } shouldBe true
        summary.sets.values.forEach { it.abilitiesRead shouldBeLessThanOrEqual it.abilities }
    }

    test("a set's detail groups its unread cards by the effect type the AI misses, biggest first") {
        val atq = MtgSetCatalog.byCode("ATQ").shouldNotBeNull()
        val service = AiReadingService(CardRegistry().apply { atq.cards.forEach { register(it) } })
        eventually(30.seconds) { service.corpusReady() shouldBe true }
        val detail = service.setDetail("ATQ").shouldNotBeNull()
        detail.ready shouldBe true
        val score = detail.score.shouldNotBeNull()
        detail.unread.size shouldBe score.total - score.readFully
        detail.groups.zipWithNext().forEach { (a, b) -> a.cards.size shouldBeGreaterThanOrEqual b.cards.size }
        val artisans = detail.unread.single { it.name == "Goblin Artisans" }
        detail.groups.single { it.key == "intent: FlipCoinEffect" }.cards shouldContain "Goblin Artisans"
        artisans.flags shouldContain "polarity: FlipCoinEffect"
        artisans.abilitiesRead shouldBeLessThan artisans.abilities
        service.setDetail("NOPE") shouldBe null
    }
})
