package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ethereal Absolution — Ravnica Allegiance #170 (canonical printing)
 * {4}{W}{B} · Enchantment
 *
 * Creatures you control get +1/+1.
 * Creatures your opponents control get -1/-1.
 * {2}{W}{B}: Exile target card from an opponent's graveyard. If it was a creature card, you
 * create a 1/1 white and black Spirit creature token with flying.
 *
 * Two static P/T modifiers over the controller-relative creature groups, plus the
 * General's Enforcer / Scavenging Ooze shape: exile the target, then gate the token on the
 * exiled card's printed type.
 */
val EtherealAbsolution = card("Ethereal Absolution") {
    manaCost = "{4}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +1/+1.\n" +
        "Creatures your opponents control get -1/-1.\n" +
        "{2}{W}{B}: Exile target card from an opponent's graveyard. If it was a creature card, " +
        "you create a 1/1 white and black Spirit creature token with flying."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.AllCreaturesYouControl,
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = -1,
            toughnessBonus = -1,
            filter = GroupFilter.AllCreaturesOpponentsControl,
        )
    }

    activatedAbility {
        cost = Costs.Mana(ManaCost.parse("{2}{W}{B}"))
        val exiled = target(TargetFilter.CardInGraveyard.ownedByOpponent())
        effect = Effects.Exile(exiled) then
            Effects.If(
                condition = Conditions.TargetIsCreatureCard(0),
                then = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE, Color.BLACK),
                    creatureTypes = setOf("Spirit"),
                    keywords = setOf(Keyword.FLYING),
                    imageUri = "https://cards.scryfall.io/normal/front/4/5/45b3bdd7-b093-4bfd-8a9a-965e72bfefb3.jpg?1783933602",
                ),
            )
        description = "{2}{W}{B}: Exile target card from an opponent's graveyard. If it was a creature card, " +
            "you create a 1/1 white and black Spirit creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Eric Deschamps"
        flavorText = "\"Time to let the dead be dead.\"\n—Kaya, to Teysa"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0872d0ff-1060-44cc-9ed0-a6aa496440c8.jpg?1783933652"
    }
}
