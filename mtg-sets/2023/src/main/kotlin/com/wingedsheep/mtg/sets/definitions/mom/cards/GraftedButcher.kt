package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grafted Butcher {1}{B}
 * Creature — Phyrexian Samurai
 * 2/2
 * When this creature enters, Phyrexians you control gain menace until end of turn.
 * Other Phyrexians you control get +1/+1.
 * {3}{B}, Sacrifice an artifact or creature: Return this card from your graveyard to the
 * battlefield. Activate only as a sorcery.
 *
 * The enters trigger locks in the set of Phyrexians at resolution (a one-shot grant per
 * permanent, CR 611.2c) — a Phyrexian entering later that turn doesn't gain menace. The
 * Butcher itself is a Phyrexian, so it gains menace too.
 */
val GraftedButcher = card("Grafted Butcher") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Samurai"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, Phyrexians you control gain menace until end of turn.\n" +
        "Other Phyrexians you control get +1/+1.\n" +
        "{3}{B}, Sacrifice an artifact or creature: Return this card from your graveyard to the " +
        "battlefield. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.grantKeywordToAll(
            Keyword.MENACE,
            GroupFilter(GameObjectFilter.Permanent.youControl().withSubtype("Phyrexian"))
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.youControl().withSubtype("Phyrexian"),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{B}"),
            Costs.Sacrifice(GameObjectFilter.Artifact or GameObjectFilter.Creature)
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        description = "{3}{B}, Sacrifice an artifact or creature: Return this card from your " +
            "graveyard to the battlefield. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e22f11a1-0cbd-4b36-8dbd-37ba29ad4608.jpg?1783917008"
    }
}
