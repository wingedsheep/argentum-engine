package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Budoka Gardener // Dokai, Weaver of Life (Champions of Kamigawa #202) — a flip card (CR 710).
 *
 * Budoka Gardener {1}{G} — Creature — Human Monk 2/1
 * "{T}: You may put a land card from your hand onto the battlefield. If you control ten or more
 * lands, flip this creature."
 *
 * Dokai, Weaver of Life — Legendary Creature — Human Monk 3/3
 * "{4}{G}{G}, {T}: Create an X/X green Elemental creature token, where X is the number of lands
 * you control."
 *
 * The land count is taken on resolution after the optional land drop, and the flip check does not
 * depend on having put a land down (see rulings).
 */
private val BudokaGardenerUpright = card("Budoka Gardener") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Monk"
    oracleText = "{T}: You may put a land card from your hand onto the battlefield. If you control ten " +
        "or more lands, flip this creature."
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Hand.putFromHand(filter = GameObjectFilter.Land) then
            Effects.If(Conditions.ControlLandsAtLeast(10), Effects.Flip())
        description = "{T}: You may put a land card from your hand onto the battlefield. If you control " +
            "ten or more lands, flip this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49999b95-5e62-414c-b975-4191b9c1ab39.jpg?1783944292"
        ruling(
            "2004-12-01",
            "Budoka Gardener flips if you control ten or more lands when its ability resolves, even if " +
                "you don't use its ability to put a land onto the battlefield."
        )
        ruling(
            "2013-07-01",
            "Budoka Gardener flips even if you put a second copy of a legendary land onto the battlefield. " +
                "You'll have ten lands on the battlefield while the ability is resolving, so the Gardener " +
                "flips. Then you will choose one of the legendary lands to keep and put the other into its " +
                "owner's graveyard as a state-based action."
        )
    }
}

private val DokaiWeaverOfLife = card("Dokai, Weaver of Life") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Monk"
    oracleText = "{4}{G}{G}, {T}: Create an X/X green Elemental creature token, where X is the number " +
        "of lands you control."
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}{G}"), Costs.Tap)
        effect = Effects.CreateDynamicToken(
            dynamicPower = DynamicAmounts.landsYouControl(),
            dynamicToughness = DynamicAmounts.landsYouControl(),
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elemental"),
            imageUri = "https://cards.scryfall.io/normal/front/c/5/c5ad13b4-bbf5-4c98-868f-4d105eaf8833.jpg?1783934351",
        )
        description = "{4}{G}{G}, {T}: Create an X/X green Elemental creature token, where X is the " +
            "number of lands you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49999b95-5e62-414c-b975-4191b9c1ab39.jpg?1783944292"
    }
}

val BudokaGardener: CardDefinition = CardDefinition.flipCard(
    unflipped = BudokaGardenerUpright,
    flipped = DokaiWeaverOfLife,
)
