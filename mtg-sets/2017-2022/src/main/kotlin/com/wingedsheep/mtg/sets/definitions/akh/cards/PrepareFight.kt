package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Prepare // Fight — Amonkhet #220
 *
 * Prepare {1}{W} — Instant: Untap target creature. It gets +2/+2 and gains lifelink until end of turn.
 * Fight {3}{G} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Target creature you control fights target creature an opponent controls.
 */
val PrepareFight = card("Prepare // Fight") {
    layout = CardLayout.SPLIT
    colorIdentity = "GW"

    face("Prepare") {
        manaCost = "{1}{W}"
        typeLine = "Instant"
        oracleText = "Untap target creature. It gets +2/+2 and gains lifelink until end of turn."

        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Untap(t) then
                Effects.ModifyStats(2, 2, t) then
                Effects.GrantKeyword(Keyword.LIFELINK, t)
        }
    }

    face("Fight") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Target creature you control fights target creature an opponent controls."
        keywords(Keyword.AFTERMATH)

        spell {
            val mine = target(TargetFilter.CreatureYouControl)
            val theirs = target(TargetFilter.CreatureOpponentControls)
            effect = Effects.Fight(mine, theirs)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f59ea6f6-2dff-4e58-9166-57cac03f1d0a.jpg?1783936453"
        ruling("2017-04-18", "Untapping an attacking creature doesn't remove it from combat.")
        ruling("2017-04-18", "If either or both targets are illegal when Fight tries to resolve, no creature will deal or be dealt damage.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from any zone other than a graveyard, you can't cast the half with aftermath.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from a graveyard, you may cast either half. If you cast the half that has aftermath, you'll exile the card if it would leave the stack.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
