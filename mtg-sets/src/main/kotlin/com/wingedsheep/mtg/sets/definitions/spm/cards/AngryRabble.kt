package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.SpellCastEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angry Rabble
 * {1}{R}
 * Creature — Human Citizen, 2/2
 * Trample
 * Whenever a player casts a spell with mana value 4 or greater, Angry Rabble deals 1 damage to each opponent.
 * {5}{R}: Put two +1/+1 counters on Angry Rabble. Activate only as a sorcery.
 */
val AngryRabble = card("Angry Rabble") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Citizen"
    power = 2
    toughness = 2
    oracleText = "Trample\nWhenever a player casts a spell with mana value 4 or greater, Angry Rabble deals 1 damage to each opponent.\n{5}{R}: Put two +1/+1 counters on Angry Rabble. Activate only as a sorcery."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = TriggerSpec(
            event = SpellCastEvent(spellFilter = GameObjectFilter.Any.manaValueAtLeast(4), player = Player.Each),
            binding = TriggerBinding.ANY
        )
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    activatedAbility {
        cost = Costs.Mana("{5}{R}")
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Bartek Fedyczak"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/938730fa-496f-4871-80ec-3e9843ecb219.jpg?1757377232"
    }
}
