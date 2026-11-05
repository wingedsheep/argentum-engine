package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * Taking cards out of a graveyard — "exile target card from a graveyard" (Scavenging Ooze,
 * Carrion Locust, Mourner's Shield's imprint), "exile target player's graveyard" (Tormod's Crypt,
 * Bojuka Bog), "put target card from a graveyard on the bottom of its owner's library"
 * (Junktroller) — reads as [IntentTag.GRAVEYARD_HATE].
 *
 * Before, the single-card exile read as exile *removal*: Scavenging Ooze was priced as a creature
 * with repeatable removal and Mourner's Shield as a 4-point removal engine; the graveyard-exile
 * pipeline and the tuck read as nothing at all, and Junktroller tucked its own controller's
 * cheapest card rather than the opponent's best.
 */
class GraveyardHateReadingAiTest : ScenarioTestBase() {

    private fun intent(name: String) = CardIntentAnalyzer.analyze(cardRegistry.getCard(name)!!)
    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("exiling or tucking a card from any graveyard reads as graveyard hate, not removal") {
            listOf(
                "Scavenging Ooze", "Carrion Locust", "Ashnod's Harvester", "Mourner's Shield",
                "Tormod's Crypt", "Bojuka Bog", "Junktroller",
            ).forEach { name ->
                withClue(name) {
                    val tags = intent(name).tags
                    tags shouldContain IntentTag.GRAVEYARD_HATE
                    tags shouldNotContain IntentTag.REMOVAL
                    tags shouldNotContain IntentTag.EXILE_REMOVAL
                }
            }
        }

        test("a graveyard card exiled to be copied or cast is recursion, not hate") {
            listOf("The Scarab God", "Dimir Doppelganger", "Daring Waverider").forEach { name ->
                withClue(name) {
                    val tags = intent(name).tags
                    tags shouldContain IntentTag.RECURSION
                    tags shouldNotContain IntentTag.GRAVEYARD_HATE
                }
            }
            withClue("The Scarab God copies the best creature card in any graveyard") {
                val ability = cardRegistry.getCard("The Scarab God")!!.script.activatedAbilities
                    .first { it.targetRequirements.isNotEmpty() }
                intents.effectTargetPolarities(ability.effect, ability.targetRequirements) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
        }

        test("our own graveyard put back on our library stays recursion") {
            val tags = intent("Mortuary Mire").tags
            tags shouldContain IntentTag.RECURSION
            tags shouldNotContain IntentTag.GRAVEYARD_HATE
        }

        test("a graveyard exiled is aimed at the opponent") {
            withClue("Tormod's Crypt: exile target player's graveyard") {
                val ability = cardRegistry.getCard("Tormod's Crypt")!!.script.activatedAbilities.single()
                intents.effectTargetPolarities(ability.effect, ability.targetRequirements) shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("each player's graveyard shuffled into their library is graveyard hate; one's own is a refill") {
            val survive = cardRegistry.getCard("Struggle // Survive")!!.cardFaces.first { it.name == "Survive" }
            CardIntentAnalyzer.analyzeEffect(survive.script.spellEffect!!, survive.script.targetRequirements).tags shouldContain
                IntentTag.GRAVEYARD_HATE
            withClue("Feldon's Cane shuffles only its controller's graveyard back: a refill, not hate") {
                intent("Feldon's Cane").tags shouldNotContain IntentTag.GRAVEYARD_HATE
            }
        }

        test("our own graveyard's creature cards exiled to become token copies are recursion, aimed at our best") {
            val hour = cardRegistry.getCard("Hour of Eternity")!!
            intent("Hour of Eternity").tags shouldContain IntentTag.RECURSION
            intent("Hour of Eternity").tags shouldNotContain IntentTag.GRAVEYARD_HATE
            intents.effectTargetPolarities(hour.script.spellEffect!!, hour.script.targetRequirements) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
        }

        test("graveyard hate is priced well below removal") {
            withClue("Mourner's Shield no longer prices as a repeatable removal engine") {
                intent("Mourner's Shield").staticPriorValue shouldBeLessThan 4.0
            }
            withClue("Tormod's Crypt is a one-shot answer waiting on the board, above an unreadable permanent") {
                intent("Tormod's Crypt").staticPriorValue shouldBeGreaterThan CardIntent.UNKNOWN.staticPriorValue
            }
            val ooze = intent("Scavenging Ooze").staticPriorValue - CardIntent.UNKNOWN.staticPriorValue
            withClue("Scavenging Ooze keeps a term for its repeatable hate, below repeatable removal's 1.5") {
                ooze shouldBeGreaterThan 0.0
                ooze shouldBeLessThan 1.5
            }
        }

        test("Junktroller tucks the opponent's best graveyard card, not our cheapest") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withCardOnBattlefield(1, "Junktroller")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Raging Goblin")
                .withCardInGraveyard(2, "Serra Angel")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val junktroller = game.findPermanent("Junktroller")!!
            val activate = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? ActivateAbility)?.sourceId == junktroller }

            val filled = TargetSelection.fillHeuristically(
                game.state, activate, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as ActivateAbility

            val target = filled.targets.single().toEntityId()
            withClue("the tuck is graveyard hate: the opponent's Serra Angel, not their Raging Goblin or our Grizzly Bears") {
                game.state.getEntity(target)?.get<CardComponent>()?.name shouldBe "Serra Angel"
                game.state.getZone(game.player2Id, Zone.GRAVEYARD) shouldContain target
            }
        }
    }
}
