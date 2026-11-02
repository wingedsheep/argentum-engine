package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider

/**
 * Guildmages' Forum — Guilds of Ravnica #250
 * Land
 * {T}: Add {C}.
 * {1}, {T}: Add one mana of any color. If that mana is spent on a multicolored creature spell, that
 * creature enters with an additional +1/+1 counter on it.
 *
 * The rider isn't a triggered ability ("If that mana is spent …"), so the counter is owed to the
 * spell at payment time and placed as it enters — nothing goes on the stack.
 */
val GuildmagesForum = card("Guildmages' Forum") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}: Add one mana of any color. If that mana is spent on a multicolored creature spell, " +
        "that creature enters with an additional +1/+1 counter on it."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddManaOfChoice(
            riders = setOf(
                ManaSpellRider.EntersWithCountersWhenSpent(
                    spellFilter = GameObjectFilter.Creature and GameObjectFilter.Multicolored,
                )
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "250"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cba12bda-d460-4206-8469-4357c967b9b8.jpg?1783934102"
    }
}
