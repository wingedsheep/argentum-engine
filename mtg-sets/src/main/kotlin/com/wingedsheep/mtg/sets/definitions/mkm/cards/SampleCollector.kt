package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Sample Collector
 * {2}{G}
 * Creature — Troll Detective
 * 2/3
 *
 * Whenever this creature attacks, you may collect evidence 3. When you do, put a +1/+1 counter on
 * target creature you control.
 *
 * The **effect** shape: collecting evidence here is not a cost at all, so it uses
 * [Effects.CollectEvidence] rather than any of the cost rails, and nothing about it is *linked* —
 * no ability on this card asks "was evidence collected".
 *
 * "When you do" is a genuine reflexive trigger (CR 603.12), not an "if you do" continuation: it goes
 * on the stack as its own triggered ability *after* the collection resolves, so its target is chosen
 * then and opponents get priority to respond to it. That is exactly what
 * [ReflexiveTriggerEffect]'s `reflexiveTargetRequirements` models.
 *
 * The `optional = true` prompt is only offered when CR 701.59b is satisfied — the engine's
 * feasibility check asks the graveyard whether it can reach 3 before asking the player, so an
 * attacker with a thin graveyard never sees a choice it couldn't take.
 */
val SampleCollector = card("Sample Collector") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Troll Detective"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature attacks, you may collect evidence 3. When you do, put a " +
        "+1/+1 counter on target creature you control. (To collect evidence 3, exile cards with " +
        "total mana value 3 or greater from your graveyard.)"

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = ReflexiveTriggerEffect(
            action = Effects.CollectEvidence(3),
            optional = true,
            reflexiveEffect = Effects.AddCounters(
                Counters.PLUS_ONE_PLUS_ONE, 1, com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget(0)
            ),
            reflexiveTargetRequirements = listOf(
                TargetCreature(filter = TargetFilter.Creature.youControl())
            ),
        )
        description = "Whenever this creature attacks, you may collect evidence 3. When you do, " +
            "put a +1/+1 counter on target creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Borja Pindado"
        flavorText = "\"Whatever broke out of here is *at least* half bear... and I'd say some " +
            "lizard as well.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76f7480c-82cc-4ddd-b619-c1a609c29a13.jpg"
    }
}
