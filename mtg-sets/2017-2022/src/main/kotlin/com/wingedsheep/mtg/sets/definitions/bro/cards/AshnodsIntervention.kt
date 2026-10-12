package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ashnod's Intervention
 * {B}
 * Instant
 * Until end of turn, target creature gets +2/+0 and gains "When this creature dies or is put
 * into exile from the battlefield, return it to its owner's hand."
 *
 * Same shape as Fake Your Own Death: a +2/+0 pump plus a granted self leaves-trigger for the turn.
 * The printed rider's two legs are granted as two triggers, each returning the card only from the
 * zone it went to (`fromZone`) — if the card has moved on again before the trigger resolves it is a
 * new object (CR 400.7) and stays where it is.
 */
val AshnodsIntervention = card("Ashnod's Intervention") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gets +2/+0 and gains \"When this creature dies or is " +
        "put into exile from the battlefield, return it to its owner's hand.\""

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, t) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD),
                    descriptionOverride = "When this creature dies, return it to its owner's hand.",
                ),
                target = t,
                duration = Duration.EndOfTurn,
            ) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.leaves(to = Zone.EXILE),
                    effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.EXILE),
                    descriptionOverride = "When this creature is put into exile from the battlefield, " +
                        "return it to its owner's hand.",
                ),
                target = t,
                duration = Duration.EndOfTurn,
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Miguel Mercado"
        flavorText = "\"Listen, Duck. Gix is everywhere, playing every side. Mishra won't listen . . . " +
            "but perhaps Urza will.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcf491db-d12b-4c58-9c05-085cac34a73e.jpg?1783920094"
    }
}
