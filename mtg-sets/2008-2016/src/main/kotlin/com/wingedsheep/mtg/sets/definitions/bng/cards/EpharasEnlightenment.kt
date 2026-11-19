package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ephara's Enlightenment
 * {1}{W}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, put a +1/+1 counter on enchanted creature.
 * Enchanted creature has flying.
 * Whenever a creature you control enters, you may return this Aura to its owner's hand.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val EpharasEnlightenment = card("Ephara's Enlightenment") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, put a +1/+1 counter on enchanted creature.\n" +
        "Enchanted creature has flying.\n" +
        "Whenever a creature you control enters, you may return this Aura to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.May(Effects.ReturnToHand(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e480d04-9a35-463e-928e-8da3556629e6.jpg?1783939526"
    }
}
