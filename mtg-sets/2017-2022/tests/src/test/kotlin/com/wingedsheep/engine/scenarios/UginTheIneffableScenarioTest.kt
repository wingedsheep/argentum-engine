package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.UginTheIneffable
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ugin, the Ineffable (WAR #2).
 *
 *   Colorless spells you cast cost {2} less to cast.
 *   +1: Exile the top card of your library face down and look at it. Create a 2/2 colorless
 *   Spirit creature token. When that token leaves the battlefield, put the exiled card into your hand.
 *   −3: Destroy target permanent that's one or more colors.
 *
 * The +1 links the exiled card to the Spirit through a delayed trigger watching that token.
 */
class UginTheIneffableScenarioTest : ScenarioTestBase() {

    init {
        test("+1 exiles the top card and makes a Spirit; the card comes to hand when the Spirit dies") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ugin, the Ineffable")
                .withCardInLibrary(1, "Shock")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ugin = game.findPermanent("Ugin, the Ineffable")!!
            activate(game, ugin, index = 0)
            game.resolveStack()

            withClue("+1 added a loyalty counter") { loyalty(game, ugin) shouldBe 5 }
            withClue("the top card was exiled") { game.isInExile(1, "Shock") shouldBe true }
            val spirit = game.findPermanent("Spirit Token")
            withClue("a Spirit token was created") { spirit shouldNotBe null }
            withClue("the card stays exiled while the Spirit is on the battlefield") {
                game.isInHand(1, "Shock") shouldBe false
            }

            game.castSpell(1, "Lightning Bolt", spirit).error shouldBe null
            repeat(3) { game.resolveStack() }

            withClue("the Spirit died") { game.findPermanent("Spirit Token") shouldBe null }
            withClue("the exiled card was put into its owner's hand") { game.isInHand(1, "Shock") shouldBe true }
            withClue("and left exile") { game.isInExile(1, "Shock") shouldBe false }
        }

        test("−3 destroys a colored permanent and can't target a colorless one") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ugin, the Ineffable")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Memnite")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ugin = game.findPermanent("Ugin, the Ineffable")!!
            val memnite = game.findPermanent("Memnite")!!
            withClue("a colorless artifact creature is not a legal target") {
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ugin,
                        abilityId = UginTheIneffable.activatedAbilities[1].id,
                        targets = listOf(ChosenTarget.Permanent(memnite)),
                    )
                ).error shouldNotBe null
            }

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = ugin,
                    abilityId = UginTheIneffable.activatedAbilities[1].id,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Memnite") shouldBe true
            loyalty(game, ugin) shouldBe 1
        }

        test("colorless spells cost {2} less") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ugin, the Ineffable")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Juggernaut")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Juggernaut").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Juggernaut") shouldBe true
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = UginTheIneffable.activatedAbilities[index].id,
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
}
