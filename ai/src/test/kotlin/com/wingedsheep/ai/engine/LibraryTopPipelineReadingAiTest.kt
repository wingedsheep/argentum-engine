package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Pipelines that dig through a library top: a reveal-until of unknown depth, an exile-until-mana-value,
 * a look that only puts cards back or into our graveyard, and a target player's library top taken away.
 */
class LibraryTopPipelineReadingAiTest : ScenarioTestBase() {

    private fun tagsOf(name: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(name)).tags

    init {
        test("a reveal-until off our own library top reads as the dig it is") {
            withClue("Aurora Awakener puts the revealed permanents onto our battlefield") {
                tagsOf("Aurora Awakener") shouldContain IntentTag.DRAW
            }
            withClue("Sanar's first-main trigger exiles the revealed cards for us to cast this turn") {
                tagsOf("Sanar, Innovative First-Year") shouldContain IntentTag.DRAW
            }
        }

        test("cards exiled until a mana value, ours to cast without paying, read as free spells") {
            tagsOf("Dream Harvest") shouldContain IntentTag.FREE_SPELL
        }

        test("a look at our own top that only puts cards back or in the graveyard is card selection") {
            tagsOf("Lluwen, Imperfect Naturalist") shouldContain IntentTag.CARD_SELECTION
            tagsOf("Sage Owl") shouldContain IntentTag.CARD_SELECTION
            withClue("a reveal-until that puts the cards onto the battlefield is no mere selection") {
                tagsOf("Aurora Awakener") shouldNotContain IntentTag.CARD_SELECTION
            }
        }

        test("a target player's library top taken away hurts them; handing some back makes it either-way") {
            val maralen = cardRegistry.requireCard("Maralen, Fae Ascendant").script.triggeredAbilities.first()
            TargetPolarityAnalyzer.forRequirements(maralen.effect, maralen.allTargetRequirements) shouldBe
                listOf(TargetPolarity.HARMFUL)
            val rakdos = cardRegistry.requireCard("Rakdos, the Muscle").script.triggeredAbilities.first()
            TargetPolarityAnalyzer.forRequirements(rakdos.effect, rakdos.allTargetRequirements) shouldBe
                listOf(TargetPolarity.HARMFUL)
            withClue("Grub's Command mills five and hands the Goblins back: a Goblin deck aims it at itself") {
                val modal = cardRegistry.requireCard("Grub's Command").script.spellEffect as ModalEffect
                TargetPolarityAnalyzer.forMode(modal.modes[3]) shouldBe listOf(TargetPolarity.FLEXIBLE)
            }
        }
    }
}
