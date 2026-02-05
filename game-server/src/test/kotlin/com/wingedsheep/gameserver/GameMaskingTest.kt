package com.wingedsheep.gameserver

import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class GameMaskingTest : GameServerTestBase() {
    init {
        context("State Masking") {
            test("player sees their own hand contents with full entity IDs") {
                val ctx = setupGame(monoGreenLands)
                val state = ctx.player1.client.requireLatestState()
                val ownHand = state.hand(ctx.player1.id)

                withClue("Own hand should be visible") {
                    ownHand.isVisible shouldBe true
                }
                withClue("Own hand should contain 7 card entity IDs") {
                    ownHand.cardIds shouldHaveSize 7
                }
            }

            test("player cannot see opponent hand contents but sees card count") {
                val ctx = setupGame(monoGreenLands)
                val state = ctx.player1.client.requireLatestState()
                val opponentHand = state.zones.find {
                    it.zoneId.zoneType == Zone.HAND && it.zoneId.ownerId == ctx.player2.id
                } ?: error("Opponent hand not found")

                withClue("Opponent hand should be marked not visible") {
                    opponentHand.isVisible shouldBe false
                }
                withClue("Opponent hand entity IDs should be hidden") {
                    opponentHand.cardIds.shouldBeEmpty()
                }
                withClue("Opponent hand card count should still be visible") {
                    opponentHand.size shouldBe 7
                }
            }

            test("battlefield is fully visible to both players") {
                val ctx = setupGame(monoGreenLands)
                val state1 = ctx.player1.client.requireLatestState()
                val state2 = ctx.player2.client.requireLatestState()

                state1.battlefield().isVisible shouldBe true
                state2.battlefield().isVisible shouldBe true
            }
        }
    }
}
