package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Piper of the Swarm
 * {1}{B}
 * Creature — Human Warlock
 * 1/3
 * Rats you control have menace.
 * {1}{B}, {T}: Create a 1/1 black Rat creature token.
 * {2}{B}{B}, {T}, Sacrifice three Rats: Gain control of target creature.
 *
 * Canonical printing: Throne of Eldraine (ELD #100). Reprinted in AFC and SLD.
 * "Rats" means Rat permanents; the control change is indefinite (Duration.Permanent).
 */
val PiperOfTheSwarm = card("Piper of the Swarm") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 1
    toughness = 3
    oracleText = "Rats you control have menace.\n" +
        "{1}{B}, {T}: Create a 1/1 black Rat creature token.\n" +
        "{2}{B}{B}, {T}, Sacrifice three Rats: Gain control of target creature."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.MENACE,
            filter = GroupFilter(GameObjectFilter.Permanent.youControl().withSubtype(Subtype.RAT)),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Rat"),
            imageUri = "https://cards.scryfall.io/normal/front/e/4/e43a205e-43ea-4b3e-92ab-c2ee2172a50a.jpg?1783932482",
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}{B}"),
            Costs.Tap,
            Costs.SacrificeMultiple(3, GameObjectFilter.Permanent.withSubtype(Subtype.RAT)),
        )
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "100"
        artist = "Irina Nordsol"
        flavorText = "\"If it has ears, it will dance to my tune.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a7962fe-b715-4981-86c3-223bad9b1899.jpg?1783932636"
        ruling(
            "2019-10-04",
            "The control-change effect of Piper of the Swarm's last ability lasts indefinitely. It doesn't wear " +
                "off during the cleanup step, and it doesn't expire if Piper of the Swarm leaves the battlefield. " +
                "In a multiplayer game, it does expire if you leave the game."
        )
    }
}
