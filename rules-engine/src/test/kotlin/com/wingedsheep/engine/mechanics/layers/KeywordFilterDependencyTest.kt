package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * CR 613.8a: a Layer-6 effect whose affected set reads keywords ("creatures with flying have
 * vigilance") depends on the same-layer effects that grant or remove those keywords, so it applies
 * after them whatever the timestamps. "Can't attack"/"can't block" restrictions apply after all
 * other continuous effects (CR 613.11), and Layer 7 reads the keywords Layer 6 settled.
 */
class KeywordFilterDependencyTest : FunSpec({
    val moat = card("Keyword Dependency Moat") {
        manaCost = "{0}"; typeLine = "Enchantment"
        staticAbility { ability = CantAttack(GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING))) }
    }
    val skyAnthem = card("Keyword Dependency Sky Anthem") {
        manaCost = "{0}"; typeLine = "Enchantment"
        staticAbility { ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING))) }
    }
    val groundedSky = card("Keyword Dependency Grounded Sky") {
        manaCost = "{0}"; typeLine = "Enchantment"
        staticAbility { ability = CantBlock(GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING))) }
    }
    val skyWatch = card("Keyword Dependency Sky Watch") {
        manaCost = "{0}"; typeLine = "Enchantment"
        staticAbility { ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING))) }
    }
    val flyer = card("Keyword Dependency Flyer") {
        manaCost = "{0}"; typeLine = "Creature — Bird"; power = 1; toughness = 1
        keywords(Keyword.FLYING)
    }
    val jump = card("Keyword Dependency Jump") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.GrantKeyword(Keyword.FLYING, t) }
    }
    val ground = card("Keyword Dependency Ground") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.RemoveKeyword(Keyword.FLYING, t) }
    }
    val mute = card("Keyword Dependency Mute") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.RemoveAllAbilities(t) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(moat, skyAnthem, skyWatch, groundedSky, flyer, jump, ground, mute))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(name: String, target: EntityId) {
        val me = activePlayer!!
        castSpell(me, putCardInHand(me, name), listOf(target)).error shouldBe null
        bothPass()
    }

    test("a flying grant newer than the restriction frees its creature to attack") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, moat.name)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.state.projectedState.cantAttack(bears) shouldBe true

        d.cast(jump.name, bears)
        d.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
        d.state.projectedState.cantAttack(bears) shouldBe false
    }

    test("a restriction newer than the flying grant agrees") {
        val d = driver()
        val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.cast(jump.name, bears)
        d.putPermanentOnBattlefield(me, moat.name)
        d.state.projectedState.cantAttack(bears) shouldBe false
    }

    test("losing flying after the restriction entered brings the creature under it") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, moat.name)
        val bird = d.putCreatureOnBattlefield(me, flyer.name)
        d.state.projectedState.cantAttack(bird) shouldBe false

        d.cast(ground.name, bird)
        d.state.projectedState.cantAttack(bird) shouldBe true
    }

    test("losing all abilities after the restriction entered brings the creature under it") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, moat.name)
        val bird = d.putCreatureOnBattlefield(me, flyer.name)

        d.cast(mute.name, bird)
        d.state.projectedState.cantAttack(bird) shouldBe true
    }

    test("a keyword-filtered Layer 6 grant applies after a newer grant of the keyword it reads") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, skyWatch.name)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false

        d.cast(jump.name, bears)
        d.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true

        val bird = d.putCreatureOnBattlefield(me, flyer.name)
        d.state.projectedState.hasKeyword(bird, Keyword.VIGILANCE) shouldBe true
        d.cast(ground.name, bird)
        d.state.projectedState.hasKeyword(bird, Keyword.VIGILANCE) shouldBe false
    }

    test("a keyword-filtered can't-block reads a newer grant") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, groundedSky.name)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.state.projectedState.cantBlock(bears) shouldBe false

        d.cast(jump.name, bears)
        d.state.projectedState.cantBlock(bears) shouldBe true
    }

    test("a keyword-filtered Layer 7 bonus reads keywords granted in Layer 6") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, skyAnthem.name)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.state.projectedState.getPower(bears) shouldBe 2

        d.cast(jump.name, bears)
        d.state.projectedState.getPower(bears) shouldBe 3

        val bird = d.putCreatureOnBattlefield(me, flyer.name)
        d.state.projectedState.getPower(bird) shouldBe 2
        d.cast(ground.name, bird)
        d.state.projectedState.getPower(bird) shouldBe 1
    }
})
