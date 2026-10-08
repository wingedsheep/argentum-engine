package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bitter Reunion
 * {1}{R}
 * Enchantment
 * When this enchantment enters, you may discard a card. If you do, draw two cards.
 * {1}, Sacrifice this enchantment: Creatures you control gain haste until end of turn.
 *
 * The ETB is [Effects.May] around [Effects.IfYouDo], so the draw is gated on a card actually being
 * discarded (Kickoff Celebrations' shape).
 */
val BitterReunion = card("Bitter Reunion") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you may discard a card. If you do, draw two cards.\n" +
        "{1}, Sacrifice this enchantment: Creatures you control gain haste until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            effect = Effects.IfYouDo(
                action = Patterns.Hand.discardCards(1),
                then = Effects.DrawCards(2)
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
        )
        description = "Creatures you control gain haste until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Jake Murray"
        flavorText = "The peace summit proved the grudge between Urza and Mishra could end only in blood."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/345a1c80-41d6-43b1-83ab-1aa56dd06b1b.jpg"
    }
}
