package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `MoveCollectionEffect.attachTo` for Equipment and Fortification cards, and the
 * `CardPredicate.CouldAttachTo` filter that keeps the unattachable ones behind.
 *
 * Rules pinned here:
 *  - An Equipment put onto the battlefield attached to a creature enters attached (Danitha,
 *    Benalia's Hope ruling 2022-09-09) — no separate attach step.
 *  - One that can't legally equip that creature enters **unattached** rather than staying in its
 *    zone (CR 301.5e; contrast the Aura rule, CR 303.4i): its own "can be attached only to …"
 *    restriction, an Equipment that is also a creature (CR 301.5c), a Fortification,
 *    which can be attached only to a land (CR 301.6).
 *  - With a `couldAttachTo(host)` filter, those cards don't move at all (Mantle of the Ancients
 *    ruling 2021-07-23) while the attachable ones still enter attached.
 */
class EnterAttachedEquipmentTest : ScenarioTestBase() {

    private fun returner(name: String, keepUnattachable: Boolean) = card(name) {
        manaCost = "{1}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.Pipeline {
                val gear = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Artifact))
                move(
                    gear,
                    CardDestination.ToZone(Zone.BATTLEFIELD),
                    attachTo = EffectTarget.Self,
                    filter = if (keepUnattachable) GameObjectFilter.Any.couldAttachTo(EffectTarget.Self) else null
                )
            }
        }
    }

    private val walkingBlade = card("Test Walking Blade") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Equipment Construct"
        power = 1
        toughness = 1
    }

    private val keep = card("Test Keep") {
        manaCost = "{2}"
        typeLine = "Artifact — Fortification"
    }

    init {
        cardRegistry.register(returner("Test Gear Returner", keepUnattachable = false))
        cardRegistry.register(returner("Test Careful Returner", keepUnattachable = true))
        cardRegistry.register(walkingBlade)
        cardRegistry.register(keep)
    }

    private fun board(returner: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, returner)
        .withCardInGraveyard(1, "Bonesplitter")
        .withCardInGraveyard(1, "Konda's Banner")
        .withCardInGraveyard(1, "Test Walking Blade")
        .withCardInGraveyard(1, "Test Keep")
        .withLandsOnBattlefield(1, "Plains", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attachedTo(name: String) =
        findPermanent(name)?.let { state.getEntity(it)?.get<AttachedToComponent>()?.targetId }

    init {
        test("an Equipment enters attached; ones that can't equip the host enter unattached") {
            val game = board("Test Gear Returner")
            game.castSpell(1, "Test Gear Returner").error shouldBe null
            game.resolveStack()

            val host = game.findPermanent("Test Gear Returner")!!
            game.attachedTo("Bonesplitter") shouldBe host
            game.state.projectedState.getPower(host) shouldBe 3
            withClue("Konda's Banner can be attached only to a legendary creature") {
                game.isOnBattlefield("Konda's Banner") shouldBe true
                game.attachedTo("Konda's Banner") shouldBe null
            }
            withClue("an Equipment that is also a creature can't equip (CR 301.5c)") {
                game.isOnBattlefield("Test Walking Blade") shouldBe true
                game.attachedTo("Test Walking Blade") shouldBe null
            }
            withClue("a Fortification can be attached only to a land (CR 301.6)") {
                game.isOnBattlefield("Test Keep") shouldBe true
                game.attachedTo("Test Keep") shouldBe null
            }
        }

        test("a couldAttachTo filter keeps the unattachable cards in their zone") {
            val game = board("Test Careful Returner")
            game.castSpell(1, "Test Careful Returner").error shouldBe null
            game.resolveStack()

            game.attachedTo("Bonesplitter") shouldBe game.findPermanent("Test Careful Returner")
            game.isInGraveyard(1, "Konda's Banner") shouldBe true
            game.isInGraveyard(1, "Test Walking Blade") shouldBe true
            game.isInGraveyard(1, "Test Keep") shouldBe true
        }
    }
}
