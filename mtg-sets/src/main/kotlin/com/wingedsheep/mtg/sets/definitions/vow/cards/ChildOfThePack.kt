package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Child of the Pack // Savage Packmate (Innistrad: Crimson Vow)
 * {2}{R}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Child of the Pack (2/5): "{2}{R}{G}: Create a 2/2 green Wolf creature token"; Daybound.
 * Back  — Savage Packmate (5/5): Trample; "Other creatures you control get +1/+0"; Nightbound.
 *
 * The front's activated ability mints the same 2/2 green Wolf token Howling Moon does (same-set token
 * art). The back's anthem is a +1/+0 [ModifyStats] over [GroupFilter.OtherCreaturesYouControl]
 * (excludeSelf, so the Packmate doesn't pump itself). The back is a transformed face with no mana cost,
 * so its color comes from a color indicator (CR 204): `colorIndicator = "GR"`.
 */

private val ChildOfThePackFront = card("Child of the Pack") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 5
    oracleText = "{2}{R}{G}: Create a 2/2 green Wolf creature token.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    activatedAbility {
        cost = Costs.Mana("{2}{R}{G}")
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            controller = EffectTarget.Controller,
            imageUri = "https://cards.scryfall.io/normal/front/d/5/d5f1e139-3054-4273-8a4d-faaaa9c383a8.jpg?1783924694",
        )
        description = "Create a 2/2 green Wolf creature token."
    }
    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb168e3c-2c78-4e70-a39b-06aa6a47998c.jpg?1783924799"
    }
}

private val SavagePackmate = card("Savage Packmate") {
    manaCost = ""
    colorIdentity = "RG"
    colorIndicator = "GR" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "Other creatures you control get +1/+0.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywords(Keyword.TRAMPLE)
    staticAbility {
        ability = ModifyStats(1, 0, GroupFilter.OtherCreaturesYouControl)
    }
    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/back/c/b/cb168e3c-2c78-4e70-a39b-06aa6a47998c.jpg?1783924799"
    }
}

val ChildOfThePack: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ChildOfThePackFront,
    backFace = SavagePackmate,
)
