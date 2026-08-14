package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bruce Banner // The Incredible Hulk — Marvel Super Heroes #49 (mythic)
 *
 * Front — Bruce Banner · {U} · Legendary Creature — Human Scientist Hero · 1/1
 *   {X}{X}, {T}: Draw X cards. Activate only as a sorcery.
 *   {2}{R}{R}{G}{G}: Transform Bruce Banner. Activate only as a sorcery.
 *
 * Back — The Incredible Hulk · Legendary Creature — Gamma Berserker Hero · 8/8
 *   Reach, trample
 *   Enrage — Whenever The Incredible Hulk is dealt damage, put a +1/+1 counter on him. If he's
 *     attacking, untap him and there is an additional combat phase after this phase.
 *
 * A **modal** double-faced creature ([CardDefinition.modalDoubleFacedPermanent]), the shape the
 * whole MSH hero cycle shares. CR 712.3 lets a modal DFC also transform, and this card uses both
 * routes to the same back face: cast it from hand for its own `{2}{R}{R}{G}{G}` (CR 712.11b/712.11c), or
 * transform into it with the front's sorcery-speed [TransformEffect] ability
 * ([TimingRule.SorcerySpeed]). So the back carries its printed mana cost and *no* color indicator —
 * its R/G comes from that cost — and per CR 712.8f (which, unlike CR 712.8e for nonmodal DFCs, has
 * no mana-value exception) the transformed permanent has the back face's mana value, not the
 * front's.
 *
 *  - **`{X}{X}, {T}: Draw X cards`** — the Gogo, Master of Mimicry idiom: the `{X}{X}` mana cost
 *    pays X twice, and the draw reads the single chosen X at resolution via
 *    [DynamicAmount.XValue]. X may legally be 0 (the card sets no minimum), which just draws
 *    nothing. "Activate only as a sorcery" is [TimingRule.SorcerySpeed].
 *
 *  - **Enrage** is an ability word — flavor only, no rules meaning — over an ordinary
 *    [Triggers.TakesDamage] trigger bound to the source. It fires on *any* damage (combat or not,
 *    from any source, including damage that is lethal: the trigger still goes on the stack even
 *    though the Hulk may already be in the graveyard when it resolves, in which case the counter
 *    has nowhere to go).
 *
 *  - **"If he's attacking, untap him and there is an additional combat phase after this phase"** is
 *    checked when the trigger *resolves*, not when it fires — so it is a [ConditionalEffect] over
 *    [Conditions.SourceIsAttacking], not an intervening-if `triggerCondition`. The body is the
 *    Combat Celebrant / Genji Glove pair: [Effects.Untap] on the source so he can attack again,
 *    then [Effects.AddCombatPhase], which inserts one extra combat phase (no trailing main phase)
 *    after the current one. Unlike Éomer and Genji Glove there is deliberately **no** `oncePerTurn`
 *    cap here — the printed card has no such rider, and the extra combats are gated by needing a
 *    fresh source of damage each time.
 */

private val BruceBannerFront = card("Bruce Banner") {
    manaCost = "{U}"
    colorIdentity = "URG"
    typeLine = "Legendary Creature — Human Scientist Hero"
    power = 1
    toughness = 1
    oracleText = "{X}{X}, {T}: Draw X cards. Activate only as a sorcery.\n" +
        "{2}{R}{R}{G}{G}: Transform Bruce Banner. Activate only as a sorcery."

    // {X}{X}, {T}: Draw X cards. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{X}"), Costs.Tap)
        effect = Effects.DrawCards(DynamicAmount.XValue)
        timing = TimingRule.SorcerySpeed
        description = "{X}{X}, {T}: Draw X cards. Activate only as a sorcery."
    }

    // {2}{R}{R}{G}{G}: Transform Bruce Banner. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{2}{R}{R}{G}{G}")
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Bruce Banner. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "49"
        artist = "Tommy Arnold"
        flavorText = "\"You shouldn't make me angry . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0dbbdcf-84e1-494f-8b8c-0a094f603fa9.jpg?1783902972"
    }
}

private val TheIncredibleHulkBack = card("The Incredible Hulk") {
    manaCost = "{2}{R}{R}{G}{G}"
    colorIdentity = "URG"
    typeLine = "Legendary Creature — Gamma Berserker Hero"
    power = 8
    toughness = 8
    oracleText = "Reach, trample\n" +
        "Enrage — Whenever The Incredible Hulk is dealt damage, put a +1/+1 counter on him. If " +
        "he's attacking, untap him and there is an additional combat phase after this phase."

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    // Enrage — Whenever The Incredible Hulk is dealt damage, put a +1/+1 counter on him. If he's
    // attacking, untap him and there is an additional combat phase after this phase.
    triggeredAbility {
        trigger = Triggers.TakesDamage
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            ConditionalEffect(
                // "If he's attacking" — a *live* check, hence the `onBattlefield()` conjunct.
                // Bare `SourceIsAttacking` resolves through PredicateEvaluator, whose IsAttacking
                // arm falls back to `LastKnownPermanentComponent.snapshot.wasAttacking` for any
                // object off the battlefield (CR 608.2h, what Garna needs). Enrage routinely fires
                // on lethal damage, so without the conjunct a Hulk that died to the very damage
                // that triggered it still read as attacking and handed out a free combat phase.
                condition = Conditions.SourceMatches(
                    GameObjectFilter.Any.onBattlefield().attacking(),
                ),
                effect = Effects.Composite(
                    // "untap him"
                    Effects.Untap(EffectTarget.Self),
                    // "there is an additional combat phase after this phase" (combat only — no main)
                    Effects.AddCombatPhase,
                ),
            ),
        )
        description = "Enrage — Whenever The Incredible Hulk is dealt damage, put a +1/+1 counter " +
            "on him. If he's attacking, untap him and there is an additional combat phase after " +
            "this phase."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "49"
        artist = "Tommy Arnold"
        flavorText = "\". . . You wouldn't like me when I'M ANGRY!!!\""
        imageUri = "https://cards.scryfall.io/normal/back/e/0/e0dbbdcf-84e1-494f-8b8c-0a094f603fa9.jpg?1783902972"
    }
}

val BruceBanner: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = BruceBannerFront,
    backFace = TheIncredibleHulkBack,
)
