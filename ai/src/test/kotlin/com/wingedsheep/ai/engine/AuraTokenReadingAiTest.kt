package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.scripting.effects.CreateAuraTokenEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Create a Sorcerer Role token attached to target creature you control" (Spellbook Vendor), "create
 * a Cursed Role token attached to target creature an opponent controls" (Diminisher Witch): the
 * token does to its creature what the Aura does, so a buff Role is a pump and the Cursed Role's
 * base 1/1 shuts its creature off.
 *
 * Unread, every Role-making card was "no intent": the AI knew who to aim the Role at (polarity
 * reads the token's definition) but not what the card was for, so a Diminisher Witch counted as
 * a vanilla body, never as an answer, and Lord Skitter's Blessing's Wicked Role as nothing.
 */
class AuraTokenReadingAiTest : ScenarioTestBase() {

    private fun tags(name: String) = CardIntentAnalyzer.analyze(cardRegistry.getCard(name)!!).tags

    init {
        test("a Cursed Role on an opponent's creature reads as an answer") {
            listOf("Diminisher Witch", "Vantress Transmuter").forEach { name ->
                withClue(name) { tags(name) shouldContain IntentTag.NEUTRALIZE }
            }
        }

        test("a buff Role on our own creature reads as a pump") {
            listOf("Spellbook Vendor", "Merry Bards", "Lord Skitter's Blessing", "Besotted Knight", "Syr Armont, the Redeemer")
                .forEach { name -> withClue(name) { tags(name) shouldContain IntentTag.PUMP } }
        }

        test("a Cursed Role on our own creature is the card's price, not an answer") {
            // Spiteful Hexmage's "target creature you control", Cursed Courtier's "attached to it".
            listOf("Spiteful Hexmage", "Cursed Courtier").forEach { name ->
                withClue(name) { tags(name) shouldNotContain IntentTag.NEUTRALIZE }
            }
        }

        test("every predefined Aura token has a reading that agrees with its own polarity") {
            val lookup = { name: String -> PredefinedTokens.allTokens.firstOrNull { it.name == name } }
            val auraTokens = PredefinedTokens.allTokens.filter { it.script.auraTarget != null }
            auraTokens.map { it.name } shouldContain "Cursed Role"
            auraTokens.forEach { token ->
                val effect = CreateAuraTokenEffect(token.name, EffectTarget.ContextTarget(0))
                val requirements = listOf(token.script.auraTarget!!)
                val read = CardIntentAnalyzer.analyzeEffect(effect, requirements).tags
                val polarity = TargetPolarityAnalyzer.forRequirements(effect, requirements, lookup).single()
                withClue("${token.name}: read $read, polarity $polarity") {
                    polarity shouldNotBe TargetPolarity.UNKNOWN
                    when (polarity) {
                        TargetPolarity.HARMFUL -> read shouldBe setOf(IntentTag.NEUTRALIZE)
                        else -> (read == setOf(IntentTag.PUMP) || read == setOf(IntentTag.RAMP)) shouldBe true
                    }
                }
            }
        }
    }
}
