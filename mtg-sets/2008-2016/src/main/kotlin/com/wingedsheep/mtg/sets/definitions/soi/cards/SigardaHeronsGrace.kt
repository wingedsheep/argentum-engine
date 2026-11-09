package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sigarda, Heron's Grace
 * {3}{G}{W}
 * Legendary Creature — Angel
 * 4/5
 * Flying
 * You and Humans you control have hexproof.
 * {2}, Exile a card from your graveyard: Create a 1/1 white Human Soldier creature token.
 *
 * "Humans you control" is the bare tribal noun — every Human permanent you control.
 */
val SigardaHeronsGrace = card("Sigarda, Heron's Grace") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Angel"
    power = 4
    toughness = 5
    oracleText = "Flying\nYou and Humans you control have hexproof.\n{2}, Exile a card from your graveyard: Create a 1/1 white Human Soldier creature token."

    keywords(Keyword.FLYING)

    // You have hexproof
    staticAbility {
        ability = GrantHexproofToController
    }

    // Humans you control have hexproof
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HEXPROOF,
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN).youControl())
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.ExileFromGraveyard(1)
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/d/9/d9cbf36e-4044-4f08-9bae-f0dcb2455716.jpg?1783937683"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "250"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db953cb9-52b4-4bf0-86ab-2c3af2faeccf.jpg?1783937711"
    }
}
