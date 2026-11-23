package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressHexproofForGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glaring Spotlight — Gatecrash #229 (canonical printing)
 * {1} Artifact
 *
 * Creatures your opponents control with hexproof can be the targets of spells and abilities you
 * control as though they didn't have hexproof.
 * {3}, Sacrifice this artifact: Creatures you control gain hexproof until end of turn and can't be
 * blocked this turn.
 *
 * The hexproof grant changes characteristics, so it is locked to the creatures you control as the
 * ability resolves (ForEachInGroup); the "can't be blocked" restriction is a floating group effect
 * that also covers creatures that come under your control later in the turn — exactly the split
 * the 2013-01-24 ruling describes.
 */
val GlaringSpotlight = card("Glaring Spotlight") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Creatures your opponents control with hexproof can be the targets of spells and abilities " +
        "you control as though they didn't have hexproof.\n" +
        "{3}, Sacrifice this artifact: Creatures you control gain hexproof until end of turn and can't be " +
        "blocked this turn."

    staticAbility {
        ability = SuppressHexproofForGroup(filter = GroupFilter.AllCreaturesOpponentsControl)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.SacrificeSelf)
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.IterationEntity)
        ) then Effects.CantBeBlockedGroup(GroupFilter.AllCreaturesYouControl)
        description = "{3}, Sacrifice this artifact: Creatures you control gain hexproof until end of turn " +
            "and can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6070239-54a7-49d4-bd3c-d5c4cda971db.jpg?1783940092"
        ruling("2013-01-24", "Creatures your opponents control don't actually lose hexproof, although you will ignore hexproof for purposes of choosing targets of spells and abilities you control.")
        ruling("2013-01-24", "Creatures that come under your control after Glaring Spotlight's last ability resolves won't have hexproof but they can't be blocked that turn.")
    }
}
