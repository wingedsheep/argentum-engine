package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Encore ability (CR 702.141a):
 *
 *   "[Cost], Exile this card from your graveyard: For each opponent, create a token that's a copy
 *    of this card that attacks that opponent this turn if able. The tokens gain haste. Sacrifice
 *    them at the beginning of the next end step. Activate only as a sorcery."
 *
 * Like [embalmAbility], an ordinary graveyard-activated ability composed of existing primitives:
 *  - the mana [cost] plus [AbilityCost.ExileSelf], `activateFromZone = Zone.GRAVEYARD` and
 *    `timing = TimingRule.SorcerySpeed`;
 *  - `ForEachPlayer(EachOpponent)` — each visit rebinds "you" to that opponent, so the token copy
 *    is created under the source's controller (`Player.ControllerOfSource`, which for the exiled
 *    card is its owner — the player who activated it), and `Player.You` names "that opponent";
 *  - `CreateTokenCopyOfTarget(Self, sacrificeAtStep = END)` — a copy of the card itself (it is in
 *    exile by now), sacrificed at the beginning of the next end step;
 *  - over every token that copy made (a token doubler can make more — "the tokens"): a permanent
 *    haste grant — the tokens *gain* haste, it isn't a copiable value, so a copy of one doesn't
 *    have it — and `MarkMustAttackThisTurn(it, that opponent)`, the player-directed attack
 *    requirement, bound only while the token could attack that player (CR 508.1d).
 */
fun encoreAbility(cost: ManaCost): ActivatedAbility = ActivatedAbility(
    id = AbilityId("encore_$cost"),
    cost = AbilityCost.Composite(
        listOf(AbilityCost.Atom(CostAtom.Mana(cost)), AbilityCost.ExileSelf)
    ),
    effect = Effects.ForEachPlayer(
        Player.EachOpponent,
        listOf(
            Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.Self,
                sacrificeAtStep = Step.END,
                controller = EffectTarget.PlayerRef(Player.ControllerOfSource),
            ),
            Effects.ForEachInCollection(
                CollectionSlot.CreatedTokens,
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent) then
                    Effects.MarkMustAttackThisTurn(
                        EffectTarget.IterationEntity,
                        defender = EffectTarget.PlayerRef(Player.You),
                    ),
            ),
        ),
    ),
    timing = TimingRule.SorcerySpeed,
    activateFromZone = Zone.GRAVEYARD,
    descriptionOverride = "Encore $cost ($cost, Exile this card from your graveyard: For each " +
        "opponent, create a token copy that attacks that opponent this turn if able. They gain " +
        "haste. Sacrifice them at the beginning of the next end step. Activate only as a sorcery.)",
)

/**
 * Add Encore [cost] (CR 702.141, Commander Legends) to a creature card.
 *
 * ```kotlin
 * encore("{3}{U}")
 * ```
 *
 * See [encoreAbility] for how the ability is composed.
 */
fun CardBuilder.encore(cost: String) {
    activatedAbilities.add(encoreAbility(ManaCost.parse(cost)))
    keywordSet.add(Keyword.ENCORE)
}
