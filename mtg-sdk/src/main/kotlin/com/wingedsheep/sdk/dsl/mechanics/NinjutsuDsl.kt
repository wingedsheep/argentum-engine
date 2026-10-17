package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Ninjutsu ability (CR 702.49a):
 *
 *   "[Cost], Reveal this card from your hand, Return an unblocked attacking creature you control to
 *    its owner's hand: Put this card onto the battlefield from your hand tapped and attacking."
 *
 * An ordinary hand-activated ability composed of existing primitives — the card is *put* onto the
 * battlefield, never cast:
 *  - the mana [cost] plus [Costs.ReturnToHand] over an unblocked attacking creature you control —
 *    the cost is only payable once blocked/unblocked status exists (after blockers are declared),
 *    which is the whole of ninjutsu's timing; activation is otherwise instant-speed, so the declare
 *    blockers, combat damage and end of combat steps are all open;
 *  - `activateFromZone = Zone.HAND`; the card stays in hand (revealed by being the ability's
 *    source on the stack, CR 702.49b) until the ability resolves;
 *  - [Effects.Move] of the card from hand ([fromZone] gate — if it left the hand in response,
 *    nothing happens) onto the battlefield tapped and attacking the defender the returned creature
 *    was attacking ([EnteringAttackDefender.RETURNED_ATTACKER], CR 702.49c). A card that isn't a
 *    creature as it enters just enters tapped (CR 506.3a).
 *
 * `keyword = Keyword.NINJUTSU` marks it so "whenever you activate a ninjutsu ability" and "ninjutsu
 * abilities you activate cost {1} less" can find it. The id is keyword-scoped (like
 * [embalmAbility]) so a whole-hand grant — Satoru Umezawa's "each creature card in your hand has
 * ninjutsu {2}{U}{B}", a `GrantActivatedAbility(ninjutsuAbility(...), …, recipientZone =
 * Zone.HAND)` — builds the same ability with no card around it.
 */
fun ninjutsuAbility(cost: ManaCost): ActivatedAbility = ActivatedAbility(
    id = AbilityId("ninjutsu_$cost"),
    cost = Costs.Composite(
        Costs.Mana(cost),
        Costs.ReturnToHand(GameObjectFilter.Creature.attacking().unblocked()),
    ),
    effect = Effects.Move(
        EffectTarget.Self,
        Zone.BATTLEFIELD,
        placement = ZonePlacement.TappedAndAttacking,
        fromZone = Zone.HAND,
        attackDefender = EnteringAttackDefender.RETURNED_ATTACKER,
    ),
    activateFromZone = Zone.HAND,
    keyword = Keyword.NINJUTSU,
    descriptionOverride = "Ninjutsu $cost ($cost, Return an unblocked attacker you control to hand: " +
        "Put this card onto the battlefield from your hand tapped and attacking.)",
)

/** [ninjutsuAbility] from a cost string — `ninjutsuAbility("{2}{U}{B}")`. */
fun ninjutsuAbility(cost: String): ActivatedAbility = ninjutsuAbility(ManaCost.parse(cost))

/**
 * Add Ninjutsu [cost] (CR 702.49): the [KeywordAbility.Ninjutsu] display marker plus the
 * [ninjutsuAbility] that carries the behaviour.
 */
fun CardBuilder.ninjutsu(cost: String) {
    keywordAbilityList.add(KeywordAbility.ninjutsu(cost))
    activatedAbilities.add(ninjutsuAbility(cost))
}
