package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Appeal // Authority — Hour of Devastation #152
 *
 * Appeal {G} — Sorcery: Until end of turn, target creature gains trample and gets +X/+X, where X is
 *   the number of creatures you control.
 * Authority {1}{W} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Tap up to two target creatures your opponents control. Creatures you control gain vigilance
 *   until end of turn.
 *
 * Appeal's X is counted once as the spell resolves and locked for the turn: the stat executor
 * evaluates the dynamic amount at resolution and records a fixed modifier. Authority's vigilance is
 * a one-shot grant to the creatures you control at resolution, and it still happens when no targets
 * are chosen.
 */
val AppealAuthority = card("Appeal // Authority") {
    layout = CardLayout.SPLIT
    colorIdentity = "GW"

    face("Appeal") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Until end of turn, target creature gains trample and gets +X/+X, where X is the number of creatures you control."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
                Effects.ModifyStats(
                    DynamicAmounts.creaturesYouControl(),
                    DynamicAmounts.creaturesYouControl(),
                    creature,
                )
        }
    }

    face("Authority") {
        manaCost = "{1}{W}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Tap up to two target creatures your opponents control. Creatures you control gain vigilance until end of turn."
        keywords(Keyword.AFTERMATH)

        spell {
            targets(TargetFilter.CreatureOpponentControls, count = 2, optional = true)
            effect = Effects.TapEachTarget() then
                Patterns.Group.grantKeywordToAll(Keyword.VIGILANCE, Filters.Group.creaturesYouControl)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c25b8ef-6331-49df-9457-b8b4e44da2c9.jpg?1783936006"
        ruling("2017-07-14", "The value of X is determined only as Appeal begins to resolve. It won't change later in the turn if the number of creatures you control changes.")
        ruling("2017-07-14", "You may cast Authority without choosing any target creatures. Creatures you control will still gain vigilance until end of turn. However, if you choose any targets and each of those targets become illegal before Authority resolves, the spell won't resolve and your creatures won't gain vigilance.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
