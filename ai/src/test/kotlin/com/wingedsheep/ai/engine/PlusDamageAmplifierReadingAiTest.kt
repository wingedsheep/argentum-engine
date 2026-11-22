package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.Recipient
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * A fixed "that much damage plus N" bonus on our own sources — Pyromancer's Gauntlet's red instants,
 * sorceries and planeswalkers, Torbran's red sources — read the way a one-sided [DoubleDamage]
 * multiplier is: a damage amplifier with permanent value, and, when its sources are our spells, a
 * payoff for the deck's instants and sorceries.
 *
 * Unread, the Gauntlet was an "unreadable permanent" worth half a point.
 */
class PlusDamageAmplifierReadingAiTest : ScenarioTestBase() {

    private fun definition(name: String) = cardRegistry.getCard(name)!!

    init {
        test("Pyromancer's Gauntlet and Torbran read as damage amplifiers with board value") {
            for (name in listOf("Pyromancer's Gauntlet", "Torbran, Thane of Red Fell")) {
                val intent = CardIntentAnalyzer.analyze(definition(name))
                intent.tags shouldContain IntentTag.DAMAGE_AMPLIFIER
                intent.staticPriorValue shouldBeGreaterThan 0.5
            }
            AiCardRating.of(definition("Pyromancer's Gauntlet"), emptyList()).board
                .parts.single { it.label == "Amplifies our damage" }.value shouldBe 2.0
        }

        test("a spell-sourced amplifier rewards the spells axis; a creature-sourced one does not") {
            SynergyAxes.of(definition("Pyromancer's Gauntlet")).rewards shouldContain SynergyAxis.SPELLS
            SynergyAxes.of(definition("Torbran, Thane of Red Fell")).rewards shouldNotContain SynergyAxis.SPELLS
        }

        test("a bonus to the opponent's sources or a shrink is no payoff") {
            val cases = listOf(
                "Enemy Plus Amplifier" to ModifyDamageAmount(
                    modifier = 2,
                    appliesTo = EventPattern.DamageEvent(source = GameObjectFilter.InstantOrSorcery.opponentControls()),
                ),
                "Own Damage Shrinker" to ModifyDamageAmount(
                    modifier = -1,
                    appliesTo = EventPattern.DamageEvent(source = GameObjectFilter.InstantOrSorcery.youControl(), recipient = Recipient.Any),
                ),
            )
            for ((name, replacement) in cases) {
                val definition = card(name) {
                    typeLine = "Artifact"
                    replacementEffect(replacement)
                }
                CardIntentAnalyzer.analyze(definition).tags shouldNotContain IntentTag.DAMAGE_AMPLIFIER
                SynergyAxes.of(definition).rewards shouldNotContain SynergyAxis.SPELLS
            }
        }
    }
}
