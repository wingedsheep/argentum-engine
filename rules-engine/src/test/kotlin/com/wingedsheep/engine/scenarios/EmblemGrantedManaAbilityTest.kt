package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.EmblemActivatedAbilityComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A mana ability granted by a permanent emblem ("Swamps you control have '{T}: Add {B}{B}{B}{B}.'"
 * — Liliana of the Dark Realms' −6). The emblem lives in no zone, so the battlefield walks that
 * find printed `GrantActivatedAbility` statics never see it. These pin that both mana readers —
 * the mana-ability enumerator (manual taps) and the auto-payer's `ManaStaticsIndex` — pick the
 * grant up, and only on the emblem controller's matching permanents.
 */
class EmblemGrantedManaAbilityTest : FunSpec({

    val swampEmblemSorcery = card("Swamp Mana Emblem") {
        val swampMana = grantedActivatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(Color.BLACK, 4)
            manaAbility = true
        }
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.CreatePermanentEmblem(
                groupFilter = GroupFilter(GameObjectFilter.Land.withSubtype("Swamp").youControl()),
                grantedActivatedAbilities = listOf(swampMana),
                emblemDescription = "Swamps you control have \"{T}: Add {B}{B}{B}{B}.\""
            )
        }
    }

    val fourDrop = card("Black Four Drop") {
        manaCost = "{3}{B}"
        typeLine = "Creature — Horror"
        power = 4
        toughness = 4
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(swampEmblemSorcery, fourDrop))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.giveEmblem(playerId: EntityId) {
        giveMana(playerId, Color.BLACK, 1)
        castSpell(playerId, putCardInHand(playerId, "Swamp Mana Emblem"))
        bothPass()
    }

    fun GameTestDriver.emblemAbilityIds() = state.entities.values
        .mapNotNull { it.get<EmblemActivatedAbilityComponent>() }
        .flatMap { grant -> grant.abilities.map { it.id } }

    fun GameTestDriver.emblemManaActions(playerId: EntityId, sourceId: EntityId) =
        legalActions(playerId).map { it.action }.filterIsInstance<ActivateAbility>()
            .filter { it.sourceId == sourceId && it.abilityId in emblemAbilityIds() }


    test("a Swamp the emblem covers offers the granted mana ability and taps for {B}{B}{B}{B}") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.giveEmblem(active)
        val swamp = driver.putLandOnBattlefield(active, "Swamp")

        val granted = driver.emblemManaActions(active, swamp)
        granted.size shouldBe 1

        driver.submitSuccess(granted.single())
        driver.state.getEntity(active)!!.get<ManaPoolComponent>()!!.black shouldBe 4
    }

    test("the auto-payer counts the grant: one Swamp pays a four-mana spell") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.giveEmblem(active)
        driver.putLandOnBattlefield(active, "Swamp")

        driver.castSpell(active, driver.putCardInHand(active, "Black Four Drop"))
        driver.bothPass()
        (driver.findPermanent(active, "Black Four Drop") != null) shouldBe true
    }

    test("an opponent's Swamp gets nothing from your emblem") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.giveEmblem(active)
        val theirSwamp = driver.putLandOnBattlefield(opponent, "Swamp")

        driver.emblemManaActions(opponent, theirSwamp).size shouldBe 0
    }
})
