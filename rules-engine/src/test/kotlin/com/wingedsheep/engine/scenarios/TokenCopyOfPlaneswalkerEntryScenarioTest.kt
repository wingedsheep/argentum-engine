package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * CR 306.5b for token copies: a token that's a copy of a planeswalker enters with that
 * planeswalker's printed loyalty. Loyalty is a copiable value (CR 707.2), so the token has a
 * printed loyalty number of its own to place; without those counters state-based actions
 * (CR 704.5i) put it into the graveyard the instant it enters — and a token that hits the
 * graveyard simply ceases to exist.
 *
 * Token minting uses the ad-hoc `BattlefieldEntry.place` insertion path, which deliberately skips
 * the enters-with setup that `ZoneTransitionService.moveToZone` runs, so the token executors call
 * the shared entry hooks themselves — exactly as they already do for Saga lore counters (see
 * [TokenCopyOfSagaEntryScenarioTest]). This test proves the planeswalker hook generally, driven by
 * a plain "create a token that's a copy of target planeswalker" instant rather than any one card.
 */
class TokenCopyOfPlaneswalkerEntryScenarioTest : ScenarioTestBase() {

    private val echo = card("Test Walker Echo") {
        manaCost = "{2}{U}"
        colorIdentity = "U"
        typeLine = "Instant"
        oracleText = "Create a token that's a copy of target planeswalker."
        spell {
            target = Targets.Planeswalker
            effect = Effects.CreateTokenCopyOfTarget(EffectTarget.ContextTarget(0))
        }
    }

    private fun loyaltyOf(entityId: EntityId, state: com.wingedsheep.engine.state.GameState): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    init {
        cardRegistry.register(echo)

        test("a token copy of a planeswalker enters with the copied printed loyalty and survives") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Test Walker Echo")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(1, "Ajani, Caller of the Pride")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val original = game.findPermanent("Ajani, Caller of the Pride")!!
            val cast = game.castSpell(1, "Test Walker Echo", original)
            withClue("casting the copy spell should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val copies = game.state.getZone(game.player1Id, Zone.BATTLEFIELD).filter { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Ajani, Caller of the Pride"
            }
            withClue("the token is still on the battlefield — 0 loyalty would have binned it") {
                copies.size shouldBe 2
            }

            val token = copies.first { game.state.getEntity(it)?.get<TokenComponent>() != null }
            withClue("the token copied Ajani's printed loyalty 4") {
                loyaltyOf(token, game.state) shouldBe 4
            }
        }
    }
}
