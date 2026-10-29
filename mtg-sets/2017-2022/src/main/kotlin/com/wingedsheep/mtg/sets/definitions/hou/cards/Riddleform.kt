package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Riddleform
 * {1}{U}
 * Enchantment
 * Whenever you cast a noncreature spell, you may have this enchantment become a 3/3 Sphinx
 * creature with flying in addition to its other types until end of turn.
 * {2}{U}: Scry 1.
 *
 * Same shape as Reptilian Reflection: "in addition to its other types" is
 * [Effects.BecomeCreature]'s default (CREATURE is added, Enchantment stays). The cast trigger
 * resolves before the spell that caused it, even if that spell is later countered.
 */
val Riddleform = card("Riddleform") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a noncreature spell, you may have this enchantment become a 3/3 " +
        "Sphinx creature with flying in addition to its other types until end of turn.\n" +
        "{2}{U}: Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        optional = true
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.FLYING),
            creatureTypes = setOf("Sphinx"),
            duration = Duration.EndOfTurn
        )
        description = "Whenever you cast a noncreature spell, you may have this enchantment become a 3/3 " +
            "Sphinx creature with flying in addition to its other types until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        effect = Effects.Scry(1)
        description = "{2}{U}: Scry 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e54dc4a-3fd6-4895-8497-3a19e4e9a3d8.jpg?1783936049"
        ruling("2020-06-23", "Riddleform's first ability resolves before the spell that caused it to trigger but after targets for that spell are chosen. It will resolve even if that spell is countered.")
        ruling("2020-06-23", "If Riddleform becomes a creature the same turn it enters the battlefield, you can't attack with it unless it gains haste.")
    }
}
