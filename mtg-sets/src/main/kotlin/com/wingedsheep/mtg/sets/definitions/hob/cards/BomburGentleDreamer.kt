package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.storied
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bombur, Gentle Dreamer
 * {2}{R}
 * Legendary Creature — Dwarf Bard
 * 5/3
 *
 * Storied.
 * Bombur doesn't untap during your untap step unless you have an enduring story.
 *
 * The drawback half is the *inverse* of the usual storied payoff: the restriction is printed and the
 * enduring story turns it **off**, so the gate is `Not(YouHaveEnduringStory)` rather than the bare
 * condition every other storied card uses. Since the designation is never lost once gained
 * (CR 702.195a), that means Bombur untaps normally from the moment the third artifact/legendary/Saga
 * lands and forever after — Bombur himself is legendary and counts toward his own threshold.
 *
 * "Doesn't untap during your untap step" is the narrow [AbilityFlag.DOESNT_UNTAP], not the stronger
 * `CANT_BECOME_UNTAPPED`: a Twiddle still untaps Bombur even while the restriction is live. Granting
 * it through [GrantKeyword] rather than the printed `flags(...)` is what makes it conditional at all
 * — `flags` bakes the restriction into the card definition where no condition can reach it, whereas
 * a static ability re-projects every time [Conditions.YouHaveEnduringStory] changes answer.
 */
val BomburGentleDreamer = card("Bombur, Gentle Dreamer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dwarf Bard"
    oracleText = "Storied (If you control three or more artifacts, legendaries, and/or Sagas, you " +
        "have an enduring story for the rest of the game.)\n" +
        "Bombur doesn't untap during your untap step unless you have an enduring story."
    power = 5
    toughness = 3

    storied()

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.source()),
            condition = Conditions.Not(Conditions.YouHaveEnduringStory)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Eric Deschamps"
        flavorText = "Bombur was always trying to recapture the beautiful dreams he had in the forest."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63c317e7-432c-4817-8db4-3670a1d84be3.jpg?1785496185"
    }
}
