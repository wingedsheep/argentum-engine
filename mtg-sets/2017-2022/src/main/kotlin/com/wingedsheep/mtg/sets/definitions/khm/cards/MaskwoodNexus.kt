package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.IsAllCreatureTypes
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Maskwood Nexus
 * {4}
 * Artifact
 * Creatures you control are every creature type. The same is true for creature spells you control
 * and creature cards you own that aren't on the battlefield.
 * {3}, {T}: Create a 2/2 blue Shapeshifter creature token with changeling. (It is every creature type.)
 *
 * [IsAllCreatureTypes] rather than a granted changeling: the Nexus adds types, not the keyword.
 * Its battlefield half is Layer 4 projection; the two cross-zone flags reach spells on the stack
 * and cards in hand/library/graveyard/exile through `ProjectedState.crossZoneGrantedSubtypes`,
 * the overlay Leyline of Transformation uses for its chosen type.
 */
val MaskwoodNexus = card("Maskwood Nexus") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Creatures you control are every creature type. The same is true for creature spells you control " +
        "and creature cards you own that aren't on the battlefield.\n" +
        "{3}, {T}: Create a 2/2 blue Shapeshifter creature token with changeling. (It is every creature type.)"

    staticAbility {
        ability = IsAllCreatureTypes(
            filter = GroupFilter.AllCreaturesYouControl,
            includeControlledSpells = true,
            includeOwnedCardsOutsideBattlefield = true
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Shapeshifter"),
            keywords = setOf(Keyword.CHANGELING),
        )
        description = "{3}, {T}: Create a 2/2 blue Shapeshifter creature token with changeling."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/471d2aef-cfd4-4131-bbc7-62eeed9f3343.jpg?1783928186"
    }
}
