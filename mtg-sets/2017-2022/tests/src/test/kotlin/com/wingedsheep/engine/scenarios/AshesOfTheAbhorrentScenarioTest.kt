package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ashes of the Abhorrent (XLN #2) — "Players can't cast spells from graveyards or activate abilities
 * of cards in graveyards. Whenever a creature dies, you gain 1 life."
 *
 * The activation lock is `PlayersCantActivateAbilities(anyZone = true)` narrowed by
 * `currentlyIn(GRAVEYARD)`: a graveyard ability is locked for every player, while a battlefield
 * ability is untouched.
 */
class AshesOfTheAbhorrentScenarioTest : ScenarioTestBase() {

    private val graveRelic = card("Ashes Test Grave Relic") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.GainLife(3)
            activateFromZone = Zone.GRAVEYARD
        }
    }

    private val totem = card("Ashes Test Totem") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.GainLife(2)
        }
    }

    init {
        cardRegistry.register(graveRelic)
        cardRegistry.register(totem)

        fun relicAbility() = cardRegistry.getCard("Ashes Test Grave Relic")!!.script.activatedAbilities.single()
        fun totemAbility() = cardRegistry.getCard("Ashes Test Totem")!!.script.activatedAbilities.single()

        test("abilities of cards in graveyards can't be activated; battlefield abilities still can") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Ashes of the Abhorrent")
                .withCardInGraveyard(1, "Ashes Test Grave Relic")
                .withCardOnBattlefield(1, "Ashes Test Totem")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val relic = game.findCardsInGraveyard(1, "Ashes Test Grave Relic").single()
            val totemId = game.findPermanent("Ashes Test Totem")!!

            withClue("the graveyard ability is neither offered nor accepted") {
                game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == relic } shouldBe false
                game.execute(ActivateAbility(game.player1Id, relic, relicAbility().id)).error shouldNotBe null
            }
            withClue("a battlefield ability still activates") {
                game.execute(ActivateAbility(game.player1Id, totemId, totemAbility().id)).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 22
            }
        }

        test("without Ashes the graveyard ability activates") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Ashes Test Grave Relic")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val relic = game.findCardsInGraveyard(1, "Ashes Test Grave Relic").single()
            game.execute(ActivateAbility(game.player1Id, relic, relicAbility().id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 23
        }

        test("whenever any creature dies, the Ashes controller gains 1 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ashes of the Abhorrent")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldBe null
            game.getLifeTotal(1) shouldBe 21
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
