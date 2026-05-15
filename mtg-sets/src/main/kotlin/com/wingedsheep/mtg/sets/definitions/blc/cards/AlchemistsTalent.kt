package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Alchemist's Talent {3}{R}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 *
 * When this Class enters, create two tapped Treasure tokens.
 *
 * {1}{R}: Level 2
 * Treasures you control have "{T}, Sacrifice this artifact: Add two mana of any
 * one color."
 *
 * {4}{R}: Level 3
 * Whenever you cast a spell, if mana from a Treasure was spent to cast it, this
 * Class deals damage equal to that spell's mana value to each opponent.
 */
val AlchemistsTalent = card("Alchemist's Talent") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "When this Class enters, create two tapped Treasure tokens.\n" +
        "{1}{R}: Level 2\n" +
        "Treasures you control have \"{T}, Sacrifice this artifact: Add two mana of any one color.\"\n" +
        "{4}{R}: Level 3\n" +
        "Whenever you cast a spell, if mana from a Treasure was spent to cast it, this Class deals damage equal to that spell's mana value to each opponent."

    // Level 1: ETB — create two tapped Treasure tokens.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateTreasure(count = 2, tapped = true)
    }

    // Level 2: Treasures you control have "{T}, Sacrifice this artifact: Add two mana
    // of any one color." The granted activated ability is a mana ability (single
    // colored payment, no triggers), so engine cost-payment paths can pick it up
    // through the same Treasure-mana-tagging path as the token's own ability.
    classLevel(2, "{1}{R}") {
        staticAbility {
            ability = GrantActivatedAbility(
                ability = ActivatedAbility(
                    cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf),
                    effect = Effects.AddAnyColorMana(2),
                    isManaAbility = true
                ),
                filter = GroupFilter(
                    GameObjectFilter.Artifact.withSubtype(Subtype.TREASURE).youControl()
                )
            )
        }
    }

    // Level 3: "Whenever you cast a spell, if mana from a Treasure was spent to cast
    // it..." The engine flags each cast with `paidWithTreasureMana` whenever the
    // [ManaPoolComponent.treasureMana] counter is decremented during payment.
    classLevel(3, "{4}{R}") {
        triggeredAbility {
            trigger = Triggers.YouCastSpellPaidWithTreasureMana
            effect = Effects.DealDamage(
                DynamicAmounts.triggeringManaValue(),
                EffectTarget.PlayerRef(Player.EachOpponent)
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fb8dace-c604-40fc-bbd4-5add5deb041a.jpg?1739656564"
        ruling("2024-07-26", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2024-07-26", "You can't activate the first level ability of a Class unless that Class is level 1. You can't activate the second level ability of a Class unless that Class is level 2.")
        ruling("2024-07-26", "Gaining a level won't remove abilities that a Class had at a previous level.")
    }
}
