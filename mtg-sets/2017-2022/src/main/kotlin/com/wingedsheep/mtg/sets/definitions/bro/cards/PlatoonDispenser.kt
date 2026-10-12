package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Platoon Dispenser — The Brothers' War #36
 * {5} · Artifact Creature — Construct · Mythic
 * 4/6
 *
 * At the beginning of your end step, if you control two or more other creatures, draw a card.
 * {3}{W}: Create a 1/1 colorless Soldier artifact creature token.
 * Unearth {2}{W}{W}
 *
 * The end-step draw is an intervening "if" — checked when it triggers and again on resolution —
 * and "other" leaves Platoon Dispenser itself out of the count ([Conditions.YouControlOtherAtLeast]).
 */
val PlatoonDispenser = card("Platoon Dispenser") {
    manaCost = "{5}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 6
    oracleText = "At the beginning of your end step, if you control two or more other creatures, draw a card.\n" +
        "{3}{W}: Create a 1/1 colorless Soldier artifact creature token.\n" +
        "Unearth {2}{W}{W} ({2}{W}{W}: Return this card from your graveyard to the battlefield. It gains haste. " +
        "Exile it at the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouControlOtherAtLeast(2, GameObjectFilter.Creature)
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Soldier"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/3/7/37da5b54-ec55-46e3-9f0b-565cbbe1ac7a.jpg?1783919907",
        )
        description = "{3}{W}: Create a 1/1 colorless Soldier artifact creature token."
    }

    unearth("{2}{W}{W}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "36"
        artist = "Leon Tukker"
        flavorText = "\"Hear that thud-clank, thud-clank? We're saved.\"\n—Ani, Urzan soldier"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c4825e3-c549-4bcb-84b5-2d0782c5c4e5.jpg?1783920118"
        ruling("2022-10-14", "You must control two or more other creatures both when the first ability triggers and when it resolves. If you control fewer than two other creatures at either time, you won't draw a card.")
        ruling("2022-10-14", "If a permanent returned to the battlefield with unearth would leave the battlefield for any reason, it's exiled instead—unless the spell or ability that's causing the permanent to leave the battlefield is actually trying to exile it!")
    }
}
