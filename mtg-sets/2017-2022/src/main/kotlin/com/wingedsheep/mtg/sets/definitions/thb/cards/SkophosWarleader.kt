package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skophos Warleader
 * {4}{R}
 * Creature — Minotaur Warrior
 * 4/5
 * {R}, Sacrifice another creature or an enchantment: This creature gets +1/+0 and gains menace until
 * end of turn.
 *
 * The Warleader isn't an enchantment, so "another creature or an enchantment" is exactly
 * [Costs.SacrificeAnother] over creature-or-enchantment.
 */
val SkophosWarleader = card("Skophos Warleader") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Warrior"
    power = 4
    toughness = 5
    oracleText = "{R}, Sacrifice another creature or an enchantment: This creature gets +1/+0 and gains " +
        "menace until end of turn. (It can't be blocked except by two or more creatures.)"

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{R}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature.or(GameObjectFilter.Enchantment)),
        )
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Svetlin Velinov"
        flavorText = "The minotaur polis of Skophos boasts grand temples to the god of slaughter."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59c39c70-8360-4c7c-b5fa-61c42828609a.jpg?1783931544"
    }
}
