package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Departed Deckhand — Core Set 2019 #49 (canonical printing)
 * {1}{U} · Creature — Spirit Pirate · 2/2
 *
 * When this creature becomes the target of a spell, sacrifice it.
 * This creature can't be blocked except by Spirits.
 * {3}{U}: Another target creature you control can't be blocked this turn except by Spirits.
 *
 * Spells only — abilities targeting it don't trigger the sacrifice (same shape as Dream Strix).
 */
val DepartedDeckhand = card("Departed Deckhand") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit Pirate"
    power = 2
    toughness = 2
    oracleText = "When this creature becomes the target of a spell, sacrifice it.\n" +
        "This creature can't be blocked except by Spirits.\n" +
        "{3}{U}: Another target creature you control can't be blocked this turn except by Spirits."

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(spellsOnly = true)
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    }

    staticAbility {
        ability = CantBeBlockedExceptBy(GameObjectFilter.Creature.withSubtype(Subtype.SPIRIT))
    }

    activatedAbility {
        description = "Another target creature you control can't be blocked this turn except by Spirits."
        cost = Costs.Mana("{3}{U}")
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.GrantCantBeBlockedExceptBy(
            creature,
            GameObjectFilter.Creature.withSubtype(Subtype.SPIRIT),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1081df25-1137-4c4d-909b-40e78723652c.jpg?1783934589"
    }
}
