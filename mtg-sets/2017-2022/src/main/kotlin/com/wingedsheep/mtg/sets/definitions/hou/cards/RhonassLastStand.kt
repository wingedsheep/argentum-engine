package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rhonas's Last Stand
 * {G}{G}
 * Sorcery
 * Create a 5/4 green Snake creature token. Lands you control don't untap during your next untap step.
 *
 * The untap clause covers every land you control at that untap step, not a set locked at
 * resolution (2017-07-14 ruling).
 */
val RhonassLastStand = card("Rhonas's Last Stand") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create a 5/4 green Snake creature token. Lands you control don't untap during your next untap step."

    spell {
        effect = Effects.CreateToken(
            power = 5,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Snake"),
            imageUri = "https://cards.scryfall.io/normal/front/5/7/57708b3d-ddfb-4924-b5c9-123aa9e967ad.jpg?1783936064"
        ) then Effects.SkipUntap(EffectTarget.Controller, GameObjectFilter.Land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Winona Nelson"
        flavorText = "Rhonas continued to fight until his last breath and even beyond."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e991d1eb-70ab-4e2c-862d-fb8bfcefc542.jpg?1783936013"
        ruling("2017-07-14", "No lands that you control will untap during your next untap step, even lands that aren't tapped as this spell resolves. This includes lands that enter the battlefield after this spell resolves.")
        ruling("2017-07-14", "If more than one spell says that lands you control don't untap during your next untap step, the effects will all wear off during that untap step. You'll untap lands you control during your untap step after that one.")
    }
}
