package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * L'Rell, Fierce Visionary
 * {1}{B}{R}
 * Legendary Creature — Klingon Warrior
 * 1/4
 *
 * At the beginning of combat on your turn, create a 2/1 red Klingon Warrior creature token with haste. Sacrifice it at the beginning of the next end step.
 * {2}, Sacrifice another creature or artifact: Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val LRellFierceVisionary = card("L'Rell, Fierce Visionary") {
    manaCost = "{1}{B}{R}"
    typeLine = "Legendary Creature — Klingon Warrior"
    colorIdentity = "BR"
    oracleText = "At the beginning of combat on your turn, create a 2/1 red Klingon Warrior creature token with haste. Sacrifice it at the beginning of the next end step.\n{2}, Sacrifice another creature or artifact: Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"
    power = 1
    toughness = 4
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Klingon", "Warrior"),
            keywords = setOf(Keyword.HASTE),
            sacrificeAtStep = Step.END
        )
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeAnother(GameObjectFilter.CreatureOrArtifact))
        effect = Effects.Surveil(1)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Patricia Pria"
        flavorText = "\"Conquer us, or we will never relent.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b06bbb9-7c6d-4889-b55a-1de77c27fd18.jpg?1792508377"
    }
}

