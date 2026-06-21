package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Savior of the Small
 * {3}{W}
 * Creature — Kor Survivor
 * 3/4
 *
 * Survival — At the beginning of your second main phase, if this creature is tapped, return
 * target creature card with mana value 3 or less from your graveyard to your hand.
 *
 * "Survival" is an ability word (no rules meaning) — modeled as a postcombat-main-phase trigger
 * ([Triggers.YourPostcombatMain]) with an intervening-if ([Conditions.SourceIsTapped], CR 603.4 —
 * checked both when it would trigger and on resolution). The target is a creature card with mana
 * value 3 or less in your graveyard, returned to your hand via [Effects.ReturnToHand].
 */
val SaviorOfTheSmall = card("Savior of the Small") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Survivor"
    power = 3
    toughness = 4
    oracleText = "Survival — At the beginning of your second main phase, if this creature is " +
        "tapped, return target creature card with mana value 3 or less from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.YourPostcombatMain
        triggerCondition = Conditions.SourceIsTapped
        val card = target(
            "target creature card with mana value 3 or less from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.Creature
                        .ownedByYou()
                        .manaValueAtMost(3),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.ReturnToHand(card)
        description = "Survival — At the beginning of your second main phase, if this creature is " +
            "tapped, return target creature card with mana value 3 or less from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Elizabeth Peiró"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2ed31ea-c278-4e8c-afa7-6d09af399345.jpg?1726285957"
    }
}
