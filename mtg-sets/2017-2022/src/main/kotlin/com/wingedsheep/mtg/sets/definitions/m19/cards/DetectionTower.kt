package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.SuppressHexproofForGroup
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Detection Tower — Core Set 2019 #249 (canonical printing)
 * Land
 *
 * {T}: Add {C}.
 * {1}, {T}: Until end of turn, your opponents and creatures your opponents control with hexproof
 * can be the targets of spells and abilities you control as though they didn't have hexproof.
 *
 * The suppression is granted to the activating player, not to the land, so it lasts until end of
 * turn even if Detection Tower leaves the battlefield (CR 611.2a).
 */
val DetectionTower = card("Detection Tower") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n{1}, {T}: Until end of turn, your opponents and creatures your opponents " +
        "control with hexproof can be the targets of spells and abilities you control as though they " +
        "didn't have hexproof."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.GrantStaticAbility(
            SuppressHexproofForGroup(
                filter = GroupFilter.AllCreaturesOpponentsControl,
                players = Player.EachOpponent
            ),
            EffectTarget.Controller,
            Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Adam Paquette"
        flavorText = "Its light exposes enemies and lays secrets bare."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02f99756-d334-4dba-a375-ba3d91ecae62.jpg?1783934505"
        ruling("2018-07-13", "Your opponents and creatures your opponents control don't actually lose hexproof, although you will ignore hexproof for purposes of choosing targets of spells and abilities you control.")
        ruling("2018-07-13", "Creatures your opponents control with \"hexproof from [quality]\" abilities can be targeted as though they didn't have those abilities as well.")
        ruling("2018-07-13", "Because Detection Tower's last ability doesn't change the characteristics of any permanents, the set of creatures affected by it is constantly updated. Creatures that come under your opponents' control later in the turn can be targeted as though they didn't have hexproof.")
    }
}
