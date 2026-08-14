package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ferocious Werefox // Guard Change
 * {3}{G}
 * Creature — Elf Fox Warrior
 * 4/3
 * Trample
 *
 * Adventure: Guard Change — {1}{G}, Instant — Adventure
 * Create a Monster Role token attached to target creature you control.
 * (Enchanted creature gets +1/+1 and has trample.)
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val FerociousWerefox = card("Ferocious Werefox") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Fox Warrior"
    oracleText = "Trample"
    power = 4
    toughness = 3

    keywords(Keyword.TRAMPLE)

    adventure("Guard Change") {
        manaCost = "{1}{G}"
        typeLine = "Instant — Adventure"
        oracleText = "Create a Monster Role token attached to target creature you control. " +
            "(If you control another Role on it, put that one into the graveyard. Enchanted creature " +
            "gets +1/+1 and has trample.) " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target("target creature you control", Targets.CreatureYouControl)
            effect = Effects.CreateRoleToken("Monster Role", t)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Caroline Gariba"
        flavorText = "Each night, the elves of Redtooth Keep transform into feral, monstrous beasts."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac1907e8-0713-47dd-ac42-bf1323c5bec0.jpg?1783915082"
        ruling(
            "2023-09-01",
            "If a permanent has more than one Role attached to it controlled by the same player, each " +
                "of those Roles except the one with the most recent timestamp is put into its owner's " +
                "graveyard. This is a state-based action."
        )
        ruling(
            "2023-09-01",
            "In rare cases, a spell or ability might attempt to create a Role token enchanting a " +
                "permanent that it can't legally enchant (because of an ability like protection from " +
                "enchantments). In such cases, the Role token isn't created."
        )
    }
}
