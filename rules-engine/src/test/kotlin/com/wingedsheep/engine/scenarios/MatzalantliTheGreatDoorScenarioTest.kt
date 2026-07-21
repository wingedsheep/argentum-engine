package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.MatzalantliTheGreatDoor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Matzalantli, the Great Door // The Core (LCI #256).
 *
 * The novel building block is the transform gate:
 * `Conditions.DistinctCardTypesInGraveyard(4, GameObjectFilter.Permanent)` — "four or more permanent
 * types among cards in your graveyard". Proven by activating the transform with a graveyard holding 3
 * permanent types (+ non-permanent cards that must NOT count) → rejected, then 4 permanent types →
 * allowed. Also covers The Core's fathomless-descent mana (X = permanent cards in the graveyard).
 */
class MatzalantliTheGreatDoorScenarioTest : FunSpec({

    val projector = StateProjector()

    val gyCreature = card("GY Creature") {
        manaCost = "{1}{G}"; colorIdentity = "G"; typeLine = "Creature — Bear"; power = 2; toughness = 2
    }
    val gyArtifact = card("GY Artifact") { manaCost = "{1}"; colorIdentity = ""; typeLine = "Artifact" }
    val gyEnchantment = card("GY Enchantment") { manaCost = "{1}{W}"; colorIdentity = "W"; typeLine = "Enchantment" }
    val gyInstant = card("GY Instant") {
        manaCost = "{U}"; colorIdentity = "U"; typeLine = "Instant"; spell { effect = Effects.GainLife(1) }
    }
    val gySorcery = card("GY Sorcery") {
        manaCost = "{B}"; colorIdentity = "B"; typeLine = "Sorcery"; spell { effect = Effects.GainLife(1) }
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(MatzalantliTheGreatDoor, gyCreature, gyArtifact, gyEnchantment, gyInstant, gySorcery)
        )
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    // Matzalantli on the battlefield; graveyard seeded with 3 permanent types (creature, artifact,
    // enchantment) plus two non-permanent cards (instant, sorcery). [includeLand] adds a Swamp for a
    // 4th permanent type (land). Returns Matzalantli's entity id.
    fun setup(driver: GameTestDriver, active: EntityId, includeLand: Boolean): EntityId {
        val matz = driver.putPermanentOnBattlefield(active, "Matzalantli, the Great Door")
        driver.putCardInGraveyard(active, "GY Creature")
        driver.putCardInGraveyard(active, "GY Artifact")
        driver.putCardInGraveyard(active, "GY Enchantment")
        driver.putCardInGraveyard(active, "GY Instant")
        driver.putCardInGraveyard(active, "GY Sorcery")
        if (includeLand) driver.putCardInGraveyard(active, "Swamp")
        return matz
    }

    val transformId = MatzalantliTheGreatDoor.activatedAbilities[1].id

    test("transform is blocked with only 3 permanent types (instant + sorcery don't count)") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val matz = setup(driver, active, includeLand = false)
        driver.giveColorlessMana(active, 4)

        // 3 permanent types (creature/artifact/enchantment); the instant + sorcery are not permanent
        // types, so the "four or more permanent types" gate is unmet — activation is rejected.
        driver.submit(ActivateAbility(playerId = active, sourceId = matz, abilityId = transformId))
            .isSuccess shouldBe false
    }

    test("transform is allowed with 4 permanent types (adds a land) and flips to The Core, a land") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val matz = setup(driver, active, includeLand = true)
        driver.giveColorlessMana(active, 4)

        driver.submit(ActivateAbility(playerId = active, sourceId = matz, abilityId = transformId))
            .isSuccess shouldBe true
        driver.bothPass()
        resolveStack(driver)

        driver.state.getEntity(matz)!!.get<CardComponent>()!!.name shouldBe "The Core"
        projector.project(driver.state).hasType(matz, "LAND") shouldBe true
    }

    test("The Core's fathomless descent taps for X mana of one color, X = permanent cards in graveyard") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val matz = setup(driver, active, includeLand = true) // 4 permanent cards + 2 non-permanent
        driver.giveColorlessMana(active, 4)

        driver.submit(ActivateAbility(playerId = active, sourceId = matz, abilityId = transformId))
            .isSuccess shouldBe true
        driver.bothPass()
        resolveStack(driver)
        driver.untapPermanent(matz)

        // {T}: Add X mana of any one color — choose green; X = the 4 permanent cards (the instant and
        // sorcery in the graveyard are not permanent cards and don't count).
        val manaAbilityId = MatzalantliTheGreatDoor.backFace!!.activatedAbilities[0].id
        driver.submit(
            ActivateAbility(playerId = active, sourceId = matz, abilityId = manaAbilityId, manaColorChoice = Color.GREEN)
        ).isSuccess shouldBe true
        if (driver.isPaused) {
            val decision = driver.pendingDecision!!
            if (decision is ChooseColorDecision) driver.submitDecision(active, ColorChosenResponse(decision.id, Color.GREEN))
        }

        driver.state.getEntity(active)!!.get<ManaPoolComponent>()!!.green shouldBe 4
    }
})
