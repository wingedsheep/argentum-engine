package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Death's Oasis
 * {W}{B}{G}
 * Enchantment
 *
 * Whenever a nontoken creature you control dies, mill two cards. Then return a creature card with
 * lesser mana value than the creature that died from your graveyard to your hand.
 * {1}, Sacrifice this enchantment: You gain life equal to the greatest mana value among creatures
 * you control.
 *
 * The return is not targeted, so it is gathered after the mill and the milled cards are eligible
 * (2020-04-17 ruling). It is mandatory: `chooseExactly(1)` over the eligible pool, which does
 * nothing when the pool is empty. The cap reads the dead creature's mana value through
 * `triggeringManaValue()`; the dead creature is off the battlefield, so the evaluator reads its
 * battlefield-exit snapshot — the mana value it had as it last existed on the battlefield, which
 * is what lets a copy of a more expensive creature return itself.
 */
val DeathsOasis = card("Death's Oasis") {
    manaCost = "{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Enchantment"
    oracleText = "Whenever a nontoken creature you control dies, mill two cards. Then return a " +
        "creature card with lesser mana value than the creature that died from your graveyard to " +
        "your hand.\n{1}, Sacrifice this enchantment: You gain life equal to the greatest mana " +
        "value among creatures you control."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().youControl()).dies()
        effect = Effects.Pipeline {
            run(Patterns.Library.mill(2))
            val eligible = gather(
                CardSource.FromZone(
                    Zone.GRAVEYARD,
                    Player.You,
                    GameObjectFilter.Creature.manaValueAtMostDynamic(DynamicAmounts.triggeringManaValue() - 1)
                )
            )
            val chosen = chooseExactly(
                1,
                from = eligible,
                prompt = "Return a creature card with lesser mana value than the creature that died to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxManaValue()
        )
        description = "{1}, Sacrifice this enchantment: You gain life equal to the greatest mana " +
            "value among creatures you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/112e2cca-bae7-4296-9514-e6058f4b38a5.jpg?1783931026"

        ruling("2020-04-17", "Because the triggered ability doesn't target the creature card in your graveyard, you may choose one of the two cards you put there from your library.")
        ruling("2020-04-17", "Use the mana value of the creature as it last existed on the battlefield and the mana value of the creature card as it exists in your graveyard to determine which cards may be returned to your hand. This may result in a creature's death bringing itself back to your hand; perhaps it was one card of a merged creature, or it had become a copy of a more expensive creature.")
    }
}
