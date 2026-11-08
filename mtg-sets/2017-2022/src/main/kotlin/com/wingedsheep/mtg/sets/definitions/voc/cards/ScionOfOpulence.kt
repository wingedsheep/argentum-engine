package com.wingedsheep.mtg.sets.definitions.voc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Scion of Opulence
 * {2}{R}
 * Creature — Vampire Noble
 * 3/1
 * Whenever this creature or another nontoken Vampire you control dies, create a Treasure token.
 * {R}, Sacrifice two artifacts: Exile the top card of your library. You may play that card this turn.
 *
 * "This creature or another nontoken Vampire you control" folds into one dies trigger over nontoken
 * Vampires you control, Scion being one itself — the shape Assay compiles and Headless Rider uses.
 * A token copy of Scion dying is outside that filter.
 */
val ScionOfOpulence = card("Scion of Opulence") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Noble"
    power = 3
    toughness = 1
    oracleText = "Whenever this creature or another nontoken Vampire you control dies, create a Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "{R}, Sacrifice two artifacts: Exile the top card of your library. You may play that card this turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Vampire").nontoken().youControl()).dies()
        effect = Effects.CreateTreasure()
        description = "Whenever this creature or another nontoken Vampire you control dies, create a Treasure token."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.SacrificeMultiple(2, GameObjectFilter.Artifact))
        effect = Patterns.Exile.impulse(1)
        description = "{R}, Sacrifice two artifacts: Exile the top card of your library. You may play that card this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc0296ec-0401-49c6-82c3-9d7e8aa5ee25.jpg?1783924998"
    }
}
