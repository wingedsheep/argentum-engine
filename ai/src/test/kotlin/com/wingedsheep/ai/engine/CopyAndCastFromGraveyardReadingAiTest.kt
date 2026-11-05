package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.GraveyardSynergy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Exile up to one target nonland card of each card type from your graveyard. Copy those cards.
 * You may cast any number of spells … from among the copies without paying their mana costs"
 * (Uldaros Theorix): the cards named are the ones we get to cast, so the slot is a gift — the
 * best card in our graveyard is the one to name. Hour of Eternity's exile-and-copy reading, with
 * the copy step and the free cast of the copies read through.
 *
 * Unread, the slot fell to the removal ranking, which names our *worst* card of each type.
 */
class CopyAndCastFromGraveyardReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("the exile-copy-cast pipeline reads its graveyard slot as a gift") {
            val uldaros = cardRegistry.getCard("Uldaros Theorix")!!
            val trigger = uldaros.script.triggeredAbilities.single()
            intents.effectTargetPolarities(trigger.effect, trigger.allTargetRequirements) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
        }

        test("a target only our own graveyard holds makes the card a graveyard payoff") {
            listOf("Uldaros Theorix", "Hour of Eternity").forEach { name ->
                withClue(name) { GraveyardSynergy.isPayoff(cardRegistry.getCard(name)!!) shouldBe true }
            }
        }

        test("Uldaros Theorix names our best creature card to recast, not our worst") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInHand(1, "Uldaros Theorix")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Serra Angel")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.castSpell(1, "Uldaros Theorix").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val ai = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
            val response = ai.respondToDecision(game.state, decision)
            response.shouldBeInstanceOf<TargetsResponse>()
            val picked = response.selectedTargets.values.flatten()
                .map { game.state.getEntity(it)?.get<CardComponent>()?.name }
            withClue("the exiled cards are the ones we cast for free: named $picked") {
                picked shouldBe listOf("Serra Angel")
            }
        }
    }
}
