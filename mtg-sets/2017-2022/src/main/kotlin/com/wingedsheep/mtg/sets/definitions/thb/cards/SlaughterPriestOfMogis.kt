package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slaughter-Priest of Mogis
 * {B}{R}
 * Creature — Minotaur Shaman
 * 2/2
 * Whenever you sacrifice a permanent, this creature gets +2/+0 until end of turn.
 * {2}, Sacrifice another creature or an enchantment: This creature gains first strike until end of
 * turn.
 *
 * The pump is [Triggers.you] `sacrifices()` in its per-permanent form — one trigger for each
 * permanent sacrificed, not one per batch. The Priest isn't an enchantment, so the activated cost
 * is [Costs.SacrificeAnother] over creature-or-enchantment.
 */
val SlaughterPriestOfMogis = card("Slaughter-Priest of Mogis") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Minotaur Shaman"
    power = 2
    toughness = 2
    oracleText = "Whenever you sacrifice a permanent, this creature gets +2/+0 until end of turn.\n" +
        "{2}, Sacrifice another creature or an enchantment: This creature gains first strike until end of turn."

    triggeredAbility {
        trigger = Triggers.you.sacrifices()
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "Whenever you sacrifice a permanent, this creature gets +2/+0 until end of turn."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature.or(GameObjectFilter.Enchantment)),
        )
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Steve Prescott"
        flavorText = "\"Mogis is pleased by this offering. Bring him more!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2b441c0-261e-4094-b694-5e11ea3c14ab.jpg?1783931518"
        ruling(
            "2020-01-24",
            "The first ability of Slaughter-Priest of Mogis is a triggered ability, not an activated " +
                "ability. It doesn't allow you to sacrifice a permanent whenever you want; rather, you need " +
                "some other way of sacrificing permanents, such as its second ability."
        )
    }
}
