package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Weaver of Blossoms // Blossom-Clad Werewolf (Innistrad: Crimson Vow)
 * {2}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Weaver of Blossoms (2/3): "{T}: Add one mana of any color"; Daybound.
 * Back  — Blossom-Clad Werewolf (3/4): "{T}: Add two mana of any one color"; Nightbound.
 *
 * A mana-dork werewolf. Both faces are a `{T}` mana ability via [Effects.AddAnyColorMana]: the front
 * adds one mana of a chosen color; the back adds *two* of a single chosen color — which is exactly
 * `AddAnyColorMana(2)` ("any one color", the player picks a color and gets two of it, per the facade's
 * Gilded Lotus note — not two independently-chosen colors). `manaAbility = true` on both so they don't
 * use the stack. The back is a transformed face with no mana cost, so its color comes from a color
 * indicator (CR 204): `colorIndicator = "G"`.
 */

private val WeaverOfBlossomsFront = card("Weaver of Blossoms") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 3
    oracleText = "{T}: Add one mana of any color.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        description = "Add one mana of any color."
    }
    daybound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "226"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73bf2a0a-b97d-4b90-abd6-d1755734ea15.jpg?1783924806"
    }
}

private val BlossomCladWerewolf = card("Blossom-Clad Werewolf") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 4
    oracleText = "{T}: Add two mana of any one color.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(2)
        manaAbility = true
        description = "Add two mana of any one color."
    }
    nightbound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "226"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/back/7/3/73bf2a0a-b97d-4b90-abd6-d1755734ea15.jpg?1783924806"
    }
}

val WeaverOfBlossoms: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = WeaverOfBlossomsFront,
    backFace = BlossomCladWerewolf,
)
