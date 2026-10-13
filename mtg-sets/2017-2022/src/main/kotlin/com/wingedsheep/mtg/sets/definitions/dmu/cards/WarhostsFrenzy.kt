package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Warhost's Frenzy
 * {2}{R}
 * Instant
 *
 * Kicker {B}
 * Creatures you control get +2/+0 until end of turn. If this spell was kicked, whenever a creature
 * you control dies this turn, draw a card.
 *
 * - The kicked rider is a floating "this turn, whenever …" ability (CR 603.7 — created by a
 *   resolving spell), so it is a global triggered ability for `Duration.EndOfTurn` (the Death Frenzy
 *   shape), gated on [Conditions.WasKicked] at resolution. "A creature you control" is read against
 *   the spell's controller, who owns the global ability.
 */
val WarhostsFrenzy = card("Warhost's Frenzy") {
    manaCost = "{2}{R}"
    colorIdentity = "BR"
    typeLine = "Instant"
    oracleText = "Kicker {B} (You may pay an additional {B} as you cast this spell.)\n" +
        "Creatures you control get +2/+0 until end of turn. If this spell was kicked, whenever a creature " +
        "you control dies this turn, draw a card."

    keywordAbility(KeywordAbility.kicker("{B}"))

    spell {
        effect = Patterns.Group.modifyStatsForAll(2, 0, GroupFilter.AllCreaturesYouControl) then
            Effects.If(
                Conditions.WasKicked,
                Effects.CreateGlobalTriggeredAbility(
                    duration = Duration.EndOfTurn,
                    ability = TriggeredAbility.create(
                        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies(),
                        effect = Effects.DrawCards(1),
                    ),
                ),
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Igor Grechanyi"
        flavorText = "Keldons may be outnumbered, but they are never outmatched."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a83d571b-ad91-43f5-8498-115cede4867d.jpg?1783921307"
    }
}
