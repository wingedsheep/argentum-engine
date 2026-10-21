package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Karn, Legacy Reforged — March of the Machine: The Aftermath #49
 * {5} · Legendary Artifact Creature — Golem · * / *
 *
 * Karn's power and toughness are each equal to the greatest mana value among artifacts you control.
 * At the beginning of your upkeep, add {C} for each artifact you control. This mana can't be spent
 * to cast nonartifact spells. Until end of turn, you don't lose this mana as steps and phases end.
 *
 * Karn is itself an artifact, so on the battlefield it counts itself. The upkeep mana is restricted
 * with the Powerstone shape ([ManaRestriction.CannotCastSpellsOtherThan] artifact — abilities and
 * other costs stay payable) and kept across step/phase ends with [ManaExpiry.KEPT_UNTIL_END_OF_TURN].
 * The artifact count is read on resolution.
 */
val KarnLegacyReforged = card("Karn, Legacy Reforged") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Creature — Golem"
    oracleText = "Karn's power and toughness are each equal to the greatest mana value among artifacts " +
        "you control.\n" +
        "At the beginning of your upkeep, add {C} for each artifact you control. This mana can't be " +
        "spent to cast nonartifact spells. Until end of turn, you don't lose this mana as steps and " +
        "phases end."

    dynamicStats(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).maxManaValue())

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddColorlessMana(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            restriction = ManaRestriction.CannotCastSpellsOtherThan(setOf(CardType.ARTIFACT)),
            expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "49"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4219b5ea-a252-4d76-a60a-9674340e8ed3.jpg?1783916509"
    }
}
