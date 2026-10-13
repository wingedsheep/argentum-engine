package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.MayCastFromGraveyardUsedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `MayCastFromGraveyard(playsLands = true)` — the *play* form of the graveyard grant (Serra Paragon).
 * The card's own scenario test covers the happy paths; these pin the edges of the land half:
 *  - another graveyard land permission (Crucible of Worlds) authorizes the play first, so the
 *    grant's per-turn allowance is not spent and its rider is not attached;
 *  - the cast half never casts a land (CR 305.9), even from a forged `CastSpell` the client sends;
 *  - without `playsLands`, a cast grant whose filter admits a land card (a land creature under a
 *    `Creature` grant) offers no land play.
 */
class GraveyardPlayGrantLandPlayTest : FunSpec({

    // Built inside `card { }`: a TriggeredAbility mints its id from the enclosing card.
    val playBench = card("Play Grant Bench") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = MayCastFromGraveyard(
                filter = GameObjectFilter.Permanent.manaValueAtMost(3),
                duringYourTurnOnly = true,
                oncePerTurn = true,
                playsLands = true,
                grantedAbilityOnEntry = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.GainLife(2),
                    descriptionOverride = "When this permanent is put into a graveyard from the battlefield, " +
                        "you gain 2 life.",
                ),
            )
        }
    }
    val crucible = card("Crucible Bench") {
        manaCost = "{3}"
        typeLine = "Artifact"
        staticAbility { ability = MayPlayLandsFromGraveyard }
    }
    val creatureCastBench = card("Creature Cast Bench") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility { ability = MayCastFromGraveyard(GameObjectFilter.Creature) }
    }
    val arbor = card("Grant Test Arbor") {
        typeLine = "Land Creature — Forest Dryad"
        power = 1
        toughness = 1
    }
    val bear = card("Grant Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(playBench, crucible, creatureCastBench, arbor, bear))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun graveyardActions(driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        driver.legalActions(player).filter {
            it.sourceZone == "GRAVEYARD" &&
                ((it.action as? CastSpell)?.cardId == cardId || (it.action as? PlayLand)?.cardId == cardId)
        }

    test("a land Crucible of Worlds also authorizes is played through Crucible — allowance kept, no rider") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val bench = driver.putPermanentOnBattlefield(player, "Play Grant Bench")
        driver.putPermanentOnBattlefield(player, "Crucible Bench")
        val forest = driver.putCardInGraveyard(player, "Forest")
        val bearCard = driver.putCardInGraveyard(player, "Grant Test Bear")

        driver.submit(PlayLand(player, forest)).outcome shouldBe Outcome.Done

        driver.state.getEntity(bench)!!.has<MayCastFromGraveyardUsedThisTurnComponent>() shouldBe false
        withClue("the grant's rider rides only plays that went through the grant") {
            driver.state.grantedTriggeredAbilities.none { it.entityId == forest } shouldBe true
        }
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 1)
        graveyardActions(driver, player, bearCard) shouldHaveSize 1
    }

    test("a land under a play grant is never offered as a cast, and a forged cast is rejected") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val bench = driver.putPermanentOnBattlefield(player, "Play Grant Bench")
        val forest = driver.putCardInGraveyard(player, "Forest")

        graveyardActions(driver, player, forest).map { it.action::class } shouldBe listOf(PlayLand::class)
        driver.submit(CastSpell(playerId = player, cardId = forest)).outcome shouldNotBe Outcome.Done
        driver.getGraveyard(player).contains(forest) shouldBe true
        driver.state.getEntity(bench)!!.has<MayCastFromGraveyardUsedThisTurnComponent>() shouldBe false
    }

    test("a cast-only grant whose filter admits a land creature neither plays nor casts it") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Creature Cast Bench")
        val arborCard = driver.putCardInGraveyard(player, "Grant Test Arbor")

        graveyardActions(driver, player, arborCard) shouldHaveSize 0
        driver.submit(PlayLand(player, arborCard)).outcome shouldNotBe Outcome.Done
        driver.submit(CastSpell(playerId = player, cardId = arborCard)).outcome shouldNotBe Outcome.Done
    }

    test("the land play counts as the turn's land drop") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Play Grant Bench")
        val forest = driver.putCardInGraveyard(player, "Forest")
        val handForest = driver.putCardInHand(player, "Forest")

        driver.submit(PlayLand(player, forest)).outcome shouldBe Outcome.Done
        driver.legalActions(player).none { (it.action as? PlayLand)?.cardId == handForest } shouldBe true
    }
})
