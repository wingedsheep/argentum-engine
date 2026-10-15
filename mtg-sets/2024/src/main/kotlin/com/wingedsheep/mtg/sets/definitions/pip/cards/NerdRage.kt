package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Nerd Rage
 * {2}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, draw two cards.
 * Enchanted creature has "You have no maximum hand size" and "Whenever this creature attacks,
 * if you have ten or more cards in hand, it gets +10/+10 until end of turn."
 *
 * Both abilities live on the creature, so "you" is the creature's controller — who may not be
 * the Aura's controller.
 */
val NerdRage = card("Nerd Rage") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, draw two cards.\n" +
        "Enchanted creature has \"You have no maximum hand size\" and \"Whenever this creature attacks, " +
        "if you have ten or more cards in hand, it gets +10/+10 until end of turn.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(2)
    }

    staticAbility {
        ability = GrantStaticAbility(NoMaximumHandSize, filter = Filters.EnchantedCreature)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.attacks()
                interveningIf = Conditions.CardsInHandAtLeast(10)
                effect = Effects.ModifyStats(10, 10, EffectTarget.Self)
                description = "Whenever this creature attacks, if you have ten or more cards in hand, " +
                    "it gets +10/+10 until end of turn."
            },
            filter = Filters.EnchantedCreature,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9857669-5fad-458a-8eda-c1790298add7.jpg?1783912412"
    }
}
