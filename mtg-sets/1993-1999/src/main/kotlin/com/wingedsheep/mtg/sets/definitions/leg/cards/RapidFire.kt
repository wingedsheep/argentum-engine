package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rapid Fire
 * {3}{W}
 * Instant
 *
 * Cast this spell only before blockers are declared.
 * Target creature gains first strike until end of turn. If it doesn't have rampage, that creature
 * gains rampage 2 until end of turn.
 *
 * "Before blockers are declared" is the turn's first combat (the ruling), so the timing line is
 * [Conditions.BeforeBlockersDeclared]. The rampage check reads the projected keyword on
 * resolution, so a printed or an earlier granted rampage both count.
 */
val RapidFire = card("Rapid Fire") {
    manaCost = "{3}{W}"
    typeLine = "Instant"
    oracleText = "Cast this spell only before blockers are declared.\n" +
        "Target creature gains first strike until end of turn. If it doesn't have rampage, that creature " +
        "gains rampage 2 until end of turn. (Whenever the creature becomes blocked, it gets +2/+2 until " +
        "end of turn for each creature blocking it beyond the first.)"
    spell {
        castOnlyIf(Conditions.BeforeBlockersDeclared)
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature) then
            Effects.If(
                Conditions.Not(Conditions.EntityMatches(creature, GameObjectFilter.Any.withKeyword(Keyword.RAMPAGE))),
                Effects.GrantRampage(2, creature),
            )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Justin Hampton"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e26e7c9c-e6de-47f4-8394-7e853408f84c.jpg?1783948082"
        ruling("2013-09-20", "If a turn has multiple combat phases, Rapid Fire can only be cast before the beginning of the Declare Blockers Step of the first combat phase in that turn.")
    }
}
