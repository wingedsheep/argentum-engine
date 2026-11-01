package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Rubblebelt Runner — Ravnica Allegiance #202
 * {1}{R}{G} · Creature — Lizard Warrior · 3 / 3
 *
 * This creature can't be blocked by creature tokens.
 *
 * Same evasion static as Duskwatch Hunter: `CantBeBlockedBy(Creature.token())`.
 */
val RubblebeltRunner = card("Rubblebelt Runner") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Lizard Warrior"
    power = 3
    toughness = 3
    oracleText = "This creature can't be blocked by creature tokens."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "Deruchenko Alexander"
        flavorText = "The sly, stealthy warriors of the Slizt Clan survive by hiding in high ground and " +
            "ambushing their enemies. Other Gruul call them skulkers and cowards, but only from a safe distance."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78cc4e02-949f-4879-9f32-7c33490d0b45.jpg?1783933638"
    }
}
