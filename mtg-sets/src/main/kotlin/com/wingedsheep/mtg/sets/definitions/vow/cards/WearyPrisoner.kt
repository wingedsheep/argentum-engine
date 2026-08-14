package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Weary Prisoner // Wrathful Jailbreaker (Innistrad: Crimson Vow)
 * {3}{R}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Weary Prisoner (2/6): Defender; Daybound.
 * Back  — Wrathful Jailbreaker (6/6): "This creature attacks each combat if able"; Nightbound.
 *
 * The front is a wall that flips into an all-out attacker at night: [Keyword.DEFENDER] on the front,
 * the [MustAttack] static (defaulting to `GroupFilter.source()`, i.e. "this creature attacks each
 * combat if able") on the back. The back is a transformed face with no mana cost, so its color comes
 * from a color indicator (CR 204): `colorIndicator = "R"`.
 */

private val WearyPrisonerFront = card("Weary Prisoner") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 6
    oracleText = "Defender\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywords(Keyword.DEFENDER)
    daybound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e641467b-ac2e-4d29-aed7-5cc227c3b1ce.jpg?1783924828"
    }
}

private val WrathfulJailbreaker = card("Wrathful Jailbreaker") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 6
    toughness = 6
    oracleText = "This creature attacks each combat if able.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    staticAbility {
        ability = MustAttack()
    }
    nightbound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/back/e/6/e641467b-ac2e-4d29-aed7-5cc227c3b1ce.jpg?1783924828"
    }
}

val WearyPrisoner: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = WearyPrisonerFront,
    backFace = WrathfulJailbreaker,
)
