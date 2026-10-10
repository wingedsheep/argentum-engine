package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Druid Class {1}{G}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * Landfall — Whenever a land you control enters, you gain 1 life.
 *
 * {2}{G}: Level 2
 * You may play an additional land on each of your turns.
 *
 * {4}{G}: Level 3
 * When this Class becomes level 3, target land you control becomes a creature with haste and
 * "This creature's power and toughness are each equal to the number of lands you control."
 * It's still a land.
 *
 * Level 3 is `BecomeCreature` with a permanent duration and dynamic P/T recomputed at projection
 * (the Beorn's Hospitality shape): it adds CREATURE while keeping the land's types, so it's still
 * a land, and the effect outlives the Class itself.
 */
val DruidClass = card("Druid Class") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "Landfall — Whenever a land you control enters, you gain 1 life.\n" +
        "{2}{G}: Level 2 — You may play an additional land on each of your turns.\n" +
        "{4}{G}: Level 3 — When this Class becomes level 3, target land you control becomes a " +
        "creature with haste and \"This creature's power and toughness are each equal to the " +
        "number of lands you control.\" It's still a land."

    // Level 1: Landfall — gain 1 life.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.GainLife(1)
        description = "Landfall — Whenever a land you control enters, you gain 1 life."
    }

    // Level 2: an additional land drop each of your turns.
    classLevel(2, "{2}{G}") {
        staticAbility {
            ability = GrantAdditionalLandDrop(count = 1)
        }
    }

    // Level 3: when this Class becomes level 3, animate target land you control permanently.
    classLevel(3, "{4}{G}") {
        triggeredAbility {
            trigger = Triggers.self.enters()
            val land = target(TargetFilter.Land.youControl())
            effect = Effects.BecomeCreature(
                target = land,
                power = DynamicAmounts.fixed(0),
                toughness = DynamicAmounts.fixed(0),
                keywords = setOf(Keyword.HASTE),
                duration = Duration.Permanent,
                dynamicPower = DynamicAmounts.landsYouControl(),
                dynamicToughness = DynamicAmounts.landsYouControl()
            )
            description = "When this Class becomes level 3, target land you control becomes a " +
                "creature with haste and \"This creature's power and toughness are each equal to " +
                "the number of lands you control.\" It's still a land."
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09278e95-eaae-4cd4-a0d8-a2d15b0abb58.jpg?1783926464"
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2021-07-23", "Gaining a level won't remove abilities that a Class had at a previous level.")
        ruling("2021-07-23", "If you have more than one Druid Class at level 2 or higher, you can play that many additional lands per turn. For example, with two Druid Classes at level 2, you can play three lands during your turn.")
        ruling("2021-07-23", "For the third ability, the power and toughness of the creature will change as the number of lands you control changes.")
    }
}
