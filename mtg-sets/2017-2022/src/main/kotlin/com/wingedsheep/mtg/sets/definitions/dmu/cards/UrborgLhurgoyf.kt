package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Urborg Lhurgoyf
 * {1}{G}
 * Creature — Lhurgoyf
 * * / 1+*
 *
 * Kicker {U} and/or {B}
 * As this creature enters, mill three cards for each time it was kicked.
 * Urborg Lhurgoyf's power is equal to the number of creature cards in your graveyard and its
 * toughness is equal to that number plus 1.
 *
 * - "Kicker {U} and/or {B}" is two kicker abilities (CR 702.33b), declared in printed order; each may
 *   be paid at most once, so "times it was kicked" is 0, 1 or 2 — one per linked kicker slot paid.
 * - The mill is an as-enters replacement ([OnEnterRun]), not a trigger: nothing can happen between
 *   entry and the mill. It is one mill of 3 × (kickers paid) cards, not one mill per kicker.
 * - The P/T is a characteristic-defining ability (`dynamicStats`) counting only *your* graveyard,
 *   so it already includes the creature cards just milled.
 */
val UrborgLhurgoyf = card("Urborg Lhurgoyf") {
    manaCost = "{1}{G}"
    colorIdentity = "GUB"
    typeLine = "Creature — Lhurgoyf"
    dynamicStats(
        DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature),
        toughnessOffset = 1
    )
    oracleText = "Kicker {U} and/or {B} (You may pay an additional {U} and/or {B} as you cast this spell.)\n" +
        "As this creature enters, mill three cards for each time it was kicked.\n" +
        "Urborg Lhurgoyf's power is equal to the number of creature cards in your graveyard and its " +
        "toughness is equal to that number plus 1."

    keywordAbility(KeywordAbility.kicker("{U}"))
    keywordAbility(KeywordAbility.kicker("{B}"))

    replacementEffect(
        OnEnterRun(
            Patterns.Library.mill(
                DynamicAmounts.conditional(Conditions.WasKickedWithFirstKicker, 3, 0) +
                    DynamicAmounts.conditional(Conditions.WasKickedWithSecondKicker, 3, 0)
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6f97236-1497-4811-8189-0545707451da.jpg?1783921291"
        ruling("2022-09-09", "You may kick Urborg Lhurgoyf only once for its {U} cost and only once for its {B} cost. You can kick it twice by paying {U}{B}, but you can't kick it twice by paying {U}{U} or {B}{B}. You can't kick it more than twice.")
        ruling("2022-09-09", "Urborg Lhurgoyf's second ability isn't a triggered ability and doesn't use the stack. Players can't cast spells or take other actions after it enters the battlefield but before its controller mills cards.")
        ruling("2022-09-09", "If it was kicked, Urborg Lhurgoyf enters the battlefield with a power and toughness determined by the number of creature cards in your graveyard after you have milled cards for each time it was kicked. For example, if you have one creature card in your graveyard before it enters the battlefield and you mill two more creature cards with its ability, it enters the battlefield as a 3/4 creature.")
        ruling("2022-09-09", "The ability that defines Urborg Lhurgoyf's power and toughness works in all zones, not just on the battlefield.")
    }
}
