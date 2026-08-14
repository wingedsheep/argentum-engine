package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Infestation Expert // Infested Werewolf (Innistrad: Crimson Vow)
 * {4}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Infestation Expert (3/4): "Whenever this creature enters or attacks, create a 1/1 green Insect
 *          creature token"; Daybound.
 * Back  — Infested Werewolf (4/5): "Whenever this creature enters or attacks, create two 1/1 green Insect
 *          creature tokens"; Nightbound.
 *
 * "Enters or attacks" is two separate triggered abilities per face — an [Triggers.EntersBattlefield] and
 * an [Triggers.Attacks] — each minting Insect tokens ([Effects.CreateToken], same-set Insect art). The
 * night face makes two per event by asking for `count = 2`. The back is a transformed face with no mana
 * cost, so its color comes from a color indicator (CR 204): `colorIndicator = "G"`.
 */

private const val INSECT_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/f/3/f3ac66f8-4bb2-42bd-9a18-7da9c3839c8b.jpg?1783924696"

private val InfestationExpertFront = card("Infestation Expert") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 3
    toughness = 4
    oracleText = "Whenever this creature enters or attacks, create a 1/1 green Insect creature token.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            controller = EffectTarget.Controller,
            imageUri = INSECT_TOKEN_IMAGE,
        )
        description = "Create a 1/1 green Insect creature token."
    }
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            controller = EffectTarget.Controller,
            imageUri = INSECT_TOKEN_IMAGE,
        )
        description = "Create a 1/1 green Insect creature token."
    }
    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0936761c-7cdc-49ef-8a0d-ed79219f1056.jpg?1783924815"
    }
}

private val InfestedWerewolf = card("Infested Werewolf") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 5
    oracleText = "Whenever this creature enters or attacks, create two 1/1 green Insect creature tokens.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            controller = EffectTarget.Controller,
            count = 2,
            imageUri = INSECT_TOKEN_IMAGE,
        )
        description = "Create two 1/1 green Insect creature tokens."
    }
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            controller = EffectTarget.Controller,
            count = 2,
            imageUri = INSECT_TOKEN_IMAGE,
        )
        description = "Create two 1/1 green Insect creature tokens."
    }
    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/back/0/9/0936761c-7cdc-49ef-8a0d-ed79219f1056.jpg?1783924815"
    }
}

val InfestationExpert: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = InfestationExpertFront,
    backFace = InfestedWerewolf,
)
