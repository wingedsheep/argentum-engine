package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.KnowledgeVault
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Knowledge Vault (LEG #281) — {4} Artifact.
 *
 * "{2}, {T}: Exile the top card of your library face down.
 *  {0}: Sacrifice this artifact. If you do, discard your hand, then put all cards exiled with this
 *  artifact into their owner's hand.
 *  When this artifact leaves the battlefield, put all cards exiled with it into their owner's
 *  graveyard."
 *
 * Proves the linked exile across the Vault leaving mid-resolution: the {0} ability sacrifices the
 * Vault and still finds its pile; a second {0} activation in response finds no Vault to sacrifice
 * and does nothing (ruling); and when the Vault is destroyed the leaves trigger bins the pile.
 */
class KnowledgeVaultScenarioTest : FunSpec({
    val exileAbilityId = KnowledgeVault.activatedAbilities[0].id
    val cashOutAbilityId = KnowledgeVault.activatedAbilities[1].id

    val tome = card("Vault Test Tome") { manaCost = "{3}"; typeLine = "Artifact" }
    val shatter = card("Vault Test Shatter") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.Destroy(t) }
    }

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(KnowledgeVault, tome, shatter))
        initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.settle(maxSteps: Int = 40) {
        repeat(maxSteps) {
            when {
                pendingDecision != null -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> return
            }
        }
    }

    /** Exiles the top card of [me]'s library with the Vault and returns that card. */
    fun GameTestDriver.stash(me: EntityId, vault: EntityId): EntityId {
        val top = putCardOnTopOfLibrary(me, tome.name)
        untapPermanent(vault)
        giveColorlessMana(me, 2)
        submitSuccess(ActivateAbility(playerId = me, sourceId = vault, abilityId = exileAbilityId))
        settle()
        state.getZone(me, Zone.EXILE).contains(top).shouldBeTrue()
        return top
    }

    test("{0} sacrifices the Vault, discards the hand, and returns only the Vault's exiled cards") {
        val driver = newDriver()
        val me = driver.player1
        val vault = driver.putPermanentOnBattlefield(me, "Knowledge Vault")
        val first = driver.stash(me, vault)
        val second = driver.stash(me, vault)
        val unrelatedExile = driver.putCardInExile(me, tome.name)
        val handCard = driver.putCardInHand(me, tome.name)

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = vault, abilityId = cashOutAbilityId))
        driver.settle()

        driver.state.getBattlefield().contains(vault).shouldBeFalse()
        driver.state.getZone(me, Zone.GRAVEYARD).contains(vault).shouldBeTrue()
        driver.state.getZone(me, Zone.GRAVEYARD).contains(handCard).shouldBeTrue()
        val hand = driver.state.getZone(me, Zone.HAND)
        hand.contains(first).shouldBeTrue()
        hand.contains(second).shouldBeTrue()
        hand.contains(unrelatedExile).shouldBeFalse()
        driver.state.getZone(me, Zone.EXILE).contains(unrelatedExile).shouldBeTrue()
    }

    test("a second {0} activation in response finds no Vault to sacrifice and does nothing") {
        val driver = newDriver()
        val me = driver.player1
        val vault = driver.putPermanentOnBattlefield(me, "Knowledge Vault")
        val stashed = driver.stash(me, vault)

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = vault, abilityId = cashOutAbilityId))
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = vault, abilityId = cashOutAbilityId))
        driver.state.stack.size shouldBe 2
        driver.settle()

        // The later activation resolved first: the stashed card came to hand. The earlier one
        // could not sacrifice the Vault, so it did not discard that freshly returned hand.
        driver.state.getZone(me, Zone.HAND).contains(stashed).shouldBeTrue()
        driver.state.getZone(me, Zone.GRAVEYARD).contains(stashed).shouldBeFalse()
    }

    test("when the Vault is destroyed, its exiled cards go to their owner's graveyard") {
        val driver = newDriver()
        val me = driver.player1
        val vault = driver.putPermanentOnBattlefield(me, "Knowledge Vault")
        val stashed = driver.stash(me, vault)
        val unrelatedExile = driver.putCardInExile(me, tome.name)

        driver.castSpell(me, driver.putCardInHand(me, shatter.name), listOf(vault)).error shouldBe null
        driver.settle()

        driver.state.getBattlefield().contains(vault).shouldBeFalse()
        driver.state.getZone(me, Zone.GRAVEYARD).contains(stashed).shouldBeTrue()
        driver.state.getZone(me, Zone.EXILE).contains(stashed).shouldBeFalse()
        driver.state.getZone(me, Zone.EXILE).contains(unrelatedExile).shouldBeTrue()
    }
})
