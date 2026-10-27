package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersUntapped
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Durational grants of the global enter-tapped / enter-untapped replacements
 * (`Effects.GrantReplacementEffect(PermanentsEnterTapped(…) | EntersUntapped(…), Self, EndOfTurn)`)
 * and the [DynamicAmount][com.wingedsheep.sdk.scripting.values.DynamicAmount] count of
 * `PlayAdditionalLandsEffect` — the two halves of Nahiri's Lithoforming's "You may play X additional
 * lands this turn. Lands you control enter tapped this turn."
 *
 * A grant from a resolved sorcery is anchored to its caster: "you" is the caster, it outlives the
 * spell, and it lapses in the cleanup step. Every entry path consults the same kernel
 * (`EnterTappedReplacements` / `EnterUntappedReplacements`), so a land play and a cast permanent
 * both see it.
 */
class GrantedEnterTappedReplacementTest : FunSpec({

    val landsEnter = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Land.youControl(),
        to = Zone.BATTLEFIELD
    )

    val TapTheLands = card("Tap the Lands") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.GrantReplacementEffect(
                replacement = PermanentsEnterTapped(appliesTo = landsEnter),
                target = EffectTarget.Self,
                duration = Duration.EndOfTurn
            )
        }
    }

    val UntapTheLands = card("Untap the Lands") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.GrantReplacementEffect(
                replacement = EntersUntapped(appliesTo = landsEnter),
                target = EffectTarget.Self,
                duration = Duration.EndOfTurn
            )
        }
    }

    val VariableBloom = card("Variable Bloom") {
        manaCost = "{X}{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.PlayAdditionalLands(DynamicAmounts.xValue())
        }
    }

    val UnearthLand = card("Unearth Land") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            val land = target(TargetFilter(GameObjectFilter.Land, zone = Zone.GRAVEYARD))
            effect = Effects.PutOntoBattlefieldFromGraveyard(land)
        }
    }

    val SlowLand = card("Slow Land") {
        typeLine = "Land"
        replacementEffect(EntersTapped())
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TapTheLands)
        driver.registerCard(UntapTheLands)
        driver.registerCard(VariableBloom)
        driver.registerCard(SlowLand)
        driver.registerCard(UnearthLand)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30, "Grizzly Bears" to 10), skipMulligans = true)
        return driver
    }

    fun GameTestDriver.castGrant(player: com.wingedsheep.sdk.model.EntityId, name: String) {
        giveMana(player, Color.GREEN, 1)
        castSpell(player, putCardInHand(player, name)).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("a granted 'lands you control enter tapped' taps a played land and outlives the sorcery") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.castGrant(player, "Tap the Lands")
        driver.state.getGraveyard(player).any { driver.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Tap the Lands" } shouldBe true

        val forest = driver.putCardInHand(player, "Forest")
        driver.playLand(player, forest).outcome shouldBe Outcome.Done
        driver.isTapped(forest) shouldBe true
    }

    test("a land put onto the battlefield by an effect enters tapped; an opponent's land doesn't") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.castGrant(player, "Tap the Lands")
        val mine = driver.putCardInGraveyard(player, "Forest")
        val theirs = driver.putCardInGraveyard(opponent, "Forest")
        listOf(mine to player, theirs to opponent).forEach { (land, owner) ->
            driver.giveMana(player, Color.GREEN, 1)
            driver.castSpellWithTargets(
                player, driver.putCardInHand(player, "Unearth Land"),
                listOf(ChosenTarget.Card(land, owner, Zone.GRAVEYARD))
            ).outcome shouldBe Outcome.Done
            driver.bothPass()
            driver.state.getBattlefield().contains(land) shouldBe true
        }

        driver.isTapped(mine) shouldBe true
        driver.isTapped(theirs) shouldBe false
    }

    test("the grant's filter is respected — a nonland permanent cast this turn enters untapped") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.castGrant(player, "Tap the Lands")
        driver.giveMana(player, Color.GREEN, 2)
        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.castSpell(player, bears)
        driver.bothPass()

        driver.state.getBattlefield().contains(bears) shouldBe true
        driver.isTapped(bears) shouldBe false
    }

    test("the grant lapses in the cleanup step — next turn's land enters untapped") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.castGrant(player, "Tap the Lands")
        driver.state.grantedReplacementEffects.size shouldBe 1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, driver.getOpponent(player))
        driver.state.grantedReplacementEffects.size shouldBe 0
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, player)
        val forest = driver.putCardInHand(player, "Forest")
        driver.playLand(player, forest).outcome shouldBe Outcome.Done
        driver.isTapped(forest) shouldBe false
    }

    test("a granted 'lands you control enter untapped' overrides a land's own enters-tapped") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val before = driver.putCardInHand(player, "Slow Land")
        driver.castGrant(player, "Untap the Lands")
        driver.playLand(player, before).outcome shouldBe Outcome.Done
        driver.isTapped(before) shouldBe false
    }

    test("PlayAdditionalLands reads its DynamicAmount count as it resolves") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val start = driver.state.getEntity(player)!!.get<LandDropsComponent>()!!.remaining

        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 2)
        driver.castXSpell(player, driver.putCardInHand(player, "Variable Bloom"), xValue = 2)
        driver.bothPass()

        driver.state.getEntity(player)!!.get<LandDropsComponent>()!!.remaining shouldBe start + 2
        repeat(start + 2) {
            driver.playLand(player, driver.putCardInHand(player, "Forest")).outcome shouldBe Outcome.Done
        }
        driver.submitExpectFailure(PlayLand(player, driver.putCardInHand(player, "Forest"))).outcome shouldNotBe Outcome.Done
    }

    test("PlayAdditionalLands with X = 0 grants nothing") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val start = driver.state.getEntity(player)!!.get<LandDropsComponent>()!!.remaining

        driver.giveMana(player, Color.GREEN, 1)
        driver.castXSpell(player, driver.putCardInHand(player, "Variable Bloom"), xValue = 0)
        driver.bothPass()

        driver.state.getEntity(player)!!.get<LandDropsComponent>()!!.remaining shouldBe start
    }
})
