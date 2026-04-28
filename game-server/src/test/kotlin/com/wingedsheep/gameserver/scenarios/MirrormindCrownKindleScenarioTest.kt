package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mirrormind Crown's replacement effect should fire when ANY token would be created
 * — including a token created by Kindle the Inner Flame (which uses
 * CreateTokenCopyOfTargetEffect rather than the generic CreateTokenEffect).
 *
 * Per the printed rulings:
 *  - Mirrormind Crown's tokens copy exactly what was printed on the equipped creature
 *    (so no haste, no end-step sacrifice trigger from Kindle).
 *  - Kindle's added keywords / triggered abilities are dropped because Mirrormind
 *    creates its own replacement tokens.
 */
class MirrormindCrownKindleScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.attachTo(attachmentName: String, creatureName: String) {
        val attachmentId = findPermanent(attachmentName)!!
        val creatureId = findPermanent(creatureName)!!

        var attachmentContainer = state.getEntity(attachmentId)!!
        attachmentContainer = attachmentContainer.with(AttachedToComponent(creatureId))
        state = state.withEntity(attachmentId, attachmentContainer)

        var creatureContainer = state.getEntity(creatureId)!!
        val existing = creatureContainer.get<AttachmentsComponent>()
        val newAttachments = (existing?.attachedIds ?: emptyList()) + attachmentId
        creatureContainer = creatureContainer.with(AttachmentsComponent(newAttachments))
        state = state.withEntity(creatureId, creatureContainer)
    }

    init {
        context("Mirrormind Crown + Kindle the Inner Flame") {
            test("Mirrormind triggers when Kindle would create a token copy and replaces it with a printed copy of the equipped creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Kindle the Inner Flame")
                    .withCardOnBattlefield(1, "Burdened Stoneback")
                    .withCardOnBattlefield(1, "Mirrormind Crown")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attachTo("Mirrormind Crown", "Burdened Stoneback")

                val burdenedId = game.findPermanent("Burdened Stoneback")!!
                val castResult = game.castSpell(1, "Kindle the Inner Flame", burdenedId)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolving Kindle should pause for Mirrormind Crown's yes/no decision.
                game.resolveStack()

                val pending = game.state.pendingDecision
                withClue("Mirrormind Crown should pause Kindle's resolution for a yes/no decision") {
                    pending shouldNotBe null
                    pending!!.context.sourceName shouldBe "Mirrormind Crown"
                }

                // Player accepts the replacement.
                game.answerYesNo(true)

                // Drain anything still on the stack (none expected — replacement creates tokens directly).
                if (game.state.stack.isNotEmpty()) game.resolveStack()

                val burdenedStonebacks = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Burdened Stoneback"
                }
                withClue("Should have original Burdened Stoneback + 1 token copy from Mirrormind") {
                    burdenedStonebacks.size shouldBe 2
                }

                val tokenId = burdenedStonebacks.first { it != burdenedId }
                val tokenContainer = game.state.getEntity(tokenId)!!
                val tokenCard = tokenContainer.get<CardComponent>()!!

                withClue("Replacement token should be a token") {
                    tokenContainer.get<TokenComponent>() shouldBe TokenComponent
                }
                withClue("Mirrormind copy must NOT inherit Kindle's added haste keyword — it copies the printed card") {
                    tokenCard.baseKeywords shouldNotContain Keyword.HASTE
                }

                // Burdened Stoneback's printed "enters with two -1/-1 counters" should still apply
                // to the Mirrormind copy (per the printed rulings: tokens enter with any printed
                // "enters with" effects of the equipped creature).
                val counters = tokenContainer.get<CountersComponent>()
                withClue("Mirrormind token of Burdened Stoneback should enter with two -1/-1 counters") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
                }
            }

            test("declining Mirrormind's replacement falls back to Kindle's normal token (haste + sac trigger)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Kindle the Inner Flame")
                    .withCardOnBattlefield(1, "Burdened Stoneback")
                    .withCardOnBattlefield(1, "Mirrormind Crown")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attachTo("Mirrormind Crown", "Burdened Stoneback")

                val burdenedId = game.findPermanent("Burdened Stoneback")!!
                game.castSpell(1, "Kindle the Inner Flame", burdenedId)
                game.resolveStack()

                game.state.pendingDecision shouldNotBe null
                game.answerYesNo(false)
                if (game.state.stack.isNotEmpty()) game.resolveStack()

                val burdenedStonebacks = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Burdened Stoneback"
                }
                withClue("Should still have original + 1 token (Kindle's normal token)") {
                    burdenedStonebacks.size shouldBe 2
                }

                val tokenId = burdenedStonebacks.first { it != burdenedId }
                val tokenCard = game.state.getEntity(tokenId)!!.get<CardComponent>()!!
                withClue("Kindle's own token should retain the added haste keyword") {
                    tokenCard.baseKeywords.contains(Keyword.HASTE) shouldBe true
                }
            }
        }
    }
}
