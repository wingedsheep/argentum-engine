package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Return to Action — Kamigawa: Neon Dynasty #121 (canonical printing)
 * {1}{B} · Instant
 *
 * Until end of turn, target creature gets +1/+0 and gains lifelink and "When this creature
 * dies, return it to the battlefield tapped under its owner's control."
 *
 * Same shape as Fake Your Own Death: a +1/+0 buff, an end-of-turn lifelink grant, and an
 * end-of-turn granted self dies-trigger that puts the card back onto the battlefield tapped
 * ([Effects.PutOntoBattlefield] with no controller override lands under its owner's control).
 */
val ReturnToAction = card("Return to Action") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gets +1/+0 and gains lifelink and \"When this " +
        "creature dies, return it to the battlefield tapped under its owner's control.\""

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 0, t) then
            Effects.GrantKeyword(Keyword.LIFELINK, t, Duration.EndOfTurn) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.PutOntoBattlefield(EffectTarget.Self, tapped = true),
                    descriptionOverride = "When this creature dies, return it to the battlefield tapped " +
                        "under its owner's control.",
                ),
                target = t,
                duration = Duration.EndOfTurn,
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Jeremy Wilson"
        flavorText = "\"You're hurt? Too bad. You stop fighting when I say, and not a moment sooner.\"\n" +
            "—Satoru Umezawa"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/342ec072-e581-490c-b5ae-a625bd35a153.jpg?1783923877"
    }
}
