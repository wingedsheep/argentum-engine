package com.wingedsheep.ai.audit

import com.wingedsheep.ai.audit.AiReadingAudit.AbilityKind
import com.wingedsheep.ai.audit.AiReadingAudit.Check
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * The per-ability reading the Assay explorer's "AI reading" panel shows: *where* each gap is (which
 * ability, step, target slot, reader) and what each reader did understand. The per-card flags it
 * folds to are pinned by [AiReadingAuditSnapshotTest] and the corpus ratchet; this pins the locations.
 */
class AiReadingByAbilityTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("a fully read spell reports what each reader understood") {
        val reading = AiReadingAudit.read(card("Lightning Bolt"))
        reading.readFully shouldBe true
        reading.flags shouldBe emptyList()
        val spell = reading.abilities.single()
        spell.ability.kind shouldBe AbilityKind.SPELL
        spell.readFully shouldBe true
        val polarity = spell.findings.single { it.check == Check.POLARITY }
        polarity.target shouldBe 1
        polarity.reason shouldContain "harmful"
        spell.findings.single { it.check == Check.INTENT }.reason shouldContain "removal"
    }

    test("a polarity gap names the target slot and the step that left it unread") {
        // A flicker of one of our own creatures (exile it, then return it) still has no polarity
        // reading: the exile half alone would read as removal aimed at the wrong side.
        val reading = AiReadingAudit.read(card("Cloudshift"))
        val gaps = reading.abilities.single { it.ability.kind == AbilityKind.SPELL }
            .findings.filter { it.check == Check.POLARITY && !it.read }
        gaps.map { it.effectType } shouldContainExactly listOf("MoveToZoneEffect")
        gaps.forEach {
            it.target shouldBe 1
            it.reason shouldBe "target 1: can't tell if this helps or hurts"
            it.step.shouldNotBeNull()
            it.flag.shouldNotBeNull().key shouldBe "polarity: ${it.effectType}"
        }
    }

    test("an intent gap points at the ability and step, and the flags fold back to the audit's") {
        val c = card("Act on Impulse")
        val reading = AiReadingAudit.read(c)
        val intent = reading.abilities.single().findings.filter { it.check == Check.INTENT }
        intent.forEach {
            it.read shouldBe false
            it.reason shouldBe "no intent: the AI doesn't know what this effect is for"
        }
        intent.map { it.effectType } shouldContainExactly listOf("GrantMayPlayFromExileEffect", "Pipeline(TopOfLibrary→exile)")
        reading.flags shouldBe AiReadingAudit.audit(c)
    }

    test("combat and timing gaps are placed on the activated ability that has them") {
        val reading = AiReadingAudit.read(card("Alchemist's Vial"))
        val activated = reading.abilities.filter { it.ability.kind == AbilityKind.ACTIVATED }
        activated shouldHaveSize 1
        val ability = activated.single()
        ability.readFully shouldBe false
        ability.ability.title shouldBe "Activated ability ${ability.ability.number}"
        ability.findings.filter { it.check == Check.COMBAT && !it.read }.map { it.effectType } shouldBe listOf("CantBlockEffect")
        ability.findings.filter { it.check == Check.TIMING && !it.read }.map { it.effectType }.toSet() shouldBe
            setOf("CantAttackEffect", "CantBlockEffect")
        ability.findings.first { it.check == Check.TIMING }.reason shouldBe "no timing for this until-end-of-turn effect"
        // The draw-on-entry trigger is read: the AI knows what it is for.
        reading.abilities.single { it.ability.kind == AbilityKind.TRIGGERED }.readFully shouldBe true
    }

    test("an Aura's host reading sits on the spell, and static abilities are listed but not checked") {
        val reading = AiReadingAudit.read(card("Pacifism"))
        val spell = reading.abilities.single { it.ability.kind == AbilityKind.SPELL }
        spell.findings.single().reason shouldBe "enchant target: reads as harmful — aimed at an opponent's side"
        reading.abilities.filter { it.ability.kind == AbilityKind.STATIC }.forEach {
            it.checked shouldBe false
            it.findings shouldBe emptyList()
        }
        reading.readFully shouldBe true
    }

    test("a static that only changes the card's own casting cost is part of the card, not an unread ability") {
        val reading = AiReadingAudit.read(card("Hollow One"))
        reading.abilities.single { it.ability.kind == AbilityKind.STATIC }.body shouldBe true
        AiCardRating.of(card("Hollow One"), listOf("HOU")).intent.unrecognised shouldBe false
    }

    test("a counter on its own source needs no intent, and reads as understood rather than unchecked") {
        val reading = AiReadingAudit.read(card("Ajani's Pridemate"))
        reading.readFully shouldBe true
        val trigger = reading.abilities.single { it.ability.kind == AbilityKind.TRIGGERED }
        trigger.checked shouldBe true
        trigger.findings.single { it.check == Check.INTENT }.reason shouldContain "needs no intent"
    }

    test("a loyalty counter on each planeswalker we control needs no intent") {
        AiReadingAudit.read(card("Way of the Mentor")).readFully shouldBe true
        AiReadingAudit.read(card("Way of the Necromancer")).readFully shouldBe true
    }

    test("a permanent untapping itself needs no intent, but untapping a chosen target stays flagged") {
        val monolith = AiReadingAudit.read(card("Basalt Monolith"))
        monolith.readFully shouldBe true
        monolith.abilities.filter { it.ability.kind == AbilityKind.ACTIVATED }
            .flatMap { it.findings }.single { it.check == Check.INTENT }.reason shouldContain "needs no intent"
        // Arbor Elf's "untap target Forest" now reads as ramp (TargetedUntapReadingTest); an untap of any
        // player's artifact or creature (Aphetto Alchemist) is still unread.
        AiReadingAudit.audit(card("Aphetto Alchemist")).map { it.key } shouldBe listOf("intent: TapUntapEffect")
    }

    test("a creature shifting its own P/T needs no intent, and a -N/-0 on a target reads as a combat effect") {
        val giant = AiReadingAudit.read(card("Flowstone Giant"))
        giant.readFully shouldBe true
        giant.abilities.filter { it.ability.kind == AbilityKind.ACTIVATED }
            .flatMap { it.findings }.single { it.check == Check.INTENT }.reason shouldContain "needs no intent"
        AiReadingAudit.audit(card("Dizzy Spell")).map { it.key }.filter { "ModifyStatsEffect" in it } shouldBe emptyList()
    }

    test("an upkeep cost its controller pays or sacrifices for needs no intent, but an opponent's tax on our draw stays flagged") {
        val djinn = AiReadingAudit.read(card("Drifting Djinn"))
        djinn.readFully shouldBe true
        djinn.abilities.single { it.ability.kind == AbilityKind.TRIGGERED }
            .findings.single { it.check == Check.INTENT }.reason shouldContain "needs no intent"
        AiReadingAudit.audit(card("Rhystic Study")).map { it.key } shouldBe listOf("intent: PayOrSufferEffect")
    }

    test("setting a player's life total needs no intent, like the gain or loss it is") {
        // Rule 119.5: the player gains or loses the difference — the same life the evaluator
        // prices on resolution as it does a life loss or damage to a player.
        for (name in listOf("Form of the Dinosaur", "Form of the Dragon", "Biorhythm")) {
            withClue(name) {
                AiReadingAudit.audit(card(name)).map { it.key }.filter { "SetLifeTotalEffect" in it } shouldBe emptyList()
            }
        }
        AiReadingAudit.read(card("Form of the Dinosaur")).readFully shouldBe true
    }

    test("a permanent removing counters from itself needs no intent, but removing them from a target stays flagged") {
        AiReadingAudit.read(card("Armageddon Clock")).readFully shouldBe true
        AiReadingAudit.read(card("Bloodletter Quill")).readFully shouldBe true
        AiReadingAudit.audit(card("Portent Tracker")).map { it.key } shouldContain "intent: RemoveCountersEffect"
    }

    test("a leaves-trigger returning what its permanent exiled needs no intent, but a return into a hand stays flagged") {
        val ring = AiReadingAudit.read(card("Oblivion Ring"))
        ring.readFully shouldBe true
        ring.abilities.filter { it.ability.kind == AbilityKind.TRIGGERED }
            .flatMap { it.findings }.filter { it.check == Check.INTENT }
            .any { it.reason.contains("needs no intent") } shouldBe true
        AiReadingAudit.audit(card("Hoarding Dragon")).map { it.key } shouldBe listOf("intent: Pipeline(FromLinkedExile→hand)")
    }

    test("a return to hand chosen among its controller's own permanents needs no intent, but a bounce that reaches others stays flagged") {
        AiReadingAudit.read(card("Azorius Chancery")).readFully shouldBe true
        AiReadingAudit.read(card("Whitemane Lion")).readFully shouldBe true
        AiReadingAudit.audit(card("Aetherize")).map { it.key } shouldContain "intent: Pipeline(BattlefieldMatching→hand)"
        // Over each player, "you" is the player iterated: the opponent returns their own land.
        AiReadingAudit.audit(card("Confounding Conundrum")).map { it.key } shouldContain "intent: ForEach>Pipeline(BattlefieldMatching→hand)"
    }

    test("a permanent setting its own base P/T needs no intent, and setting a target's reads as a combat enhancement") {
        AiReadingAudit.audit(card("Bramblefort Fink")).map { it.key } shouldNotContain "intent: SetBaseStatsEffect"
        AiReadingAudit.audit(card("Master of Winds")).map { it.key } shouldNotContain "intent: SetBaseStatsEffect"
        val squareUp = AiReadingAudit.read(card("Square Up"))
        squareUp.abilities.flatMap { it.findings }.filter { it.check == Check.INTENT }
            .any { it.read && it.reason.contains("makes a creature better in combat") } shouldBe true
    }
})
