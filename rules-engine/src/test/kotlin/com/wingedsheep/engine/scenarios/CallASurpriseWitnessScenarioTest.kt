package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Call a Surprise Witness (MKM #6) — {1}{W} Sorcery.
 *
 * "Return target creature card with mana value 3 or less from your graveyard to the battlefield.
 *  Put a flying counter on it. It's a Spirit in addition to its other types."
 *
 * The risk in this shape is the target reference surviving the graveyard → battlefield move: if it
 * doesn't, the counter and the subtype land on nothing and the card degrades into a plain
 * reanimation spell. Both riders are therefore asserted on the returned permanent, along with "in
 * addition to" — the printed creature types must still be there — and the mana-value gate on the
 * target.
 */
class CallASurpriseWitnessScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Call a Surprise Witness") {

            test("returns a cheap creature with a flying counter and the Spirit type added") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Call a Surprise Witness")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Call a Surprise Witness", 1, "Grizzly Bears")
                    .error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                withClue("the Bears left the graveyard for the battlefield") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }

                withClue("a flying counter, not a granted keyword") {
                    game.state.getEntity(bears)?.get<CountersComponent>()
                        ?.getCount(CounterType.FLYING) shouldBe 1
                }

                val projected = projector.project(game.state)
                withClue("the counter grants flying, and Spirit is added to the printed Bear type") {
                    projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
                    projected.hasSubtype(bears, "Spirit") shouldBe true
                    projected.hasSubtype(bears, "Bear") shouldBe true
                }
            }

            test("a creature card with mana value 4 or more is not a legal target") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Call a Surprise Witness")
                    .withCardInGraveyard(1, "Serra Angel")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .build()

                withClue("Serra Angel costs {3}{W}{W} — above the mana value 3 gate") {
                    game.castSpellTargetingGraveyardCard(1, "Call a Surprise Witness", 1, "Serra Angel")
                        .error shouldNotBe null
                }
                game.isInGraveyard(1, "Serra Angel") shouldBe true
            }
        }
    }
}
