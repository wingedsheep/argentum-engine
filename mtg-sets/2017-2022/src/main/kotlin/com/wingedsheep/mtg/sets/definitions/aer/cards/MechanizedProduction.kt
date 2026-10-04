package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mechanized Production
 * {2}{U}{U}
 * Enchantment — Aura
 *
 * Enchant artifact you control
 * At the beginning of your upkeep, create a token that's a copy of enchanted artifact. Then if
 * you control eight or more artifacts with the same name as one another, you win the game.
 *
 * The copy reads [EffectTarget.EnchantedPermanent] at resolution. The win check is a plain "then
 * if", not an intervening if, so it runs after the token exists and counts it. "With the same
 * name as one another" is `largestSameNameGroup()` over artifacts you control — the eight need
 * not share the enchanted artifact's name, and a nonartifact of that name doesn't count (rulings
 * of 2017-02-09).
 */
val MechanizedProduction = card("Mechanized Production") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact you control\n" +
        "At the beginning of your upkeep, create a token that's a copy of enchanted artifact. " +
        "Then if you control eight or more artifacts with the same name as one another, you win the game."

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Artifact.youControl()))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.CreateTokenCopyOfTarget(EffectTarget.EnchantedPermanent) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).largestSameNameGroup(),
                    ComparisonOperator.GTE,
                    8
                ),
                then = Effects.WinGame(
                    message = "Mechanized Production: controlled eight or more artifacts with the same name."
                )
            )
        description = "At the beginning of your upkeep, create a token that's a copy of enchanted artifact. " +
            "Then if you control eight or more artifacts with the same name as one another, you win the game."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "38"
        artist = "Adam Paquette"
        flavorText = "\"Give me eight walkers, I'll give you the city.\"\n—Dovin Baan"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/235dd8f1-215a-4b0a-9e94-0d0d5a3c730b.jpg?1783936771"
        ruling(
            "2017-02-09",
            "The eight artifacts with the same name don't have to have the same name as the enchanted " +
                "artifact. For example, you win the game if you control eight Thopter artifact creature " +
                "tokens as Mechanized Production's ability resolves, even if Mechanized Production isn't " +
                "attached to a Thopter."
        )
        ruling(
            "2017-02-09",
            "All eight of the permanents sharing a name must be artifacts. If you control only seven " +
                "artifacts with the same name and a nonartifact permanent with that same name, you won't " +
                "win the game."
        )
        ruling(
            "2017-02-09",
            "If you control eight or more artifacts that share a name while you control Mechanized " +
                "Production, you won't win the game yet. You'll win the game while resolving its " +
                "triggered ability during your upkeep."
        )
        ruling(
            "2017-02-09",
            "If the enchanted artifact leaves the battlefield in response to Mechanized Production's " +
                "triggered ability but Mechanized Production does not, Mechanized Production is put into " +
                "its owner's graveyard as a state-based action with no enchanted artifact. The triggered " +
                "ability creates no token, but you can still win the game if you control enough " +
                "artifacts with the same name."
        )
    }
}
