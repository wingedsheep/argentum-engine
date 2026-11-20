package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Prevent all damage that would be dealt to and dealt by target permanent an opponent controls"
 * reads as disarming the opponent's threat, and a lasting grant of a mill ability to every creature
 * we control reads as mill.
 */
class ShieldAndSilenceReadingTest : FunSpec({
    val catalog by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun card(name: String) = catalog[name] ?: error("$name is not in the card catalog")

    test("preventing damage to and by an opponent's permanent is aimed at the opponent") {
        listOf("Kiora, the Crashing Wave", "Dovin, Hand of Control").forEach { name ->
            withClue(name) {
                val ability = card(name).script.activatedAbilities.first { ab ->
                    TargetPolarityAnalyzer.forRequirements(ab.effect, ab.targetRequirements).isNotEmpty() &&
                        ab.effect.toString().contains("PreventDamage")
                }
                TargetPolarityAnalyzer.forRequirements(ability.effect, ability.targetRequirements) shouldBe
                    listOf(TargetPolarity.HARMFUL)
            }
        }
    }

    test("granting every creature we control a mill ability reads as mill") {
        CardIntentAnalyzer.analyze(card("Phenax, God of Deception")).tags shouldContain IntentTag.MILL
    }
})
