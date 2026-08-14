package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Oakshade Stalker // Moonlit Ambusher (Innistrad: Crimson Vow)
 * {2}{G}
 * Creature — Human Ranger Werewolf // Creature — Werewolf
 *
 * Front — Oakshade Stalker (3/3): "You may cast this spell as though it had flash if you pay {2} more
 *          to cast it"; Daybound.
 * Back  — Moonlit Ambusher (6/3): Nightbound (a vanilla body plus the keyword).
 *
 * The front's optional-flash clause is [KeywordAbility.flashKicker], the same
 * "pay {2} more → cast at instant speed" rail Harbinger of the Tides uses (an `OptionalAdditionalCost`
 * with `grantsFlashTiming`). The back is a transformed face with no mana cost, so its color comes from
 * a color indicator (CR 204): `colorIndicator = "G"`.
 */

private val OakshadeStalkerFront = card("Oakshade Stalker") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Ranger Werewolf"
    power = 3
    toughness = 3
    oracleText = "You may cast this spell as though it had flash if you pay {2} more to cast it.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywordAbility(KeywordAbility.flashKicker("{2}"))
    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bcaa944-4e45-457c-be9c-07377b6ed08b.jpg?1783924813"
    }
}

private val MoonlitAmbusher = card("Moonlit Ambusher") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 6
    toughness = 3
    oracleText = "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/back/8/b/8bcaa944-4e45-457c-be9c-07377b6ed08b.jpg?1783924813"
    }
}

val OakshadeStalker: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = OakshadeStalkerFront,
    backFace = MoonlitAmbusher,
)
