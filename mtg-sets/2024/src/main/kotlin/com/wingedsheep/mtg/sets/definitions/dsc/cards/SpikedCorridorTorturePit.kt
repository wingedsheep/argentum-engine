package com.wingedsheep.mtg.sets.definitions.dsc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Spiked Corridor // Torture Pit (DSC 28) — split-layout Room (CR 709.5).
 *
 * Spiked Corridor {3}{R} — When you unlock this door, create three 1/1 red Devil creature tokens
 * with "When this token dies, it deals 1 damage to any target."
 *
 * Torture Pit {3}{R} — If a source you control would deal noncombat damage to an opponent, it deals
 * that much damage plus 2 instead.
 *
 * Torture Pit's replacement is printed on a door, so it exists only while that door is unlocked:
 * the engine folds unlocked-face replacement effects into the Room's baked replacement component
 * (RoomFaceStatics) and re-bakes it when a door locks or unlocks. The extra 2 is dealt by the
 * original source, through the same [ModifyDamageAmount] path as Mechanized Warfare.
 */
val SpikedCorridorTorturePit = card("Spiked Corridor // Torture Pit") {
    layout = CardLayout.SPLIT
    colorIdentity = "R"

    face("Spiked Corridor") {
        manaCost = "{3}{R}"
        typeLine = "Enchantment — Room"
        oracleText = "When you unlock this door, create three 1/1 red Devil creature tokens with " +
            "\"When this token dies, it deals 1 damage to any target.\""

        triggeredAbility {
            trigger = Triggers.self.doorUnlocked()
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Devil"),
                count = 3,
                triggeredAbilities = listOf(
                    grantedTriggeredAbility {
                        trigger = Triggers.self.dies()
                        val any = target(Targets.Any)
                        effect = Effects.DealDamage(1, any)
                        description = "When this token dies, it deals 1 damage to any target."
                    }
                ),
            )
            description = "When you unlock this door, create three 1/1 red Devil creature tokens with " +
                "\"When this token dies, it deals 1 damage to any target.\""
        }
    }

    face("Torture Pit") {
        manaCost = "{3}{R}"
        typeLine = "Enchantment — Room"
        oracleText = "If a source you control would deal noncombat damage to an opponent, it deals " +
            "that much damage plus 2 instead."

        replacementEffect(
            ModifyDamageAmount(
                modifier = 2,
                appliesTo = EventPattern.DamageEvent(
                    source = GameObjectFilter.Any.youControl(),
                    recipient = Recipient.Opponent,
                    damageType = DamageType.NonCombat,
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Arthur Yuan"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b90164a1-8b0a-4445-a0ad-43e4c3324925.jpg?1783909656"
        ruling("2024-09-20", "The additional 2 damage is dealt by the same source as the original source of damage. The additional damage isn't dealt by Torture Pit unless Torture Pit is the original source of damage.")
        ruling("2024-09-20", "If another effect modifies how much noncombat damage the source would deal, including preventing some of it, the player being dealt damage chooses an order in which to apply those effects. If all of the damage is prevented, Torture Pit's effect no longer applies.")
        ruling("2024-09-20", "If noncombat damage dealt by a source you control is being divided or assigned among multiple players, divide the original amount before adding 2.")
    }
}
