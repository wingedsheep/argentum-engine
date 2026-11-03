package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cleveland Booker, Empath
 * {2}{R/G}{R/G}
 * Legendary Creature — Kwejian Rogue
 * 2/2
 *
 * Landfall — Whenever a land you control enters, create a 2/2 red and green Worm creature token
 * with "This token can't block."
 * {8}: Creatures you control get +2/+0 until end of turn.
 */
val ClevelandBookerEmpath = card("Cleveland Booker, Empath") {
    manaCost = "{2}{R/G}{R/G}"
    typeLine = "Legendary Creature — Kwejian Rogue"
    power = 2
    toughness = 2
    oracleText = "Landfall — Whenever a land you control enters, create a 2/2 red and green Worm creature token with \"This token can't block.\"\n" +
        "{8}: Creatures you control get +2/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED, Color.GREEN),
            creatureTypes = setOf("Worm"),
            staticAbilities = listOf(CantBlock()),
            imageUri = "https://cards.scryfall.io/normal/front/e/7/e7cde898-fc1f-4261-8f71-cfc4275bab54.jpg?1793411584",
        )
    }

    activatedAbility {
        cost = Costs.Mana("{8}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(2, 0, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Kaitlyn McCulley"
        flavorText = "Book felt a connection to life-forms that too many others took for granted."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b2e52e2-5cb9-4360-92d6-5c0d9e435d09.jpg?1792508285"
    }
}
