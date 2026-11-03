package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Siren's Ruse (XLN #80).
 *
 *   Exile target creature you control, then return that card to the battlefield under its owner's
 *   control. If a Pirate was exiled this way, draw a card.
 *
 * Pins that the Pirate check reads the creature as it was exiled: a Pirate flickers and draws, a
 * non-Pirate flickers without drawing, and a Pirate token — which never comes back — still draws
 * (ruling 2017-09-29).
 */
class SirensRuseScenarioTest : ScenarioTestBase() {

    init {
        fun game(creature: String, isToken: Boolean = false): TestGame = scenario()
            .withPlayers("Player", "Opponent")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withCardInHand(1, "Siren's Ruse")
            .withCardOnBattlefield(1, creature, isToken = isToken)
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .build()

        test("flickering a Pirate returns it and draws a card") {
            val game = game("Rigging Runner")
            val before = game.findPermanent("Rigging Runner")!!
            val beforeObject = game.state.objectRef(before)!!
            game.state = game.state.withEntity(
                before,
                game.state.getEntity(before)!!.with(
                    CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2))
                )
            )
            val handBefore = game.handSize(1)

            game.castSpell(1, "Siren's Ruse", before).error shouldBe null
            game.resolveStack()

            val after = game.findPermanent("Rigging Runner")
            withClue("the Pirate returns as a new object") {
                after shouldNotBe null
                // EntityId follows the physical card; the zone-change generation identifies the object.
                game.state.objectRef(after!!) shouldNotBe beforeObject
                game.state.isCurrentObject(beforeObject) shouldBe false
                (game.state.getEntity(after)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
            }
            withClue("Siren's Ruse left the hand, one card drawn") {
                game.handSize(1) shouldBe handBefore
            }
        }

        test("flickering a non-Pirate returns it without drawing") {
            val game = game("Grizzly Bears")
            val handBefore = game.handSize(1)

            game.castSpell(1, "Siren's Ruse", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe handBefore - 1
        }

        test("a Pirate token is gone for good but still draws a card") {
            val game = game("Rigging Runner", isToken = true)
            val handBefore = game.handSize(1)

            game.castSpell(1, "Siren's Ruse", game.findPermanent("Rigging Runner")!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Rigging Runner") shouldBe false
            game.handSize(1) shouldBe handBefore
        }
        for (creature in listOf("Rigging Runner", "Grizzly Bears")) {
            test("an exile redirected to the graveyard neither returns nor draws for $creature") {
                val game = game(creature)
                val target = game.findPermanent(creature)!!
                val replacement = game.findPermanent("Island")!!
                game.state = game.state.withEntity(
                    replacement,
                    game.state.getEntity(replacement)!!.with(
                        ReplacementEffectSourceComponent(listOf(
                            RedirectZoneChange(
                                newDestination = Zone.GRAVEYARD,
                                appliesTo = EventPattern.ZoneChangeEvent(
                                    from = Zone.BATTLEFIELD,
                                    to = Zone.EXILE
                                )
                            )
                        ))
                    )
                )
                val handBefore = game.handSize(1)

                game.castSpell(1, "Siren's Ruse", target).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield(creature) shouldBe false
                game.isInGraveyard(1, creature) shouldBe true
                game.handSize(1) shouldBe handBefore - 1
            }
        }
    }
}
