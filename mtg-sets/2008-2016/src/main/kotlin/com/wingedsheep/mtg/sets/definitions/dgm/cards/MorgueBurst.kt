package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Morgue Burst — Dragon's Maze #86
 * {4}{B}{R} · Sorcery · Common
 *
 * Return target creature card from your graveyard to your hand. Morgue Burst deals damage to any
 * target equal to the power of the card returned this way.
 *
 * The power is the card's as it last existed in the graveyard (ruling 2013-04-15), so it is frozen
 * with [Effects.StoreNumber] before the return — a characteristic-defining power that counts
 * graveyard cards would otherwise be re-read from hand. If the creature card is an illegal target
 * on resolution, its target slot is dropped (CR 608.2b): nothing returns, the stored power reads 0,
 * and no damage is dealt — the second ruling. An illegal damage target still lets the card return.
 */
val MorgueBurst = card("Morgue Burst") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to your hand. Morgue Burst deals " +
        "damage to any target equal to the power of the card returned this way."

    spell {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        val damageTarget = target(Targets.Any)
        effect = Effects.StoreNumber("morgue_burst_power", DynamicAmounts.powerOf(creatureCard)) then
            Effects.ReturnToHand(creatureCard) then
            Effects.DealDamage(
                DynamicAmounts.storedNumber("morgue_burst_power"),
                damageTarget,
                damageSource = EffectTarget.Self
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Raymond Swanland"
        flavorText = "\"Let him in. He's on the list.\"\n—Olrich, Rakdos club owner"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b3c2909-87ab-4027-9b56-58a2abae3fa3.jpg?1783940025"
        ruling(
            "2013-04-15",
            "Use the power of the card as it last existed in your graveyard to determine how much " +
                "damage will be dealt."
        )
        ruling(
            "2013-04-15",
            "If the creature card is an illegal target when Morgue Burst tries to resolve (most likely " +
                "because it was exiled in response), it won't be returned to your hand and no damage " +
                "will be dealt to the target."
        )
    }
}
