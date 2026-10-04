package com.wingedsheep.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Resolution-time casting with an additional mana cost — the `additionalManaCost` of
 * [CastFromCollectionWithoutPayingCostEffect], "cast it by paying {R}{R} in addition to its other
 * costs" (Ogre Battlecaster).
 *
 * Rules pinned here: the extra mana is an additional cost, added to the mana cost when the total
 * cost is determined (CR 601.2f); a total the caster can't pay means the spell isn't cast (CR
 * 601.2h) and the card stays where it is with nothing granted; the stamp is owed by that one cast
 * only. Card-level coverage lives in `OgreBattlecasterScenarioTest`.
 */
class CastFromCollectionAdditionalManaCostTest : FunSpec({

    // "Exile cards from the top of your library until you exile a nonland card. Cast that card by
    // paying {R}{R} in addition to its other costs." No "may": the cost gate is what's under test.
    val Caster = card("Additional Mana Cast Test") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val (nonland, exiled) = gatherUntilMatch(GameObjectFilter.Nonland)
                exile(exiled)
                run(Effects.CastFromCollection(nonland, additionalManaCost = "{R}{R}"))
            }
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Caster))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.redInPool(): Int = state.getEntity(player1)?.get<ManaPoolComponent>()?.red ?: 0

    /** Casts the test instant with [red] extra red floating for the resolution-time cast. */
    fun GameTestDriver.castCaster(red: Int) {
        val caster = putCardInHand(player1, "Additional Mana Cast Test")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, caster).error shouldBe null
        giveMana(player1, Color.RED, red)
        bothPass()
    }

    fun GameTestDriver.isExiled(cardId: EntityId) = cardId in state.getZone(ZoneKey(player1, Zone.EXILE))

    test("the card is cast paying its mana cost plus {R}{R}") {
        val d = newDriver()
        val bolt = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt") // {R}

        d.castCaster(red = 3)

        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null
        d.getStackSpellNames() shouldBe listOf("Lightning Bolt")
        d.redInPool() shouldBe 0
        // The extra cost was owed by that one cast; nothing of it rides the spell onward.
        d.state.getEntity(bolt)?.get<PlayWithCostIncreaseComponent>() shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("the mana cost alone isn't enough: nothing is cast, nothing is spent, nothing is left granted") {
        val d = newDriver()
        val bolt = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")

        d.castCaster(red = 2)
        // Targets are chosen before the total cost is determined (CR 601.2c before 601.2f), so the
        // shortfall surfaces once the cast reaches payment.
        d.submitTargetSelection(d.player1, listOf(d.player2))

        d.getStackSpellNames() shouldBe emptyList()
        d.redInPool() shouldBe 2
        d.isExiled(bolt) shouldBe true
        d.state.getEntity(bolt)!!.get<PlayWithCostIncreaseComponent>() shouldBe null
        d.state.mayPlayPermissions.none { bolt in it.cardIds } shouldBe true
    }

    test("the cast handler charges the colored increase on top of the mana cost") {
        val d = newDriver()
        val courser = d.putCardInExile(d.player1, "Centaur Courser") // {2}{G}
        val (permId, withPermission) = d.state.newEntity()
        d.replaceState(
            withPermission.addMayPlayPermission(
                MayPlayPermission(permId, setOf(courser), d.player1, timestamp = withPermission.timestamp)
            ).updateEntity(courser) {
                it.with(PlayWithCostIncreaseComponent(d.player1, ManaCost.parse("{R}{R}")))
            }
        )

        // {2}{G} paid, but the {R}{R} is missing: generic mana can't stand in for a colored increase.
        d.giveMana(d.player1, Color.GREEN, 5)
        d.submitExpectFailure(CastSpell(d.player1, courser))
        d.isExiled(courser) shouldBe true

        d.giveMana(d.player1, Color.RED, 2)
        d.submit(CastSpell(d.player1, courser)).error shouldBe null
        d.getStackSpellNames() shouldBe listOf("Centaur Courser")
        d.redInPool() shouldBe 0
    }

    test("a generic tax still reads as generic mana") {
        PlayWithCostIncreaseComponent(EntityId("p"), amount = 2).cost shouldBe ManaCost.parse("{2}")
    }

    test("the SDK shape: an additional mana cost rides a paid cast and reads as the Oracle text does") {
        val effect = Effects.CastFromCollection(CollectionSlot("card"), additionalManaCost = "{R}{R}")
            as CastFromCollectionWithoutPayingCostEffect
        effect.additionalManaCost shouldBe ManaCost.parse("{R}{R}")
        effect.description shouldBe "Cast that card by paying {R}{R} in addition to its other costs"
        shouldThrow<IllegalArgumentException> {
            CastFromCollectionWithoutPayingCostEffect(from = "card", additionalManaCost = ManaCost.parse("{R}{R}"))
        }
    }
})
