package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Becomes a copy of" — [com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect]
 * — reads as [IntentTag.PERMANENT_COPY]. A copy source the filter keeps on our side is a gift (the
 * best of ours is the one to copy); any other copy source, and the permanent that becomes the copy,
 * are open for the simulation. An instant that turns one creature into a copy until end of turn is a
 * combat trick.
 *
 * The decision it changes: Blade of Shared Souls' "become a copy of another target creature you
 * control" used to take the removal ranking for its unread slot — worst creature first — and turned
 * the equipped Rebel into a copy of a 2/1. Read as a gift, it copies the Hill Giant.
 */
class PermanentCopyReadingAiTest : ScenarioTestBase() {

    private fun tags(card: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags

    init {
        test("becoming a copy reads as a permanent copy") {
            for (card in listOf("Blade of Shared Souls", "Mirage Mirror", "True Polymorph", "Lazav, the Multifarious")) {
                withClue(card) { tags(card) shouldContain IntentTag.PERMANENT_COPY }
            }
        }

        test("an own-side copy source is a gift; any other copy source and the copied-onto permanent stay open") {
            val blade = cardRegistry.requireCard("Blade of Shared Souls").script.triggeredAbilities.single { it.allTargetRequirements.isNotEmpty() }
            TargetPolarityAnalyzer.forRequirements(blade.effect, blade.allTargetRequirements) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
            val polymorph = cardRegistry.requireCard("True Polymorph").script
            TargetPolarityAnalyzer.forRequirements(polymorph.spellEffect!!, polymorph.targetRequirements) shouldBe
                listOf(TargetPolarity.FLEXIBLE, TargetPolarity.FLEXIBLE)
        }

        test("an exile that comes back at the end step is a blink, so Niko copies the best creature of ours") {
            val niko = cardRegistry.requireCard("Niko, Light of Hope")
            TargetPolarityAnalyzer.forAbility(niko, niko.script.activatedAbilities.single { !it.isManaAbility }) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
            val liberate = cardRegistry.requireCard("Liberate").script
            TargetPolarityAnalyzer.forRequirements(liberate.spellEffect!!, liberate.targetRequirements) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
            // The same end-step blink is protection, not removal, in intent — aimed at our own creature.
            tags("Niko, Light of Hope") shouldNotContain IntentTag.REMOVAL
            tags("Liberate") shouldContainExactlyInAnyOrder listOf(IntentTag.PROTECTION)
            // At anyone's creature it still takes a blocker away for the turn.
            tags("Galepowder Mage") shouldContain IntentTag.REMOVAL
        }

        test("an instant that makes one creature a copy until end of turn is a combat trick; a group copy is not") {
            tags("Fleeting Reflection") shouldContain IntentTag.COMBAT_TRICK
            tags("Absorb Identity") shouldNotContain IntentTag.COMBAT_TRICK
        }

        test("the copy cards read fully") {
            for (card in listOf("Blade of Shared Souls", "True Polymorph", "Mirage Mirror", "Absorb Identity", "Niko, Light of Hope")) {
                val reading = AiReadingAudit.read(cardRegistry.requireCard(card))
                withClue("$card: $reading") { reading.readFully shouldBe true }
            }
        }

        test("Blade of Shared Souls copies the best creature we control, not the weakest") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Blade of Shared Souls")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            val cast = game.castSpell(1, "Blade of Shared Souls")
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).respondToDecision(game.state, decision)
            response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.getValue(0) shouldContainExactly listOf(giant)
        }
    }
}
