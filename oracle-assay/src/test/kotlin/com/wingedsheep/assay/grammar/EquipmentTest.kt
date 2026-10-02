package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.corpus.OracleFace
import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The equipment band: "Equip {1}", and the two sentences an Equipment shares with an Aura.
 *
 * The band is two changes in two different places on purpose, and the tests are split the same way.
 * The *noun* is normalization's — an Equipment and an Aura print different words for the identical
 * model, so [Normalizer] abstracts one onto the other and the grammar never learns a second spelling
 * ([com.wingedsheep.assay.normalize.NormalizerTest] holds that half). The *keyword* is a line rule,
 * because "Equip {1}" fills a `CardScript` slot and a `CardDefinition` field at once.
 *
 * The whole-card cases below are therefore run through normalization first: reading a real
 * Equipment is exactly the composition of the two halves, and testing the line rule alone would
 * prove the band works on text no card prints.
 */
class EquipmentTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    /** A whole face, normalized and read line by line, as the touchstone does it. */
    fun card(name: String, typeLine: String, text: String): CardFragment {
        val normalized = Normalizer.normalize(OracleFace(name = name, oracleText = text, typeLine = typeLine))
        // The face must also survive the inverse, or the reading below is about text no card printed.
        normalized.restore(normalized.lines) shouldBe text
        return normalized.lines
            .map(::fragment)
            .fold(CardFragment.EMPTY as CardFragment?) { acc, f -> acc?.merge(f) }!!
    }

    /**
     * The id the equip rule mints. Read off a parse rather than restated, because no printed word
     * determines it — a test that hard-coded the constant would be asserting the grammar's private
     * naming instead of its reading.
     */
    val equipId: AbilityId = fragment("Equip {1}").script.activatedAbilities.single().id

    // Equip is lowered, not stored: the card gets `equipCost` *and* the ability CR 702.6a describes.
    // The rule builds it through `ActivatedAbility.equip`, the same factory `CardBuilder.equipAbility`
    // calls, so this assertion is about the lowering being shared rather than about its contents.
    "the equip line fills a script slot and a card field at once" {
        val equip = fragment("Equip {1}")

        equip.equipCost shouldBe ManaCost.parse("{1}")
        equip.script.activatedAbilities.single().isEquipAbility shouldBe true
        equip shouldBe CardFragment(
            equipCost = ManaCost.parse("{1}"),
            script = CardScript(
                activatedAbilities = listOf(
                    ActivatedAbility.equip(ManaCost.parse("{1}"), id = equip.script.activatedAbilities.single().id),
                ),
            ),
        )
        roundTrips("Equip {1}")
        roundTrips("Equip {0}")
        roundTrips("Equip {2}{W}")
    }

    // Bonesplitter, whole. Its static is byte-identical to Holy Strength's, which is the finding the
    // normalization pass is built on rather than an accident of this card.
    "an Equipment reads as the same statics an Aura does, plus its equip cost" {
        card("Bonesplitter", "Artifact — Equipment", "Equipped creature gets +2/+0.\nEquip {1}") shouldBe
            CardFragment(
                equipCost = ManaCost.parse("{1}"),
                script = CardScript(
                    staticAbilities = listOf(ModifyStats(2, 0)),
                    activatedAbilities = listOf(ActivatedAbility.equip(ManaCost.parse("{1}"), id = equipId)),
                ),
            )
    }

    "the granted-keyword sentence reads on an Equipment as it does on an Aura" {
        val fromEquipment = card(
            "Cobbled Wings",
            "Artifact — Equipment",
            "Equipped creature has flying.\nEquip {1}",
        )
        val fromAura = card("Flight", "Enchantment — Aura", "Enchant creature\nEnchanted creature has flying.")

        fromEquipment.script.staticAbilities shouldBe listOf(GrantKeyword(Keyword.FLYING))
        fromEquipment.script.staticAbilities shouldBe fromAura.script.staticAbilities
    }

    // The rule is fail-closed against the fields "Equip {1}" does not say. An equip ability whose
    // target filter, quality or timing is anything other than the lowering's must not print as the
    // bare printed line — a card whose "Equip Human {1}" came back as "Equip {1}" would be a
    // different card, and the differential would confirm it.
    "an equip ability the printed line does not describe refuses to print" {
        val quality = CardFragment(
            equipCost = ManaCost.parse("{1}"),
            script = CardScript(
                activatedAbilities = listOf(
                    ActivatedAbility.equip(ManaCost.parse("{1}"), quality = "Human", id = equipId),
                ),
            ),
        )
        Grammar.abilityLine.printLine(quality) shouldBe null
    }

    // `CardDefinition.equipCost` is not decoration beside the ability: `CardValidator` requires an
    // Equipment type line wherever it is set and the engine's equip permissions read it, so a
    // fragment carrying only the ability is a different card and must not print the line.
    "the ability without the cost is not the equip line" {
        val abilityOnly = CardFragment(
            script = CardScript(
                activatedAbilities = listOf(
                    ActivatedAbility.equip(ManaCost.parse("{1}"), id = equipId),
                ),
            ),
        )
        Grammar.abilityLine.printLine(abilityOnly) shouldBe null
    }

    // One card, one equip cost — `CardDefinition` has one field. Two "Equip" lines is a shape the
    // SDK cannot hold, so the fold declines and the gate counts the card rather than losing one.
    "two equip lines do not fold into one card" {
        fragment("Equip {1}").merge(fragment("Equip {2}")) shouldBe null
    }

    // The attach band. "It" is the source, and `AttachEquipmentEffect` always moves the source, so
    // the model names only the host: the bound target, filtered as printed.
    "an Equipment that attaches itself on entering reads as AttachEquipment to the target" {
        val text = "When ${Normalizer.SELF} enters, attach it to target creature you control."
        val ability = fragment(text).script.triggeredAbilities.single()
        ability.effect shouldBe AttachEquipmentEffect(Targets.bound())
        Targets.targetedFilter(ability.targetRequirement!!) shouldBe
            fragment("When ${Normalizer.SELF} enters, tap target creature you control.")
                .script.triggeredAbilities.single().targetRequirement!!.let(Targets::targetedFilter)
        // The pronoun is the alternate; the name is what prints.
        Grammar.abilityLine.printLine(fragment(text)) shouldBe
            "When ${Normalizer.SELF} enters, attach ${Normalizer.SELF} to target creature you control."
        roundTrips("When ${Normalizer.SELF} enters, attach ${Normalizer.SELF} to target artifact creature you control.")
        roundTrips("When ${Normalizer.SELF} enters, attach ${Normalizer.SELF} to up to one target creature you control.")
    }

    // In a filtered trigger "it" is the Equipment that triggered it, which the SDK spells as a
    // different effect; reading it as the source would attach the wrong object.
    "a filtered trigger's it is not the source, so the attach declines there" {
        Grammar.abilityLine
            .parseLine("Whenever an Equipment you control enters, attach it to target creature you control.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }
})
