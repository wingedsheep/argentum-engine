package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Turning a face-down permanent face up while a "loses all abilities" effect applies to it.
 *
 * The morph and disguise special actions need the permanent to have a morph (disguise) cost "if it
 * were face up" (CR 702.37e, 702.168d), and a lose-all-abilities effect on the face-down permanent
 * takes that away (Dance of the Skywise ruling, 2015-02-25). The manifest and cloak procedures ask
 * only for a creature card with a mana cost (CR 701.40b, 701.58b), so a manifested card keeps that
 * route. An effect that turns it face up isn't the special action and isn't blocked.
 */
class FaceDownLostAbilitiesTurnUpTest : FunSpec({
    val hide = card("Lost-Abilities Hider") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val chosen = chooseAnyNumber(from = hand, prompt = "Choose cards")
                move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You), faceDown = FaceDownMode.HIDDEN)
            }
        }
    }
    val manifest = card("Lost-Abilities Manifester") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(1), player = Player.You))
                move(top, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You), faceDown = FaceDownMode.MANIFEST)
            }
        }
    }
    val strip = card("Lost-Abilities Stripper") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(t, Duration.EndOfTurn)
        }
    }
    val flipper = card("Lost-Abilities Flipper") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Creature.faceDown()))
            effect = Effects.TurnFaceUp(t)
        }
    }
    val morphling = card("Lost-Abilities Morphling") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Elf"
        power = 1; toughness = 3
        morph = "{G}"
    }
    val disguised = card("Lost-Abilities Disguiser") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Elf"
        power = 3; toughness = 1
        disguise = "{G}"
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(hide, manifest, strip, flipper, morphling, disguised))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.hideFaceDown(name: String): EntityId {
        val c = putCardInHand(player1, name)
        val spell = putCardInHand(player1, hide.name)
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, spell).error shouldBe null
        bothPass().error shouldBe null
        submitCardSelection(player1, listOf(c)).error shouldBe null
        state.getEntity(c)!!.has<FaceDownComponent>() shouldBe true
        return c
    }

    fun GameTestDriver.manifestTop(name: String): EntityId {
        val c = putCardOnTopOfLibrary(player1, name)
        val spell = putCardInHand(player1, manifest.name)
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, spell).error shouldBe null
        bothPass().error shouldBe null
        state.getEntity(c)!!.has<FaceDownComponent>() shouldBe true
        return c
    }

    fun GameTestDriver.castOn(spellName: String, target: EntityId) {
        val spell = putCardInHand(player1, spellName)
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, spell, listOf(target)).error shouldBe null
        bothPass().error shouldBe null
    }

    fun GameTestDriver.turnUpActions(id: EntityId) =
        legalActions(player1).filter { (it.action as? TurnFaceUp)?.sourceId == id }

    test("a face-down morph that lost all abilities can't be turned face up for its morph cost") {
        val d = driver()
        val elf = d.hideFaceDown(morphling.name)
        d.giveMana(d.player1, Color.GREEN, 1)
        d.turnUpActions(elf).size shouldBe 1

        d.castOn(strip.name, elf)
        d.state.projectedState.hasLostAllAbilities(elf) shouldBe true
        d.turnUpActions(elf).shouldBeEmpty()

        d.submit(TurnFaceUp(d.player1, elf)).error shouldNotBe null
        d.state.getEntity(elf)!!.has<FaceDownComponent>() shouldBe true
    }

    test("a face-down disguise card that lost all abilities can't be turned face up for its disguise cost") {
        val d = driver()
        val elf = d.hideFaceDown(disguised.name)
        d.giveMana(d.player1, Color.GREEN, 1)
        d.turnUpActions(elf).size shouldBe 1

        d.castOn(strip.name, elf)
        d.turnUpActions(elf).shouldBeEmpty()
        d.submit(TurnFaceUp(d.player1, elf)).error shouldNotBe null
    }

    test("once the effect ends, the morph route is open again") {
        val d = driver()
        val elf = d.hideFaceDown(morphling.name)
        d.castOn(strip.name, elf)
        d.turnUpActions(elf).shouldBeEmpty()

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = d.player2)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = d.player1)
        d.state.projectedState.hasLostAllAbilities(elf) shouldBe false
        d.giveMana(d.player1, Color.GREEN, 1)
        d.turnUpActions(elf).size shouldBe 1
        d.submit(TurnFaceUp(d.player1, elf)).error shouldBe null
        d.state.getEntity(elf)!!.has<FaceDownComponent>() shouldBe false
    }

    test("a manifested morph card that lost all abilities keeps only the manifest route") {
        val d = driver()
        val elf = d.manifestTop(morphling.name)
        val procedures = d.state.getEntity(elf)!!.get<MorphDataComponent>()!!.procedures
        procedures.map { it.mechanic }.toSet() shouldBe setOf(FaceDownMode.MANIFEST, FaceDownMode.MORPH)
        d.giveMana(d.player1, Color.GREEN, 2)
        d.turnUpActions(elf).size shouldBe 2

        d.castOn(strip.name, elf)
        val offered = d.turnUpActions(elf)
        offered.size shouldBe 1
        val manifestIndex = procedures.indexOfFirst { it.mechanic == FaceDownMode.MANIFEST }
        (offered.single().action as TurnFaceUp).procedureIndex shouldBe manifestIndex
        // One route left, so the button no longer names the mechanic.
        offered.single().description shouldBe "Turn face-up ({1}{G})"

        val morphIndex = procedures.indexOfFirst { it.mechanic == FaceDownMode.MORPH }
        d.submit(TurnFaceUp(d.player1, elf, procedureIndex = morphIndex)).error shouldNotBe null
        d.submit(TurnFaceUp(d.player1, elf, procedureIndex = manifestIndex)).error shouldBe null
        d.state.getEntity(elf)!!.has<FaceDownComponent>() shouldBe false
    }

    test("an effect can still turn a face-down morph that lost all abilities face up") {
        val d = driver()
        val elf = d.hideFaceDown(morphling.name)
        d.castOn(strip.name, elf)
        d.castOn(flipper.name, elf)
        d.state.getEntity(elf)!!.has<FaceDownComponent>() shouldBe false
    }
})
