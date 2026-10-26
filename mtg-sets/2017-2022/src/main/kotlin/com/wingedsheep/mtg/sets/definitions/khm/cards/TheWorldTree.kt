package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The World Tree — Kaldheim #275
 * Land · Rare
 *
 * The middle line is Chromatic Lantern's land grant gated by a land count
 * ([Conditions.ControlLandsAtLeast]), so it switches on and off with the board; the auto-payer
 * sees the grant only while the condition holds (`ManaStaticsIndex`). The search is unbounded,
 * so it is Nissa, Who Shakes the World's inline any-number pipeline rather than
 * `Patterns.Library.searchLibrary`, which only offers "up to N".
 */
val TheWorldTree = card("The World Tree") {
    manaCost = ""
    colorIdentity = "WUBRG"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {G}.\n" +
        "As long as you control six or more lands, lands you control have \"{T}: Add one mana of any color.\"\n" +
        "{W}{W}{U}{U}{B}{B}{R}{R}{G}{G}, {T}, Sacrifice this land: Search your library for any number of " +
        "God cards, put them onto the battlefield, then shuffle."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    staticAbility {
        condition = Conditions.ControlLandsAtLeast(6)
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            filter = GroupFilter(GameObjectFilter.Land.youControl())
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}{W}{U}{U}{B}{B}{R}{R}{G}{G}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline(
            descriptionOverride = "Search your library for any number of God cards, put them onto the " +
                "battlefield, then shuffle."
        ) {
            val gods = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any.withSubtype(Subtype.GOD)),
                search = true
            )
            val found = chooseAnyNumber(
                from = gods,
                prompt = "Search your library for any number of God cards",
            )
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, player = Player.You))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
        description = "{W}{W}{U}{U}{B}{B}{R}{R}{G}{G}, {T}, Sacrifice this land: Search your library for " +
            "any number of God cards, put them onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "275"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a70cb6d9-3955-4064-917b-11dec26440c5.jpg?1783928169"
    }
}
