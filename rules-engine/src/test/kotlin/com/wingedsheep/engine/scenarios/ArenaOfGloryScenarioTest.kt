package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ArenaOfGlory
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Arena of Glory (MH3) — proves the two genuinely new pieces this land needed: the Exert cost
 * (CR 701.43a, [com.wingedsheep.sdk.scripting.AbilityCost.Exert] — a wholly new engine primitive,
 * no prior user) and the checkland-style conditional tapped entry, plus that the haste rider
 * ([com.wingedsheep.sdk.scripting.effects.ManaSpellRider.GrantsKeywordWhenSpent], already proven
 * by Carnelian Orb of Dragonkind) correctly distinguishes the exert ability's {R}{R} from the
 * land's own plain {R} — only the former grants haste.
 */
class ArenaOfGloryScenarioTest : FunSpec({

    val plainAbilityId = ArenaOfGlory.activatedAbilities[0].id
    val exertAbilityId = ArenaOfGlory.activatedAbilities[1].id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        return d
    }

    /** Walk the turn cycle until [player] is active again and has passed their untap step. */
    fun advanceToOwnNextTurn(d: GameTestDriver, player: EntityId) {
        d.passPriorityUntil(Step.END)
        d.bothPass()
        while (d.activePlayer != player) {
            d.passPriorityUntil(Step.END)
            d.bothPass()
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /**
     * Untap [permanent] behind the engine's back, standing in for any untapper. Needed because
     * Arena of Glory's exert ability also costs {T}, so the only way to reach the CR 701.43b corners
     * ("a permanent can be exerted even if it's not tapped or has already been exerted") is to get
     * the land untapped again without waiting for an untap step.
     */
    fun forceUntap(d: GameTestDriver, permanent: EntityId) {
        d.replaceState(d.state.updateEntity(permanent) { it.without<TappedComponent>() })
    }

    test("enters tapped without a Mountain, untapped with one") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Enters-tapped replacement effects only apply through the real PlayLand pipeline —
        // putLandOnBattlefield bypasses it entirely (mirrors TdmCheckLandsScenarioTest).
        val noMountain = d.putCardInHand(active, "Arena of Glory")
        d.playLand(active, noMountain).isSuccess shouldBe true
        d.state.getEntity(noMountain)?.has<TappedComponent>() shouldBe true
    }

    test("enters untapped when a Mountain is already in play") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putLandOnBattlefield(active, "Mountain")
        val withMountain = d.putCardInHand(active, "Arena of Glory")
        d.playLand(active, withMountain).isSuccess shouldBe true
        d.state.getEntity(withMountain)?.has<TappedComponent>() shouldBe false
    }

    test("exerting doesn't untap next untap step, but untaps normally the turn after") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val arena = d.putLandOnBattlefield(active, "Arena of Glory")
        d.giveMana(active, Color.RED, 1)

        d.submitSuccess(
            ActivateAbility(playerId = active, sourceId = arena, abilityId = exertAbilityId)
        )
        d.state.getEntity(arena)?.has<TappedComponent>() shouldBe true
        d.state.getEntity(arena)?.has<ExertedComponent>() shouldBe true

        // Through the rest of this turn, all of the opponent's turn, and into the exerting
        // player's own next untap step: the exert skips it, and the marker expires there.
        advanceToOwnNextTurn(d, active)
        d.state.getEntity(arena)?.has<TappedComponent>() shouldBe true
        d.state.getEntity(arena)?.has<ExertedComponent>() shouldBe false

        // A later turn (no re-exert) untaps it normally.
        advanceToOwnNextTurn(d, active)
        d.state.getEntity(arena)?.has<TappedComponent>() shouldBe false
    }

    test("exerting an untapped, already-exerted permanent is legal and doesn't stack (CR 701.43b)") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val arena = d.putLandOnBattlefield(active, "Arena of Glory")
        d.giveMana(active, Color.RED, 1)
        d.submitSuccess(ActivateAbility(playerId = active, sourceId = arena, abilityId = exertAbilityId))
        d.state.getEntity(arena)?.has<ExertedComponent>() shouldBe true

        // Untapped and already exerted — CR 701.43b says both are fine, so the cost is payable and
        // the activation succeeds a second time this turn.
        forceUntap(d, arena)
        d.giveMana(active, Color.RED, 1)
        d.submitSuccess(ActivateAbility(playerId = active, sourceId = arena, abilityId = exertAbilityId))
        d.state.getEntity(arena)?.has<ExertedComponent>() shouldBe true

        // "If you exert a permanent more than once before your next untap step, each effect causing
        // it not to untap expires during the same untap step" — two exerts cost exactly one untap
        // step, not two. After the first one it's still tapped, with the marker gone…
        advanceToOwnNextTurn(d, active)
        d.state.getEntity(arena)?.has<ExertedComponent>() shouldBe false
        d.state.getEntity(arena)?.has<TappedComponent>() shouldBe true

        // …and the next untap step untaps it normally.
        advanceToOwnNextTurn(d, active)
        d.state.getEntity(arena)?.has<TappedComponent>() shouldBe false
    }

    test("an exerted permanent that is already untapped still loses the marker at the untap step") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val arena = d.putLandOnBattlefield(active, "Arena of Glory")
        d.giveMana(active, Color.RED, 1)
        d.submitSuccess(ActivateAbility(playerId = active, sourceId = arena, abilityId = exertAbilityId))
        forceUntap(d, arena)

        // 2024-06-07 ruling: "If an exerted permanent is already untapped during your next untap
        // step … exert's effect … expires without having done anything." The marker is not a stun
        // counter — it doesn't wait around for an untap to eat.
        advanceToOwnNextTurn(d, active)
        d.state.getEntity(arena)?.has<ExertedComponent>() shouldBe false
        d.state.getEntity(arena)?.has<TappedComponent>() shouldBe false
    }

    test("mana from the exert ability grants haste to a creature spell; the plain ability's mana does not") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val arena = d.putLandOnBattlefield(active, "Arena of Glory")

        // Plain {T}: Add {R} — spent on Grizzly Bears — should NOT grant haste.
        //
        // The pool is exactly {R}{G} and Grizzly Bears costs {1}{G}: the green pays the {G} pip and
        // the plain ability's red is the *only* mana left to pay the {1}, so it is necessarily
        // spent on the spell. Handing over a colourless as well would let it pay the generic half
        // and leave the red sitting untouched in the pool, at which point this assertion would
        // still pass even if the plain ability did carry a haste rider.
        d.submitSuccess(ActivateAbility(playerId = active, sourceId = arena, abilityId = plainAbilityId))
        d.giveMana(active, Color.GREEN, 1)
        val bearsId = d.putCardInHand(active, "Grizzly Bears")
        d.castSpell(active, bearsId)
        d.bothPass()
        val bears = d.findPermanent(active, "Grizzly Bears")!!
        d.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
    }

    test("mana from the exert ability grants haste") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val arena = d.putLandOnBattlefield(active, "Arena of Glory")
        d.giveMana(active, Color.RED, 1) // pays the {R} part of the exert cost

        d.submitSuccess(ActivateAbility(playerId = active, sourceId = arena, abilityId = exertAbilityId))
        // The activation produces {R}{R} into the pool; give the extra generic mana Grizzly
        // Bears needs beyond the {G} pip, which the exert-tagged red also covers as generic.
        d.giveMana(active, Color.GREEN, 1)
        val bearsId = d.putCardInHand(active, "Grizzly Bears")
        d.castSpell(active, bearsId)
        d.bothPass()
        val bears = d.findPermanent(active, "Grizzly Bears")!!
        d.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
    }
})
