package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Wildwood Mentor
 * {2}{G}
 * Creature — Treefolk
 * 1/1
 *
 * Whenever a token you control enters, put a +1/+1 counter on this creature.
 * Whenever this creature attacks, another target attacking creature gets +X/+X until end of turn,
 * where X is this creature's power.
 *
 * The first ability is per-token, not once per batch — "a token you control enters" with
 * [TriggerBinding.ANY] over `GameObjectFilter.Any.youControl().token()`, so a single effect making
 * three tokens adds three counters. Any token counts, not just creature tokens (Food, Treasure and
 * Role tokens all feed it).
 *
 * The attack trigger's +X/+X reads Wildwood Mentor's power *as the ability resolves* via
 * [EntityReference.Source] — which is after the counter triggers from the same combat have already
 * resolved, and after any pump in response. It resolves to a fixed +X/+X modification at that
 * moment, so it does not re-scale if the Mentor grows or dies later in the turn. `.other()` on the
 * target filter is the printed "another": the Mentor can't pump itself, and with no other attacker
 * the trigger simply has no legal target.
 */
val WildwoodMentor = card("Wildwood Mentor") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 1
    toughness = 1
    oracleText = "Whenever a token you control enters, put a +1/+1 counter on this creature.\n" +
        "Whenever this creature attacks, another target attacking creature gets +X/+X until end " +
        "of turn, where X is this creature's power."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Any.youControl().token(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever a token you control enters, put a +1/+1 counter on this creature."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        val ally = target(
            "another target attacking creature",
            TargetCreature(filter = TargetFilter.Creature.attacking().other())
        )
        val sourcePower = DynamicAmount.EntityProperty(
            EntityReference.Source,
            EntityNumericProperty.Power
        )
        effect = Effects.ModifyStats(power = sourcePower, toughness = sourcePower, target = ally)
        description = "Whenever this creature attacks, another target attacking creature gets " +
            "+X/+X until end of turn, where X is this creature's power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "322"
        artist = "Piotr Foksowicz"
        flavorText = "\"Not bad. If only you had branches.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96c247f4-06cf-4c41-8285-d44d40f4130c.jpg?1783915037"
    }
}
