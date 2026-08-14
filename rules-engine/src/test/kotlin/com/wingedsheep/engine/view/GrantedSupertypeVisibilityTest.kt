package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.AddCardTypeEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * A granted supertype (Origin of Spider-Man's "it becomes a legendary Spider Hero in addition to
 * its other types", the Ring emblem's "your Ring-bearer is legendary") has to reach the client's
 * rendered type line, not just the projection the engine reasons over.
 *
 * Supertypes share the projected `types` set with card types and subtypes
 * ([StateProjector.extractTypes]), so [ClientStateTransformer] must read them from the projection.
 * Reading base `CardComponent.typeLine.supertypes` instead dropped granted supertypes entirely —
 * "LEGENDARY" isn't a [com.wingedsheep.sdk.core.CardType], so the projected-card-types filter
 * discarded it too and the player saw a plain "Creature — Cat".
 */
class GrantedSupertypeVisibilityTest : FunSpec({

    val projector = StateProjector()

    // {0} sorcery mirroring Origin of Spider-Man chapter II's type grant (minus the counter).
    val legendMaker = card("Legend Maker") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Target creature becomes a legendary Spider Hero in addition to its other types."
        spell {
            val creature = target("creature", Targets.Creature)
            effect = Effects.Composite(
                listOf(
                    AddCardTypeEffect("LEGENDARY", creature, Duration.Permanent),
                    Effects.AddCreatureType("Spider", creature, Duration.Permanent),
                    Effects.AddCreatureType("Hero", creature, Duration.Permanent)
                )
            )
        }
    }

    // {0} sorcery mirroring Impostor Syndrome's "a copy of it, except it isn't legendary".
    val impostor = card("Impostor Maker") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Create a token that's a copy of target creature, except it isn't legendary."
        spell {
            val creature = target("creature", Targets.Creature)
            effect = Effects.CreateTokenCopyOfTarget(
                target = creature,
                removedSupertypes = setOf(Supertype.LEGENDARY)
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + legendMaker + impostor)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castOn(playerId: EntityId, cardName: String, targetId: EntityId) {
        submit(
            CastSpell(
                playerId = playerId,
                cardId = putCardInHand(playerId, cardName),
                targets = listOf(ChosenTarget.Permanent(targetId)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        bothPass()
    }

    fun view(driver: GameTestDriver, playerId: EntityId) =
        ClientStateTransformer(cardRegistry = driver.cardRegistry)
            .transform(driver.state, viewingPlayerId = playerId)

    test("a creature granted LEGENDARY renders 'Legendary' in the client type line") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val lion = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        view(driver, player).cards[lion].shouldNotBeNull().let {
            it.typeLine shouldBe "Creature — Cat"
            it.legendaryByEffect shouldBe false
        }

        driver.castOn(player, "Legend Maker", lion)

        projector.project(driver.state).isLegendary(lion) shouldBe true
        view(driver, player).cards[lion].shouldNotBeNull().let {
            it.typeLine shouldBe "Legendary Creature — Cat Spider Hero"
            // The printed frame is non-legendary, so the client needs the at-a-glance chip too.
            it.legendaryByEffect shouldBe true
        }
    }

    test("the granted supertype is visible to the opponent too") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val lion = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        driver.castOn(player, "Legend Maker", lion)

        view(driver, opponent).cards[lion].shouldNotBeNull()
            .typeLine shouldBe "Legendary Creature — Cat Spider Hero"
    }

    test("a printed legendary permanent still renders 'Legendary'") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val prospector = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")

        view(driver, player).cards[prospector].shouldNotBeNull().let {
            it.typeLine shouldBe "Legendary Creature — Monkey Pirate"
            // Printed legendary — the "granted" chip is for effects only.
            it.legendaryByEffect shouldBe false
        }
    }

    // CR 708.2a: a face-down permanent is a 2/2 creature with no name, no subtypes — and no
    // supertypes. Opponents get a hardcoded masked card, but the *controller* is shown the real
    // name and so falls through the normal transform path; reading base supertypes there made
    // their own face-down legendary creature read "Legendary Creature" while its card types and
    // subtypes had already been masked down to "Creature" by the projection.
    test("a face-down printed-legendary creature is not Legendary, even to its controller") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val prospector = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        driver.replaceState(
            driver.state.updateEntity(prospector) { it.with(FaceDownComponent) }
        )

        view(driver, player).cards[prospector].shouldNotBeNull().let {
            it.typeLine shouldBe "Creature"
            it.legendaryByEffect shouldBe false
        }
        // The opponent's masked view was never affected, but pin it so the two stay in step.
        view(driver, opponent).cards[prospector].shouldNotBeNull().let {
            it.typeLine shouldBe "Creature"
            it.legendaryByEffect shouldBe false
        }
    }

    // The two legend flags drive chips that render at the same spot and say opposite things, so
    // a permanent that is *both* a de-legendarized copy and a grant target must set only one.
    test("granting Legendary back to a non-legendary copy clears nonLegendaryCopy") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val prospector = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        driver.castOn(player, "Impostor Maker", prospector)

        val token = driver.getPermanents(player).first { it != prospector }
        view(driver, player).cards[token].shouldNotBeNull().let {
            it.typeLine shouldBe "Creature — Monkey Pirate"
            it.nonLegendaryCopy shouldBe true
            it.legendaryByEffect shouldBe false
        }

        driver.castOn(player, "Legend Maker", token)

        projector.project(driver.state).isLegendary(token) shouldBe true
        view(driver, player).cards[token].shouldNotBeNull().let {
            it.typeLine shouldBe "Legendary Creature — Monkey Pirate Spider Hero"
            // It really is legendary again, so "Not Legendary" would be a lie — only one chip.
            it.nonLegendaryCopy shouldBe false
            it.legendaryByEffect shouldBe true
        }
    }
})
