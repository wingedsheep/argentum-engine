package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gaea's Liege
 * {3}{G}{G}{G}
 * Creature — Avatar
 * * / *
 * As long as Gaea's Liege isn't attacking, its power and toughness are each equal to the number of
 * Forests you control. As long as Gaea's Liege is attacking, its power and toughness are each equal
 * to the number of Forests defending player controls.
 * {T}: Target land becomes a Forest until this creature leaves the battlefield.
 *
 * Both sentences describe one characteristic-defining ability reading a different count, so they
 * fold into a single `Conditional` amount over [Conditions.SourceIsAttacking] (same shape as Angry
 * Mob) rather than two effects that could disagree. "Defending player" resolves off the Liege's own
 * attack, and "Forests" is the land subtype, so lands the ability turned into Forests count.
 *
 * "Becomes a Forest" overwrites the land's other land types (CR 305.7) — [Effects.SetLandType] —
 * and lasts [Duration.WhileSourceOnBattlefield].
 */
val GaeasLiege = card("Gaea's Liege") {
    manaCost = "{3}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Avatar"
    oracleText = "As long as Gaea's Liege isn't attacking, its power and toughness are each equal to the " +
        "number of Forests you control. As long as Gaea's Liege is attacking, its power and toughness are " +
        "each equal to the number of Forests defending player controls.\n" +
        "{T}: Target land becomes a Forest until this creature leaves the battlefield."

    dynamicStats(
        DynamicAmounts.conditional(
            condition = Conditions.SourceIsAttacking,
            ifTrue = DynamicAmounts
                .battlefield(Player.DefendingPlayer, GameObjectFilter.Land.withSubtype(Subtype.FOREST))
                .count(),
            ifFalse = DynamicAmounts
                .battlefield(Player.You, GameObjectFilter.Land.withSubtype(Subtype.FOREST))
                .count(),
        )
    )

    activatedAbility {
        val land = target(TargetFilter.Land)
        cost = Costs.Tap
        effect = Effects.SetLandType(
            landType = "Forest",
            target = land,
            duration = Duration.WhileSourceOnBattlefield("this creature"),
        )
        description = "{T}: Target land becomes a Forest until this creature leaves the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "196"
        artist = "Dameon Willich"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b15221-c8b0-4861-9f8b-8a65834ad499.jpg?1783948677"
        ruling("2006-10-15", "Will not add or remove the supertype snow to or from a land.")
        ruling("2006-09-25", "The activated ability doesn't affect whether the land is basic or not. It overwrites any other land types. Being a Forest gives the affected land the ability \"{T}: Add {G}.\"")
        ruling("2004-10-04", "When being declared as an attacker, use the \"not attacking\" power and toughness. It only changes after declaration is complete.")
    }
}
