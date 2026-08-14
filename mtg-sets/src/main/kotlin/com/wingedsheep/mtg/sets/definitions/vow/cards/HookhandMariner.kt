package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Hookhand Mariner // Riphook Raider (Innistrad: Crimson Vow)
 * {3}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Hookhand Mariner (4/4): Daybound (a vanilla body plus the keyword).
 * Back  — Riphook Raider (6/4): "This creature can't be blocked by creatures with power 2 or less";
 *          Nightbound.
 *
 * The back's evasion is the standard [CantBeBlockedBy] over [GameObjectFilter.Creature.powerAtMost] —
 * the same idiom Sandman, Shifting Scoundrel and Cavern Stomper use for "power 2 or less" blockers.
 * The back is a transformed face with no mana cost, so its color comes from a color indicator (CR 204):
 * `colorIndicator = "G"`.
 */

private val HookhandMarinerFront = card("Hookhand Mariner") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 4
    toughness = 4
    oracleText = "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    daybound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54a4b031-0919-44aa-a35e-68da7a27235a.jpg?1783924817"
    }
}

private val RiphookRaider = card("Riphook Raider") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 6
    toughness = 4
    oracleText = "This creature can't be blocked by creatures with power 2 or less.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2))
    }
    nightbound()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/back/5/4/54a4b031-0919-44aa-a35e-68da7a27235a.jpg?1783924817"
    }
}

val HookhandMariner: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = HookhandMarinerFront,
    backFace = RiphookRaider,
)
