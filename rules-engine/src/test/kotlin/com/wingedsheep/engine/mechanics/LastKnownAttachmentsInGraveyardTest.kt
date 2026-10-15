package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `CardSource.AttachedTo(host, filter, zone = GRAVEYARD)` — the last-known attachments of a host
 * that has left the battlefield, gathered from where they followed it: "return the Aura cards that
 * were attached to it from your graveyard" (Cass, Hand of Vengeance).
 *
 * Rules pinned here:
 *  - CR 608.2h: the dying creature's attachments are read as last-known information; the Auras
 *    reach the graveyard afterwards, by the unattached-Aura state-based action.
 *  - CR 400.7: an Aura card that changed zones again after that is a new object with no memory of
 *    having been attached, so it is no longer gathered.
 *  - Only cards that were attached are gathered — an unrelated Aura card in the same graveyard isn't.
 *  - The gather reads *your* graveyard: an Aura owned by an opponent went to theirs and is not found.
 */
class LastKnownAttachmentsInGraveyardTest : ScenarioTestBase() {

    /** "Whenever an enchanted creature you control dies, return the Aura cards that were attached to
     *  it from your graveyard to the battlefield attached to target creature." */
    private val auraKeeper = card("Test Aura Keeper") {
        manaCost = "{2}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl().enchanted()).dies()
            val creature = target(TargetFilter.Creature)
            effect = Effects.Pipeline {
                val auras = gather(
                    CardSource.AttachedTo(
                        host = EffectTarget.TriggeringEntity,
                        filter = GameObjectFilter.Any.withSubtype(Subtype.AURA),
                        zone = Zone.GRAVEYARD
                    )
                )
                move(auras, CardDestination.ToZone(Zone.BATTLEFIELD), attachTo = creature)
            }
        }
    }

    init {
        cardRegistry.register(auraKeeper)
    }

    private fun board(ownerOfHolyStrength: Int = 1) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Test Aura Keeper")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardAttachedTo(ownerOfHolyStrength, "Holy Strength", "Grizzly Bears")
        .withCardAttachedTo(1, "Unholy Strength", "Grizzly Bears")
        .withCardInGraveyard(1, "Pacifism")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardInHand(1, "Murder")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attachedTo(name: String) =
        findPermanent(name)?.let { state.getEntity(it)?.get<AttachedToComponent>()?.targetId }

    private fun TestGame.killBearsAndTargetGiant() {
        castSpell(1, "Murder", findPermanent("Grizzly Bears")!!).error shouldBe null
        resolveStack()
        withClue("the keeper's dies trigger asks for its target") {
            (getPendingDecision() is ChooseTargetsDecision) shouldBe true
        }
        selectTargets(listOf(findPermanent("Hill Giant")!!)).error shouldBe null
    }

    init {
        test("Auras that followed the dead host into the graveyard return attached to the target") {
            val game = board()
            game.killBearsAndTargetGiant()
            game.resolveStack()

            val giant = game.findPermanent("Hill Giant")!!
            game.attachedTo("Holy Strength") shouldBe giant
            game.attachedTo("Unholy Strength") shouldBe giant
            withClue("an Aura card that was never attached to the host stays in the graveyard") {
                game.isInGraveyard(1, "Pacifism") shouldBe true
            }
        }

        test("an Aura card that changed zones again is a new object and is not gathered") {
            val game = board()
            game.killBearsAndTargetGiant()

            // With the trigger on the stack, Holy Strength leaves the graveyard and comes back.
            val holy = game.findCardsInGraveyard(1, "Holy Strength").single()
            val exiled = game.zones.moveToZone(game.state, holy, Zone.EXILE).state
            game.state = game.zones.moveToZone(exiled, holy, Zone.GRAVEYARD).state
            game.resolveStack()

            game.attachedTo("Unholy Strength") shouldBe game.findPermanent("Hill Giant")
            game.isInGraveyard(1, "Holy Strength") shouldBe true
        }

        test("an attached Aura owned by an opponent went to their graveyard and is not gathered") {
            val game = board(ownerOfHolyStrength = 2)
            game.killBearsAndTargetGiant()
            game.resolveStack()

            game.attachedTo("Unholy Strength") shouldBe game.findPermanent("Hill Giant")
            game.isInGraveyard(2, "Holy Strength") shouldBe true
        }
    }
}
