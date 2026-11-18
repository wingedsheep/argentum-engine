package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.audit.AiReadingAudit.Check
import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A [com.wingedsheep.sdk.scripting.effects.RemoveKeywordEffect] on the ability's own source —
 * Xathrid Slyblade's "{3}{B}: Until end of turn, this creature loses hexproof and gains first
 * strike and deathtouch" — is the price of the grant beside it. Combat needs nothing from it (the
 * grants carry the ability's reach), and it is timed as the grant it accompanies: the whole ability
 * reads as a fight-bound expiring grant, so it is held for a fight the Slyblade can still be in.
 *
 * Unread, the self-loss was flagged twice ("combat can't see this keyword grant", "no timing for
 * this until-end-of-turn effect"), and its unknown leaf kept the whole ability out of the
 * expiring-grant window.
 */
class RemoveKeywordReadingTest : FunSpec({

    val slyblade by lazy {
        MtgSetCatalog.all.flatMap { it.cards }.first { it.name == "Xathrid Slyblade" }
    }

    test("Xathrid Slyblade's self hexproof loss reads as neutral to combat and timed with its grants") {
        val reading = AiReadingAudit.read(slyblade)
        withClue(reading.abilities.flatMap { it.findings }.filterNot { it.read }.joinToString("\n")) {
            reading.readFully shouldBe true
        }
        val findings = reading.abilities.flatMap { it.findings }.filter { it.effectType == "RemoveKeywordEffect" }
        findings.single { it.check == Check.COMBAT }.read shouldBe true
        findings.single { it.check == Check.TIMING }.read shouldBe true
    }

    test("the whole activation is an expiring grant the window can time") {
        val ability = slyblade.script.activatedAbilities.single()
        ExpiringGrantWindow.hasExpiringReading(ability.effect, creatureGrants = true, source = slyblade) shouldBe true
    }
})
