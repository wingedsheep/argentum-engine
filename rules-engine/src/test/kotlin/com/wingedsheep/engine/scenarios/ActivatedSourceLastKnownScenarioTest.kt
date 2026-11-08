package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Whenever a player activates an ability of an artifact / land on the battlefield" when the
 * ability's own cost takes the source away (CR 113.7a): a fetch land, a Treasure. The trigger is
 * matched after the cost is paid, with the source already in the graveyard, so its source filter
 * must read the permanent as it last existed on the battlefield — on both activation paths, the
 * stack one and the mana-ability one. A card activated from the graveyard was never a permanent
 * and must stay unmatched by `onBattlefield()` (Harsh Mentor's ruling).
 *
 * The same holds off the battlefield: "an ability of a card in your graveyard" (Neerdiv, Devious
 * Diver) sees a graveyard card whose own cost exiled it as the graveyard card it was when the
 * ability was activated — and a permanent sacrificed into the graveyard by its own cost was never
 * a card in a graveyard.
 */
class ActivatedSourceLastKnownScenarioTest : FunSpec({

    val landWatcher = card("Land Watcher") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        oracleText = "Whenever a player activates an ability of a land on the battlefield, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.anyPlayer.activatesAbility(of = GameObjectFilter.Land.onBattlefield())
            effect = Effects.GainLife(1)
        }
    }

    val artifactWatcher = card("Artifact Watcher") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        oracleText = "Whenever a player activates an ability of an artifact on the battlefield, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.anyPlayer.activatesAbility(
                of = GameObjectFilter.Artifact.onBattlefield(), includeManaAbilities = true
            )
            effect = Effects.GainLife(1)
        }
    }

    val creatureWatcher = card("Creature Watcher") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        oracleText = "Whenever a player activates an ability of a creature on the battlefield, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.anyPlayer.activatesAbility(of = GameObjectFilter.Creature.onBattlefield())
            effect = Effects.GainLife(1)
        }
    }

    val crackLand = card("Crack Land") {
        typeLine = "Land"
        oracleText = "{T}, Sacrifice this land: Draw a card."
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
            effect = Effects.DrawCards(1)
        }
    }

    // "Sacrifice a creature" — a generic sacrifice cost the source can pay with itself.
    val fodderBeast = card("Fodder Beast") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 1
        oracleText = "Sacrifice a creature: Draw a card."
        activatedAbility {
            cost = Costs.Sacrifice(GameObjectFilter.Creature)
            effect = Effects.DrawCards(1)
        }
    }

    val manaTrinket = card("Mana Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Sacrifice this artifact: Add {C}."
        activatedAbility {
            cost = Costs.SacrificeSelf
            effect = AddColorlessManaEffect(1)
            manaAbility = true
        }
    }

    val graveBeast = card("Grave Beast") {
        manaCost = "{2}{B}"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
        oracleText = "Exile this card from your graveyard: Draw a card."
        activatedAbility {
            cost = Costs.ExileSelf
            effect = Effects.DrawCards(1)
            activateFromZone = Zone.GRAVEYARD
        }
    }

    val graveyardWatcher = card("Graveyard Watcher") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        oracleText = "Whenever a player activates an ability of a card in your graveyard, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.anyPlayer.activatesAbility(
                of = GameObjectFilter.Any.ownedByYou().currentlyIn(Zone.GRAVEYARD), includeManaAbilities = true
            )
            effect = Effects.GainLife(1)
        }
    }

    // A graveyard ability whose cost leaves the card where it is.
    val graveWhisper = card("Grave Whisper") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        oracleText = "{0}: Draw a card. Activate only from your graveyard."
        activatedAbility {
            cost = Costs.Free
            effect = Effects.DrawCards(1)
            activateFromZone = Zone.GRAVEYARD
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(
            landWatcher, artifactWatcher, creatureWatcher, crackLand, fodderBeast, manaTrinket, graveBeast,
            graveyardWatcher, graveWhisper,
        )
            .forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.activate(source: EntityId, name: String, by: EntityId = player1) {
        val abilityId = cardRegistry.requireCard(name).activatedAbilities[0].id
        submitSuccess(ActivateAbility(playerId = by, sourceId = source, abilityId = abilityId))
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 30) { bothPass(); guard++ }
    }

    test("a land sacrificed by its own cost is still a land on the battlefield to the trigger") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player2, "Land Watcher")
        val land = driver.putPermanentOnBattlefield(driver.player1, "Crack Land")

        driver.activate(land, "Crack Land")

        driver.getLifeTotal(driver.player2) shouldBe 21
    }

    test("an artifact sacrificed for its own mana is matched as it last existed") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player2, "Artifact Watcher")
        val trinket = driver.putPermanentOnBattlefield(driver.player1, "Mana Trinket")

        driver.activate(trinket, "Mana Trinket")

        driver.getLifeTotal(driver.player2) shouldBe 21
    }

    test("a creature that pays its own generic sacrifice cost is matched as it last existed") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player2, "Creature Watcher")
        val beast = driver.putPermanentOnBattlefield(driver.player1, "Fodder Beast")

        val abilityId = driver.cardRegistry.requireCard("Fodder Beast").activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = driver.player1, sourceId = beast, abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(beast)),
            )
        )
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 30) { driver.bothPass(); guard++ }

        driver.findPermanent(driver.player1, "Fodder Beast") shouldBe null
        driver.getLifeTotal(driver.player2) shouldBe 21
    }

    test("a creature card activated from the graveyard is not a creature on the battlefield") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player2, "Creature Watcher")
        val beast = driver.putCardInGraveyard(driver.player1, "Grave Beast")

        driver.activate(beast, "Grave Beast")

        driver.getLifeTotal(driver.player2) shouldBe 20
    }

    test("a sacrificed land's last-known types still gate the filter") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player2, "Creature Watcher")
        val land = driver.putPermanentOnBattlefield(driver.player1, "Crack Land")

        driver.activate(land, "Crack Land")

        driver.getLifeTotal(driver.player2) shouldBe 20
    }

    test("a graveyard card its own cost exiles is still a card in your graveyard to the trigger") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Graveyard Watcher")
        val beast = driver.putCardInGraveyard(driver.player1, "Grave Beast")

        driver.activate(beast, "Grave Beast")

        driver.getGraveyardCardNames(driver.player1).contains("Grave Beast") shouldBe false
        driver.getLifeTotal(driver.player1) shouldBe 21
    }

    test("a graveyard ability that leaves the card in place is matched live") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Graveyard Watcher")
        val whisper = driver.putCardInGraveyard(driver.player1, "Grave Whisper")

        driver.activate(whisper, "Grave Whisper")

        driver.getLifeTotal(driver.player1) shouldBe 21
    }

    test("a card exiled from another player's graveyard is not a card in your graveyard") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Graveyard Watcher")
        val beast = driver.putCardInGraveyard(driver.player2, "Grave Beast")
        driver.passPriority(driver.player1)

        driver.activate(beast, "Grave Beast", by = driver.player2)

        driver.getLifeTotal(driver.player1) shouldBe 20
    }

    test("a permanent its own cost sacrificed into the graveyard was never a card in a graveyard") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Graveyard Watcher")
        val land = driver.putPermanentOnBattlefield(driver.player1, "Crack Land")

        driver.activate(land, "Crack Land")

        driver.getGraveyardCardNames(driver.player1).contains("Crack Land") shouldBe true
        driver.getLifeTotal(driver.player1) shouldBe 20
    }
})
