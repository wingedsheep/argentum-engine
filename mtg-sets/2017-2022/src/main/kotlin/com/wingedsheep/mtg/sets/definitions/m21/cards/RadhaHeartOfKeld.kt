package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Radha, Heart of Keld — Core Set 2021 #224
 * {1}{R}{G} · Legendary Creature — Elf Warrior · 3/3
 *
 * During your turn, Radha has first strike.
 * You may look at the top card of your library any time, and you may play lands from the top of
 * your library.
 * {4}{R}{G}: Radha gets +X/+X until end of turn, where X is the number of lands you control.
 *
 * First strike is the Leech Fanatic shape ([GrantKeyword] gated by [Conditions.IsYourTurn]). The
 * library clauses are Augur of Autumn's / Courser of Kruphix's pair: [LookAtTopOfLibrary] plus a
 * lands-only [PlayLandsAndCastFilteredFromTopOfLibrary]. X is counted as the pump resolves.
 */
val RadhaHeartOfKeld = card("Radha, Heart of Keld") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Elf Warrior"
    power = 3
    toughness = 3
    oracleText = "During your turn, Radha has first strike.\n" +
        "You may look at the top card of your library any time, and you may play lands from the top of your library.\n" +
        "{4}{R}{G}: Radha gets +X/+X until end of turn, where X is the number of lands you control."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    staticAbility { ability = LookAtTopOfLibrary() }

    staticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = null)
    }

    activatedAbility {
        val lands = DynamicAmounts.landsYouControl()
        cost = Costs.Mana("{4}{R}{G}")
        effect = Effects.ModifyStats(lands, lands, EffectTarget.Self)
        description = "{4}{R}{G}: Radha gets +X/+X until end of turn, where X is the number of lands you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bbd37b1-49cb-4295-9a1f-fb85368a8f12.jpg?1783930660"
    }
}
