package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The player-scoped land-play statics — [MayPlayLandsFromGraveyard] ("You may play lands from your
 * graveyard") and [GrantAdditionalLandDrop] ("You may play an additional land on each of your
 * turns", CR 305.2) — are read off the holder's *projected* static abilities, so a permanent has
 * them however it got them: printed, gained from a resolved effect (Nog, Ambitious Cadet), or
 * granted by a [GrantStaticAbility]. The "you" is the holder's controller, and a holder that loses
 * all its abilities (CR 613.1f) stops granting.
 */
class GainedLandPlayStaticsTest : FunSpec({

    val cadet = card("Land Grant Cadet") {
        manaCost = "{1}"
        typeLine = "Creature — Ferengi"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Free
            effect = Effects.GrantStaticAbility(MayPlayLandsFromGraveyard, EffectTarget.Self, Duration.Permanent) then
                Effects.GrantStaticAbility(GrantAdditionalLandDrop(), EffectTarget.Self, Duration.Permanent)
            description = "This creature gains the two land-play statics."
        }
    }
    val banner = card("Land Grant Banner") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = GrantStaticAbility(GrantAdditionalLandDrop(), Filters.Group.creaturesYouControl)
        }
    }
    val humility = card("Land Grant Humility") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility { ability = LoseAllAbilities(Filters.Group.allCreatures) }
    }
    val gatedCrucible = card("Gated Crucible") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = ConditionalStaticAbility(
                MayPlayLandsFromGraveyard,
                Conditions.YouControl(GameObjectFilter.Creature),
            )
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(cadet, banner, humility, gatedCrucible))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.gainGrants(cadetId: EntityId) {
        submit(ActivateAbility(activePlayer!!, cadetId, cadet.activatedAbilities.first().id)).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.graveyardLandPlays(player: EntityId, cardId: EntityId) =
        legalActions(player).filter { it.sourceZone == "GRAVEYARD" && (it.action as? PlayLand)?.cardId == cardId }

    test("statics gained from a resolved effect grant their holder's controller both land permissions") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val cadetId = driver.putCreatureOnBattlefield(player, "Land Grant Cadet")
        val handForest = driver.putCardInHand(player, "Forest")
        val graveyardForest = driver.putCardInGraveyard(player, "Forest")
        val secondHandForest = driver.putCardInHand(player, "Forest")

        driver.graveyardLandPlays(player, graveyardForest) shouldHaveSize 0
        driver.gainGrants(cadetId)

        driver.graveyardLandPlays(player, graveyardForest) shouldHaveSize 1
        driver.submit(PlayLand(player, handForest)).outcome shouldBe Outcome.Done
        withClue("the gained additional land drop allows a second play, from the graveyard") {
            driver.submit(PlayLand(player, graveyardForest)).outcome shouldBe Outcome.Done
        }
        withClue("one extra drop, not unlimited") {
            driver.submit(PlayLand(player, secondHandForest)).outcome shouldNotBe Outcome.Done
        }
    }

    test("the gained permissions belong to the holder's controller, not the opponent") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val cadetId = driver.putCreatureOnBattlefield(opponent, "Land Grant Cadet")
        val graveyardForest = driver.putCardInGraveyard(player, "Forest")
        driver.replaceState(driver.state.copy(
            grantedStaticAbilities = listOf(
                com.wingedsheep.engine.event.GrantedStaticAbility(
                    entityId = cadetId,
                    ability = MayPlayLandsFromGraveyard,
                    duration = Duration.Permanent,
                    sourceId = cadetId,
                    controllerId = opponent,
                    layerTimestamp = driver.state.timestamp,
                )
            )
        ))

        driver.graveyardLandPlays(player, graveyardForest) shouldHaveSize 0
        driver.submit(PlayLand(player, graveyardForest)).outcome shouldNotBe Outcome.Done
    }

    test("a holder that loses all its abilities stops granting") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val cadetId = driver.putCreatureOnBattlefield(player, "Land Grant Cadet")
        val graveyardForest = driver.putCardInGraveyard(player, "Forest")
        driver.gainGrants(cadetId)
        // Cast, not placed: the lose-all-abilities effect needs a timestamp later than the gain
        // (CR 613.7a — an ability gained *after* it would survive).
        val humilityCard = driver.putCardInHand(player, "Land Grant Humility")
        driver.giveColorlessMana(player, 2)
        driver.castSpell(player, humilityCard).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(player, "Land Grant Humility") shouldNotBe null

        driver.graveyardLandPlays(player, graveyardForest) shouldHaveSize 0
        driver.submit(PlayLand(player, graveyardForest)).outcome shouldNotBe Outcome.Done
    }

    test("a GrantStaticAbility of an additional land drop counts once per holder") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Land Grant Banner")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val forests = List(4) { driver.putCardInHand(player, "Forest") }

        withClue("the turn's drop plus one per creature holding the grant") {
            forests.take(3).forEach { driver.submit(PlayLand(player, it)).outcome shouldBe Outcome.Done }
        }
        driver.submit(PlayLand(player, forests[3])).outcome shouldNotBe Outcome.Done
    }

    test("a printed permission behind a condition applies only while the condition holds") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Gated Crucible")
        val graveyardForest = driver.putCardInGraveyard(player, "Forest")

        driver.graveyardLandPlays(player, graveyardForest) shouldHaveSize 0
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.graveyardLandPlays(player, graveyardForest) shouldHaveSize 1
        driver.submit(PlayLand(player, graveyardForest)).outcome shouldBe Outcome.Done
    }
})
