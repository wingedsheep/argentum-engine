package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.SetColors
import com.wingedsheep.sdk.scripting.SpendManaAsColor
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mycosynth Lattice
 * {6}
 * Artifact
 *
 * All permanents are artifacts in addition to their other types.
 * All cards that aren't on the battlefield, spells, and permanents are colorless.
 * Players may spend mana as though it were mana of any color.
 *
 * Three statics. The type grant is battlefield-only (the ruling: spells and cards elsewhere aren't
 * permanents, so they don't become artifacts). The color clause is a [SetColors] to no colors over
 * every permanent, reaching every player's spells and off-battlefield cards (`offBattlefield =
 * Player.Each`); its filter is [GameObjectFilter.Any] so no card predicate narrows the off-battlefield
 * half. The mana clause is [SpendManaAsColor] with both ends open — any mana, colorless included
 * (the ruling), as any color, for every player — which leaves `{C}` pips and spending restrictions
 * intact.
 */
val MycosynthLattice = card("Mycosynth Lattice") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "All permanents are artifacts in addition to their other types.\n" +
        "All cards that aren't on the battlefield, spells, and permanents are colorless.\n" +
        "Players may spend mana as though it were mana of any color."

    staticAbility {
        ability = GrantCardType(cardType = "ARTIFACT", filter = GroupFilter.AllPermanents)
    }
    staticAbility {
        ability = SetColors(colors = emptySet(), filter = GroupFilter(GameObjectFilter.Any), offBattlefield = Player.Each)
    }
    staticAbility {
        ability = SpendManaAsColor(fromColor = null, toColor = null, players = Player.Each)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Anthony S. Waters & Cara Mitten"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7e7f15a-074a-4137-88ca-e5d376d146fd.jpg?1783944422"
    }
}
