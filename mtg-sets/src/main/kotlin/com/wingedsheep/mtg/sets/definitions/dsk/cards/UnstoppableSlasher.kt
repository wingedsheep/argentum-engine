package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

val UnstoppableSlasher = card("Unstoppable Slasher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Assassin"
    oracleText = "Deathtouch\n" +
        "Whenever this creature deals combat damage to a player, they lose half their life, rounded up.\n" +
        "When this creature dies, if it had no counters on it, return it to the battlefield tapped under its owner's control with two stun counters on it."
    power = 2
    toughness = 3

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.LoseHalfLife(
            roundUp = true,
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            lifePlayer = Player.TriggeringPlayer
        )
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = ConditionalEffect(
            condition = Compare(
                DynamicAmount.ContextProperty(ContextPropertyKey.LAST_KNOWN_TOTAL_COUNTER_COUNT),
                ComparisonOperator.EQ,
                DynamicAmount.Fixed(0)
            ),
            effect = Effects.Composite(listOf(
                Effects.PutOntoBattlefield(EffectTarget.Self, tapped = true),
                Effects.AddCounters(Counters.STUN, 2, EffectTarget.Self)
            ))
        )
        description = "When this creature dies, if it had no counters on it, return it to the battlefield tapped under its owner's control with two stun counters on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Maxime Minard"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c78da035-6b5b-4136-9ab6-f622b64fdc54.jpg?1726286292"
    }
}
