package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Join Shields — Guilds of Ravnica #181 (canonical printing)
 * {3}{G}{W} · Instant
 *
 * Untap all creatures you control. They gain hexproof and indestructible until end of turn.
 *
 * "They" is every creature you control as the spell resolves — including ones that were already
 * untapped (see ruling) — so one [Effects.ForEachInGroup] pass untaps each and grants both keywords.
 */
val JoinShields = card("Join Shields") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Instant"
    oracleText = "Untap all creatures you control. They gain hexproof and indestructible until end of turn. " +
        "(They can't be the targets of spells or abilities your opponents control. Damage and effects that " +
        "say \"destroy\" don't destroy them.)"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Untap(EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Winona Nelson"
        flavorText = "\"We are the shield that never breaks, the bough that never burns, the song that can never be silenced.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91898d8a-589d-43db-8dc4-0ec6a3bd6d49.jpg?1783934136"
        ruling(
            "2018-10-05",
            "Untapped creatures you control can't be untapped again, but those creatures still gain hexproof and indestructible."
        )
    }
}
