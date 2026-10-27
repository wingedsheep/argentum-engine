package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.SkipUntapComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SkipUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `SkipUntapEffect`'s `permanents` filter — "[permanents] that player controls don't untap during
 * their next untap step". The filter is read at that untap step, against what the player controls
 * then (Icebreaker Kraken ruling, 2021-02-05), and two such effects each hold their own permanents.
 */
class SkipUntapFilterTest : FunSpec({
    val freezeArtifactsAndCreatures = card("Freeze Artifacts And Creatures") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(Targets.Player); effect = Effects.SkipUntap(t, GameObjectFilter.Artifact or GameObjectFilter.Creature) }
    }
    val freezeLands = card("Freeze Lands") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(Targets.Player); effect = Effects.SkipUntap(t, GameObjectFilter.Land) }
    }
    val freezeCreatures = card("Freeze Creatures") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(Targets.Player); effect = Effects.SkipUntap(t, GameObjectFilter.Creature) }
    }
    val steal = card("Steal For Untap") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.GainControl(t) }
    }
    val trinket = card("Untap Test Trinket") { typeLine = "Artifact" }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(freezeArtifactsAndCreatures, freezeLands, freezeCreatures, steal, trinket))
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun cast(d: GameTestDriver, name: String, target: EntityId) {
        val me = d.activePlayer!!
        d.castSpell(me, d.putCardInHand(me, name), listOf(target)).error shouldBe null
        d.bothPass()
    }
    fun untap(d: GameTestDriver) = d.services.turnManager.beginningPhaseManager.performUntapStep(d.state)
    fun GameTestDriver.tapped(id: EntityId) = state.getEntity(id)!!.has<TappedComponent>()

    test("artifacts and creatures stay tapped, lands untap, and the marker is consumed") {
        val d = driver(); val me = d.activePlayer!!
        val artifact = d.putPermanentOnBattlefield(me, trinket.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        listOf(artifact, bear, forest).forEach(d::tapPermanent)
        cast(d, freezeArtifactsAndCreatures.name, me)

        d.replaceState(untap(d).state)
        d.tapped(artifact) shouldBe true
        d.tapped(bear) shouldBe true
        d.tapped(forest) shouldBe false
        d.state.getEntity(me)!!.has<SkipUntapComponent>() shouldBe false

        d.replaceState(untap(d).state)
        d.tapped(artifact) shouldBe false
        d.tapped(bear) shouldBe false
    }

    test("a permanent gained after the effect resolved doesn't untap either") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        val theirBear = d.putCreatureOnBattlefield(other, "Grizzly Bears").also(d::tapPermanent)
        cast(d, freezeArtifactsAndCreatures.name, me)
        cast(d, steal.name, theirBear)
        d.state.projectedState.getController(theirBear) shouldBe me

        d.replaceState(untap(d).state)
        d.tapped(theirBear) shouldBe true
    }

    test("two effects each hold their own permanents rather than the second replacing the first") {
        val d = driver(); val me = d.activePlayer!!
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        val artifact = d.putPermanentOnBattlefield(me, trinket.name)
        listOf(bear, forest, artifact).forEach(d::tapPermanent)
        cast(d, freezeLands.name, me)
        cast(d, freezeCreatures.name, me)

        d.replaceState(untap(d).state)
        d.tapped(bear) shouldBe true
        d.tapped(forest) shouldBe true
        d.tapped(artifact) shouldBe false
    }

    test("the description reads the filter the way Oracle prints it") {
        SkipUntapEffect(EffectTarget.PlayerRef(Player.TargetOpponent)).description shouldBe
            "Creatures and lands target opponent controls don't untap during their next untap step"
        SkipUntapEffect(EffectTarget.PlayerRef(Player.TargetOpponent), GameObjectFilter.Artifact or GameObjectFilter.Creature)
            .description shouldBe "Artifacts and creatures target opponent controls don't untap during their next untap step"
    }
})
