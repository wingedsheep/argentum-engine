package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Endless Sands — Hour of Devastation #176 (canonical printing)
 * Land — Desert
 * {T}: Add {C}.
 * {2}, {T}: Exile target creature you control.
 * {4}, {T}, Sacrifice this land: Return each creature card exiled with this land to the
 * battlefield under its owner's control.
 *
 * "Exiled with this land" is a linked-exile pile (CR 607): `ExileLinkedToSource` records each
 * creature against this land, and the payoff gathers exactly that pile with
 * `CardSource.FromLinkedExile`. The pile is read off the source even though the land was
 * sacrificed as part of the cost (the same shape as Connecting the Dots).
 *
 * "Each *creature card*": the move is filtered to creature cards as they sit in exile, so a token
 * (which ceases to exist) or an animated land (a land card in exile) stays where it is — the
 * 2017-07-14 ruling. "Under its owner's control", so a stolen creature you exiled goes home.
 */
val EndlessSands = card("Endless Sands") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Desert"
    oracleText = "{T}: Add {C}.\n{2}, {T}: Exile target creature you control.\n" +
        "{4}, {T}, Sacrifice this land: Return each creature card exiled with this land to the " +
        "battlefield under its owner's control."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        val creatureYouControl = target(TargetFilter.CreatureYouControl)
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.ExileLinkedToSource(creatureYouControl)
        description = "{2}, {T}: Exile target creature you control."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            move(
                exiled,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                underOwnersControl = true,
                filter = GameObjectFilter.Creature,
            )
        }
        description = "{4}, {T}, Sacrifice this land: Return each creature card exiled with this " +
            "land to the battlefield under its owner's control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ebd3e0a-30cc-4bc1-b9fa-6d65db9a6f5b.jpg?1783935997"
        ruling(
            "2017-07-14",
            "The last ability of Endless Sands returns only creature cards exiled by that card. If " +
                "Endless Sands leaves the battlefield before you activate that ability, the exiled " +
                "creatures are lost in the dunes forever."
        )
        ruling(
            "2017-07-14",
            "If you exile a creature that isn't a creature card with the second ability of Endless " +
                "Sands (such as a token creature or a land that has become a creature), the third " +
                "ability won't return that token or card from exile."
        )
        ruling(
            "2017-04-18",
            "Desert is a land subtype with no special meaning. It doesn't grant the land an " +
                "intrinsic mana ability. Other cards may care about which lands are Deserts."
        )
    }
}
