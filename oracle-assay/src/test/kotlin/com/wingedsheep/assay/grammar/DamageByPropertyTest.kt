package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Damage sized by a characteristic of the object dealing it: "~ deals damage equal to its power to
 * any target." The source position reads the source as both dealer and object; a filtered trigger's
 * "it … its power" reads the matched creature as both, naming it as `damageSource`.
 */
class DamageByPropertyTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    val sourcePower = DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.Power)

    // Spikeshot Goblin's golden: the source's power, no separate dealer.
    "the source deals damage equal to its own power" {
        val damage = fragment("{R}, {T}: ~ deals damage equal to its power to any target.")
            .script.activatedAbilities.single().effect.shouldBeInstanceOf<DealDamageEffect>()
        damage.amount shouldBe sourcePower
        damage.damageSource shouldBe null
        roundTrips("{R}, {T}: ~ deals damage equal to its power to any target.")
        roundTrips("{G}, {T}: ~ deals damage equal to its toughness to target creature with flying.")
    }

    // Heartfire Hero: the pronoun subject of a dies trigger is still the source.
    "a dies trigger's pronoun is the source" {
        val damage = fragment("When ~ dies, it deals damage equal to its power to each opponent.")
            .script.triggeredAbilities.single().effect.shouldBeInstanceOf<DealDamageEffect>()
        damage.amount shouldBe sourcePower
        damage.target shouldBe EffectTarget.PlayerRef(Player.EachOpponent)
        damage.damageSource shouldBe null
    }

    // Warstorm Surge's golden: the matched creature deals the damage and sizes it.
    "a filtered trigger's pronoun is the creature it matched, as dealer and as amount" {
        val line = "Whenever a creature you control enters, it deals damage equal to its power to any target."
        val damage = fragment(line).script.triggeredAbilities.single().effect.shouldBeInstanceOf<DealDamageEffect>()
        damage.amount shouldBe DynamicAmount.EntityProperty(EffectTarget.TriggeringEntity, EntityNumericProperty.Power)
        damage.damageSource shouldBe EffectTarget.TriggeringEntity
        roundTrips(line)
    }

    // Fail-closed: an amount read off one object and dealt by another is not this sentence.
    "a dealer that is not the amount's object does not print" {
        val parsed = fragment("{R}, {T}: ~ deals damage equal to its power to any target.")
        val ability = parsed.script.activatedAbilities.single()
        val mismatched = (ability.effect as DealDamageEffect).copy(damageSource = EffectTarget.TriggeringEntity)
        Grammar.abilityLine.printLine(
            parsed.copy(script = parsed.script.copy(activatedAbilities = listOf(ability.copy(effect = mismatched))))
        ) shouldBe null
    }
})
