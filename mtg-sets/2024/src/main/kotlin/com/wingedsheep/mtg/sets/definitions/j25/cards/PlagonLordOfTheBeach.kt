package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Plagon, Lord of the Beach
 * {2}{U}
 * Legendary Creature — Starfish Wizard
 * 0/3
 *
 * When Plagon enters, draw a card for each creature you control with toughness greater than its power.
 * {W/U}: Target creature you control assigns combat damage equal to its toughness rather than its
 * power this turn.
 *
 * The enters trigger counts at resolution (Plagon itself, a 0/3, counts if still on the battlefield).
 * The activated ability is the turn-scoped [AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS] grant
 * (as on Bill the Pony) — unconditional, and it changes only combat damage, never the creature's power.
 */
val PlagonLordOfTheBeach = card("Plagon, Lord of the Beach") {
    manaCost = "{2}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Starfish Wizard"
    power = 0
    toughness = 3
    oracleText = "When Plagon enters, draw a card for each creature you control with toughness greater " +
        "than its power.\n" +
        "{W/U}: Target creature you control assigns combat damage equal to its toughness rather than its " +
        "power this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.toughnessGreaterThanPower()).count()
        )
        description = "When Plagon enters, draw a card for each creature you control with toughness " +
            "greater than its power."
    }

    activatedAbility {
        cost = Costs.Mana("{W/U}")
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(
            AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS,
            creature,
            Duration.EndOfTurn
        )
        description = "Target creature you control assigns combat damage equal to its toughness rather " +
            "than its power this turn."
    }

    metadata {
        ruling(
            "2024-11-08",
            "Plagon's last ability doesn't actually change the target creature's power. It changes only the " +
                "amount of combat damage the creature assigns. All other rules and effects that check power " +
                "or toughness use the real values, even if they cause damage \"equal to a creature's power\" " +
                "to be dealt."
        )
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "GOSSAN"
        flavorText = "\"No more lurking in the shallows. We ride!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f8a6bfe-6033-4f6b-ab45-6b553f8b51a1.jpg?1783908858"
    }
}
