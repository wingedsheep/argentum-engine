package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Decree of Silence
 * {6}{U}{U}
 * Enchantment
 * Whenever an opponent casts a spell, counter that spell and put a depletion counter
 * on Decree of Silence. If there are three or more depletion counters on Decree of
 * Silence, sacrifice it.
 * Cycling {4}{U}{U}
 * When you cycle Decree of Silence, you may counter target spell.
 */
val DecreeOfSilence = card("Decree of Silence") {
    manaCost = "{6}{U}{U}"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent casts a spell, counter that spell and put a depletion counter on Decree of Silence. If there are three or more depletion counters on Decree of Silence, sacrifice it.\nCycling {4}{U}{U}\nWhen you cycle Decree of Silence, you may counter target spell."

    // Ability 1: Whenever an opponent casts a spell, counter that spell + depletion counter + sacrifice check
    triggeredAbility {
        trigger = TriggerSpec(
            event = GameEvent.SpellCastEvent(player = Player.Opponent),
            binding = TriggerBinding.ANY
        )
        effect = Effects.CounterTriggeringSpell()
            .then(Effects.AddCounters("depletion", 1, EffectTarget.Self))
            .then(
                ConditionalEffect(
                    condition = Compare(
                        DynamicAmount.CountersOnSelf(CounterTypeFilter.Named("depletion")),
                        ComparisonOperator.GTE,
                        DynamicAmount.Fixed(3)
                    ),
                    effect = SacrificeSelfEffect
                )
            )
    }

    // Cycling {4}{U}{U}
    keywordAbility(KeywordAbility.cycling("{4}{U}{U}"))

    // When you cycle this card, you may counter target spell.
    triggeredAbility {
        trigger = Triggers.YouCycleThis
        val t = target("target spell", Targets.Spell)
        effect = MayEffect(Effects.CounterSpell())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2fc46e2-5e19-4999-a4cd-1e84697066c1.jpg?1562536891"
        ruling("2022-12-08", "When you cycle this card, first the cycling ability goes on the stack, then the triggered ability goes on the stack on top of it. The triggered ability will resolve before you draw a card from the cycling ability.")
        ruling("2022-12-08", "The cycling ability and the triggered ability are separate. If the triggered ability doesn't resolve (because, for example, it has been countered, or all of its targets have become illegal), the cycling ability will still resolve, and you'll draw a card.")
        ruling("2022-12-08", "You can cycle this card even if there are no legal targets for the triggered ability.")
    }
}
