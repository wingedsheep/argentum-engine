package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Until end of turn, target creature gains 'When this creature dies, return it to its owner's
 * hand'" (Verdant Rebirth, Ashnod's Intervention) and its "… to the battlefield" sibling (Feign
 * Death): a grant that helps the creature's controller, and protection spent only if that
 * creature dies before cleanup.
 *
 * Unread, the target slot was a coin toss and Verdant Rebirth read as a bare cantrip — cast in a
 * main phase on an empty stack for the card, throwing the protection away.
 */
class DeathReturnGrantReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a granted self-return dies trigger is aimed at our own creature and reads as protection") {
            listOf("Verdant Rebirth", "Ashnod's Intervention", "Feign Death").forEach { name ->
                withClue("$name: the creature that comes back is its controller's") {
                    intents.spellTargetPolarities(name) shouldBe listOf(TargetPolarity.BENEFICIAL)
                    CardIntentAnalyzer.analyze(cardRegistry.getCard(name)!!).tags shouldContain IntentTag.PROTECTION
                }
            }
        }

        test("Verdant Rebirth is held on an empty stack in our main phase, not cast for the card") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Verdant Rebirth")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val card = game.findCardsInHand(1, "Verdant Rebirth").first()
            val bears = game.findPermanent("Grizzly Bears")!!
            val cast = CastSpell(game.player1Id, card, listOf(ChosenTarget.Permanent(bears)))

            withClue("without the floor the cantrip's draw decides alone") {
                HoldPolicy(intents).verdictFor(game.state, game.player1Id, "Verdant Rebirth", cast = cast) shouldBe
                    TimingVerdict.Neutral
            }
            withClue("nothing can kill the Bears before cleanup, so the grant would be thrown away") {
                HoldPolicy(intents, refuseUnspendableGrants = true)
                    .verdictFor(game.state, game.player1Id, "Verdant Rebirth", cast = cast) shouldBe TimingVerdict.NoWindow
            }
        }

        test("Verdant Rebirth answers removal on the stack aimed at our creature") {
            val game = scenario().withPlayers()
                .withActivePlayer(2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Verdant Rebirth")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(2, "Murder")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Swamp")
                .build().advanceToPriority(2, Step.PRECOMBAT_MAIN)
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(2, "Murder", bears).error shouldBe null
            val card = game.findCardsInHand(1, "Verdant Rebirth").first()
            val cast = CastSpell(game.player1Id, card, listOf(ChosenTarget.Permanent(bears)))

            HoldPolicy(intents, refuseUnspendableGrants = true)
                .verdictFor(game.state, game.player1Id, "Verdant Rebirth", cast = cast) shouldNotBe TimingVerdict.NoWindow
        }
    }
}
