package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Copying a spell or ability — [com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect] and
 * its kin — reads as [IntentTag.SPELL_COPY], and rewards the instants and sorceries a deck casts.
 *
 * The decision it changes: "Choose new targets for the copy" took the generic removal-first
 * ranking, so Fork on the opponent's Giant Growth pumped their creature a second time. Read through
 * the copied spell's own slot, the copy is ours: a helpful one comes to our side, a harmful one
 * goes to theirs.
 */
class SpellCopyReadingAiTest : ScenarioTestBase() {

    private fun tags(card: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags

    /** Bob casts [spell] at [target]; Alice Forks it and the AI picks the copy's new target. */
    private fun forkChoice(spell: String, target: String, mana: String): Pair<TestGame, List<EntityId>> {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Fork")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(1, "Savannah Lions")
            .withCardInHand(2, spell)
            .withLandsOnBattlefield(2, mana, 1)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Raging Goblin")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(2, spell, game.findPermanent(target)!!).error shouldBe null
        val cast = game.state.stack.last()
        game.passPriority().error shouldBe null
        val fork = game.state.getHand(game.player1Id)
            .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Fork" }
        game.execute(CastSpell(game.player1Id, fork, listOf(ChosenTarget.Spell(cast)))).error shouldBe null
        game.resolveStack()

        val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).respondToDecision(game.state, decision)
        return game to response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.values.flatten()
    }

    init {
        test("copying a spell or ability reads as a spell copy") {
            for (card in listOf("Fork", "Double Major", "Lithoform Engine", "Echo, Perceptive Prodigy", "Kirol, Attentive First-Year")) {
                withClue(card) { tags(card) shouldContain IntentTag.SPELL_COPY }
            }
        }

        test("an instant-or-sorcery copier rewards spells; a creature-spell copier doesn't") {
            for (card in listOf("Fork", "Lithoform Engine")) {
                withClue(card) { SynergyAxes.of(cardRegistry.requireCard(card)).rewards shouldContain SynergyAxis.SPELLS }
            }
            // Copying a creature spell is a second creature, not a second spell.
            SynergyAxes.of(cardRegistry.requireCard("Double Major")).rewards shouldNotContain SynergyAxis.SPELLS
        }

        test("the spell-copy cards read fully") {
            for (card in listOf("Fork", "Double Major", "Lithoform Engine", "Kirol, Attentive First-Year")) {
                val reading = AiReadingAudit.read(cardRegistry.requireCard(card))
                withClue("$card: $reading") { reading.readFully shouldBe true }
            }
        }

        test("Fork's copy of their Giant Growth pumps our best creature, not theirs") {
            val (game, picked) = forkChoice("Giant Growth", target = "Grizzly Bears", mana = "Forest")
            picked shouldContainExactly listOf(game.findPermanent("Hill Giant")!!)
        }

        test("Fork's copy of their Lightning Bolt goes at their side") {
            val (game, picked) = forkChoice("Lightning Bolt", target = "Hill Giant", mana = "Mountain")
            val theirs = setOf(game.player2Id, game.findPermanent("Grizzly Bears")!!, game.findPermanent("Raging Goblin")!!)
            withClue("picked $picked") { (picked.single() in theirs) shouldBe true }
        }
    }
}
