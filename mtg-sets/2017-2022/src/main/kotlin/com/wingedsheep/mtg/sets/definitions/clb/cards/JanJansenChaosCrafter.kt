package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Jan Jansen, Chaos Crafter
 * {R}{W}{B}
 * Legendary Creature — Gnome Artificer
 * 3/3
 *
 * Haste
 * {T}, Sacrifice an artifact creature: Create two Treasure tokens.
 * {T}, Sacrifice a noncreature artifact: Create two 1/1 colorless Construct artifact creature tokens.
 */
val JanJansenChaosCrafter = card("Jan Jansen, Chaos Crafter") {
    manaCost = "{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Legendary Creature — Gnome Artificer"
    oracleText = "Haste\n" +
        "{T}, Sacrifice an artifact creature: Create two Treasure tokens.\n" +
        "{T}, Sacrifice a noncreature artifact: Create two 1/1 colorless Construct artifact creature tokens."
    power = 3
    toughness = 3

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.ArtifactCreature))
        effect = Effects.CreateTreasure(2)
        description = "{T}, Sacrifice an artifact creature: Create two Treasure tokens."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Artifact.notCreature()))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Construct"),
            count = 2,
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/0/f/0f5715cf-fbf6-4b13-af91-525edc7706ea.jpg?1783922318"
        )
        description = "{T}, Sacrifice a noncreature artifact: Create two 1/1 colorless Construct artifact creature tokens."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "277"
        artist = "Vladimir Krisetskiy"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b215a08-41db-4440-8e33-34a84a33db50.jpg?1783922693"
    }
}
