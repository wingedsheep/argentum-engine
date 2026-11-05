package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
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
 * "Change the target of target spell" —
 * [com.wingedsheep.sdk.scripting.effects.ChangeTargetEffect] — reads as
 * [IntentTag.SPELL_REDIRECT]. A redirect pulled onto its own source (Spellskite, Hydroelectric
 * Specimen) or off its controller (Reflecting Mirror) aims at an opposing spell; an unrestricted
 * one (Bolt Bend) stays open to both sides. Copying a target spell is open to both sides too: the
 * copy is ours whoever cast the original.
 *
 * The decision it changes: "Choose a new target" used to take the generic keep-the-best ranking,
 * so Bolt Bend on the opponent's Lightning Bolt sent it into our own best creature. Read through
 * the Bolt's own slot, a harmful spell goes to their side and a helpful one comes to ours.
 */
class SpellRedirectReadingAiTest : ScenarioTestBase() {

    private fun tags(card: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags

    private fun TestGame.castBoltBendAt(stackObject: EntityId) {
        val bend = state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Bolt Bend" }
        execute(CastSpell(player1Id, bend, listOf(ChosenTarget.Spell(stackObject)))).error shouldBe null
    }

    /** Bob casts [spell] at [target]; Alice answers with Bolt Bend and the AI picks the new target. */
    private fun redirectChoice(spell: String, target: String, mana: String): Pair<TestGame, List<EntityId>> {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Bolt Bend")
            .withLandsOnBattlefield(1, "Mountain", 4)
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
        game.castBoltBendAt(cast)
        game.resolveStack()

        val decision = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).respondToDecision(game.state, decision)
        return game to response.shouldBeInstanceOf<CardsSelectedResponse>().selectedCards
    }

    init {
        test("changing a spell's target reads as a redirect, not as protection") {
            for (card in listOf("Bolt Bend", "Misdirection", "Spellskite", "Reflecting Mirror", "Hydroelectric Specimen")) {
                withClue(card) {
                    tags(card) shouldContain IntentTag.SPELL_REDIRECT
                    tags(card) shouldNotContain IntentTag.PROTECTION
                }
            }
        }

        test("a redirect onto its source or off its controller aims at their spell; an open one stays two-sided") {
            val spellskite = cardRegistry.requireCard("Spellskite")
            TargetPolarityAnalyzer.forAbility(spellskite, spellskite.script.activatedAbilities.single()) shouldBe
                listOf(TargetPolarity.HARMFUL)
            val mirror = cardRegistry.requireCard("Reflecting Mirror")
            TargetPolarityAnalyzer.forAbility(mirror, mirror.script.activatedAbilities.single()) shouldBe
                listOf(TargetPolarity.HARMFUL)
            TargetPolarityAnalyzer.forSpell(cardRegistry.requireCard("Bolt Bend")) shouldBe listOf(TargetPolarity.FLEXIBLE)
            TargetPolarityAnalyzer.forSpell(cardRegistry.requireCard("Double Major")) shouldBe listOf(TargetPolarity.FLEXIBLE)
        }

        test("the redirect cards read fully") {
            for (card in listOf("Bolt Bend", "Misdirection", "Spellskite", "Hydroelectric Specimen", "Reroute", "Return the Favor")) {
                val reading = AiReadingAudit.read(cardRegistry.requireCard(card))
                withClue("$card: $reading") { reading.readFully shouldBe true }
            }
        }

        test("Bolt Bend sends their Lightning Bolt into their creature, not into ours") {
            val (game, picked) = redirectChoice("Lightning Bolt", target = "Hill Giant", mana = "Mountain")
            picked shouldContainExactly listOf(game.findPermanent("Grizzly Bears")!!)
        }

        test("Bolt Bend brings their Giant Growth onto our best creature") {
            val (game, picked) = redirectChoice("Giant Growth", target = "Grizzly Bears", mana = "Forest")
            picked shouldContainExactly listOf(game.findPermanent("Hill Giant")!!)
        }
    }
}
