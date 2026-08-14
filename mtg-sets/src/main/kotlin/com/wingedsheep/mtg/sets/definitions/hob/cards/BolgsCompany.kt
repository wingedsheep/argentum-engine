package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bolg's Company — The Hobbit #149
 * {B}{R} · Creature — Goblin Soldier · Rare
 * 2/2
 *
 * This creature has haste as long as you control another Goblin.
 * {T}, Sacrifice another Goblin: Add {B}{R}.
 *
 * Modeling notes:
 *  - The haste clause is a continuous static gated on a *different* Goblin (`excludeSelf`), so it
 *    goes dark the moment the other Goblin leaves — the Barrow Naughty / Magitek Infantry shape.
 *    Note the ordering trap this creates: sacrificing your only other Goblin to the mana ability
 *    turns haste off, which matters if this creature hasn't attacked yet.
 *  - "Add {B}{R}" is one black *and* one red, not a choice, so it is two `AddMana` effects rather
 *    than an `AddManaOfChoice`. No targets and no stack — a true mana ability (CR 605.1a).
 */
val BolgsCompany = card("Bolg's Company") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Goblin Soldier"
    power = 2
    toughness = 2
    oracleText = "This creature has haste as long as you control another Goblin.\n" +
        "{T}, Sacrifice another Goblin: Add {B}{R}."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE, GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN))
        )
        effect = Effects.Composite(
            Effects.AddMana(Color.BLACK),
            Effects.AddMana(Color.RED)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}, Sacrifice another Goblin: Add {B}{R}."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "149"
        artist = "Michele Giorgi"
        flavorText = "Tidings they had gathered in secret ways; and in all the mountains there was " +
            "a forging and an arming."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea3f5644-f7e3-40de-ada5-cea2e9113cfb.jpg?1785497179"
    }
}
