package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Geth, Thane of Contracts
 * {1}{B}{B}
 * Legendary Creature — Phyrexian Zombie
 * 3/4
 * Other creatures you control get -1/-1.
 * {1}{B}{B}, {T}: Return target creature card from your graveyard to the battlefield. It gains
 * "If this creature would leave the battlefield, exile it instead of putting it anywhere else."
 * Activate only as a sorcery.
 *
 * The granted replacement is [Effects.GrantExileOnLeave] on the returned card, as on Kheru Lich
 * Lord — it has no duration, so it lasts as long as that object stays on the battlefield.
 */
val GethThaneOfContracts = card("Geth, Thane of Contracts") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Phyrexian Zombie"
    power = 3
    toughness = 4
    oracleText = "Other creatures you control get -1/-1.\n" +
        "{1}{B}{B}, {T}: Return target creature card from your graveyard to the battlefield. It gains " +
        "\"If this creature would leave the battlefield, exile it instead of putting it anywhere else.\" " +
        "Activate only as a sorcery."

    staticAbility {
        ability = ModifyStats(
            powerBonus = -1,
            toughnessBonus = -1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)
        )
    }

    activatedAbility {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        cost = Costs.Composite(Costs.Mana("{1}{B}{B}"), Costs.Tap)
        effect = Effects.PutOntoBattlefieldFromGraveyard(creatureCard) then
            Effects.GrantExileOnLeave(creatureCard)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "95"
        artist = "Martin de Diego Sádaba"
        flavorText = "\"It seems someone forgot to read the fine print.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c26e18be-81a1-4645-866a-fae5c2fdf7c9.jpg?1783918044"
    }
}
