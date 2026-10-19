package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Historian's Wisdom — Kamigawa: Neon Dynasty #192
 * {2}{G} · Enchantment — Aura
 *
 * Enchant artifact or creature
 * When this Aura enters, if enchanted permanent is a creature with the greatest power among
 * creatures on the battlefield, draw a card.
 * As long as enchanted permanent is a creature, it gets +2/+1.
 *
 * "A creature with the greatest power" ≡ the host is a creature AND its power is at least the
 * battlefield-wide maximum creature power (ties count). The +2/+1 already applies when the
 * intervening-if is checked, as it should.
 */
val HistoriansWisdom = card("Historian's Wisdom") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "When this Aura enters, if enchanted permanent is a creature with the greatest power among creatures on the battlefield, draw a card.\n" +
        "As long as enchanted permanent is a creature, it gets +2/+1."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.All(
            Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature),
            Conditions.CompareAmounts(
                DynamicAmounts.powerOf(EffectTarget.EnchantedPermanent),
                ComparisonOperator.GTE,
                DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature).maxPower()
            )
        )
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = ModifyStats(2, 1, GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Jarel Threat"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/941f91bb-f5a3-4cca-85b5-b610502460bb.jpg?1783923847"
    }
}
