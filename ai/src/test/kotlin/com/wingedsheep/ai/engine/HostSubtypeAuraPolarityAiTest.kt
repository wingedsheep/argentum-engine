package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.CreatureTypeSynergy
import com.wingedsheep.ai.engine.knowledge.HostSplit
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Auras whose gift or curse turns on the enchanted creature's type — Sea Legs' "+0/+2 as long as
 * it's a Pirate. Otherwise, it gets -2/-0", Gift of Fangs, Bonds of Faith, Clutch of Undeath.
 *
 * One polarity for the slot could only ever read them UNKNOWN: they are a pump on one kind of
 * creature and a curse on every other. Read once per kind of host they are FLEXIBLE with a
 * [HostSplit], which the target selector resolves per candidate: our Pirates and their
 * non-Pirates are the right targets, and an Aura that can only land on our non-Pirates or their
 * Pirates is held.
 */
class HostSubtypeAuraPolarityAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun seaLegsCast(game: TestGame) =
        GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
            val spell = action.action as? CastSpell ?: return@first false
            game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Sea Legs"
        }

    init {
        test("a creature-type-gated Aura reads flexible, split by whether the host has the type") {
            listOf(
                "Sea Legs" to "Pirate", "Gift of Fangs" to "Vampire",
                "Bonds of Faith" to "Human", "Clutch of Undeath" to "Zombie",
            ).forEach { (name, type) ->
                val card = cardRegistry.getCard(name)!!
                withClue("$name: a gift on a $type, a curse on anything else") {
                    TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.FLEXIBLE)
                    TargetPolarityAnalyzer.hostSplit(card) shouldBe
                        HostSplit(Subtype(type), TargetPolarity.BENEFICIAL, TargetPolarity.HARMFUL)
                }
            }
        }

        test("the type the Aura is gated on is a tribe it asks for, and a -X/-X Aura is removal") {
            CreatureTypeSynergy.rewarded(cardRegistry.getCard("Sea Legs")!!) shouldContain Subtype("Pirate")
            val deadWeight = CardIntentAnalyzer.analyze(cardRegistry.getCard("Dead Weight")!!).tags
            withClue("Dead Weight's -2/-2 answers what it enchants; it is no pump") {
                deadWeight shouldContain IntentTag.REMOVAL
                deadWeight shouldNotContain IntentTag.PUMP
            }
        }

        test("Sea Legs ranks our Pirate and their non-Pirate ahead of the wrong-side creatures") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Sea Legs")
                .withCardOnBattlefield(1, "Deadeye Plunderers")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Storm Fleet Aerialist")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = seaLegsCast(game)
            val rank = TargetSelection.ranker(game.state, action, game.player1Id, intents, polarityFromEffect = true)
            val candidates = TargetSelection.targetInfosFor(action)!!.single().validTargets
            val names = candidates.sortedByDescending(rank).map { id ->
                val name = game.state.getEntity(id)!!.get<CardComponent>()!!.name
                val side = if (TargetSelection.isOurs(game.state, id, game.player1Id)) "ours" else "theirs"
                "$name ($side)"
            }

            names.take(2) shouldContainExactlyInAnyOrder listOf("Deadeye Plunderers (ours)", "Grizzly Bears (theirs)")
            TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, intents) shouldBe false
        }

        test("Sea Legs with only our non-Pirates and their Pirates to enchant is held") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Sea Legs")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Storm Fleet Aerialist")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            withClue("+0/+2 on their Pirate and -2/-0 on our Bears both help the opponent") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, seaLegsCast(game), game.player1Id, intents) shouldBe true
            }
        }
    }
}
