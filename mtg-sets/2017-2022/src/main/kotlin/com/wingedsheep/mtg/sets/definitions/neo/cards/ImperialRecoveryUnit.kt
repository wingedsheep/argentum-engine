package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Imperial Recovery Unit — Kamigawa: Neon Dynasty #18 (canonical printing)
 * {2}{W} · Artifact — Vehicle · 3/4
 *
 * Whenever this Vehicle attacks, return target creature or Vehicle card with mana value 2 or
 * less from your graveyard to your hand.
 * Crew 2
 *
 * "Creature or Vehicle card" is [GameObjectFilter.CreatureOrVehicle] (a Vehicle is matched by its
 * subtype); the mana-value cap applies to both branches. The target is mandatory, so the trigger
 * is simply removed from the stack when no such card is in your graveyard.
 */
val ImperialRecoveryUnit = card("Imperial Recovery Unit") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 4
    oracleText = "Whenever this Vehicle attacks, return target creature or Vehicle card with mana " +
        "value 2 or less from your graveyard to your hand.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This " +
        "Vehicle becomes an artifact creature until end of turn.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(
            TargetFilter(
                GameObjectFilter.CreatureOrVehicle.manaValueAtMost(2).ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = Effects.ReturnToHand(t)
        description = "Whenever this Vehicle attacks, return target creature or Vehicle card with " +
            "mana value 2 or less from your graveyard to your hand."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac9b2c7b-c93b-4b97-999e-86faed8a26ee.jpg?1783923921"
    }
}
