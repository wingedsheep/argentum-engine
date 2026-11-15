package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Manifest the top card of your library" and "manifest dread" put a face-down 2/2 creature onto
 * our battlefield whatever the card is — a body, read as a token maker. The Fate Reforged "Form"
 * cycle manifests and then attaches itself to that body ("it becomes an Aura with enchant
 * creature"), so it reads as the body plus what its "enchanted creature" statics grant.
 */
class ManifestReadingAiTest : ScenarioTestBase() {

    private fun tagsOf(name: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(name)).tags

    init {
        test("manifesting from our own library reads as making a creature body") {
            for (card in listOf("Cloudform", "Rageform", "Manifest Dread", "Cursed Windbreaker")) {
                withClue(card) { tagsOf(card) shouldContain IntentTag.TOKEN_MAKER }
            }
        }

        test("a Form still reads as what it grants the creature it enchants") {
            val tags = tagsOf("Cloudform")
            tags shouldContain IntentTag.EVASION_GRANT
            tags shouldContain IntentTag.PROTECTION
        }

        test("a manifested body feeds creatures-enter and death payoffs, but is no token") {
            val feeds = SynergyAxes.of(cardRegistry.requireCard("Cloudform")).feeds
            feeds shouldContain SynergyAxis.CREATURES_ENTER
            feeds shouldContain SynergyAxis.DEATH
            feeds shouldNotContain SynergyAxis.TOKENS
        }

        test("a manifest handed to the exiled creature's controller is no body of ours") {
            tagsOf("Reality Shift") shouldNotContain IntentTag.TOKEN_MAKER
        }
    }
}
