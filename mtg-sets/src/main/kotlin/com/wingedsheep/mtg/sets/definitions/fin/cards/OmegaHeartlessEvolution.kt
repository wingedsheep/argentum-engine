package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Omega, Heartless Evolution
 * {5}{G}{U}
 * Legendary Artifact Creature — Robot
 * 8/8
 *
 * Wave Cannon — When Omega enters, for each opponent, tap up to one target nonland permanent that
 * opponent controls. Put X stun counters on each of those permanents and you gain X life, where X
 * is the number of nonbasic lands you control.
 *
 * Modeling:
 *  - "Wave Cannon" is an ability word (flavor only). The clause is an enter trigger with an
 *    optional target — "up to one target nonland permanent that opponent controls" via
 *    `TargetPermanent(optional = true, filter = NonlandPermanentOpponentControls)`. Following the
 *    corpus convention for "for each opponent, … target … that player controls" (Blatant Thievery),
 *    the per-opponent structure is modeled as a single optional target — exactly right in 1v1 and
 *    the shape multiplayer targeting will generalize.
 *  - X = the number of nonbasic lands you control (`Count(You, BATTLEFIELD, NonbasicLand)`), read
 *    once at resolution. The chosen permanent is tapped, gets X stun counters (`AddDynamicCounters`
 *    with `Counters.STUN`, so it stays tapped through its controller's next untap), and you gain X
 *    life. The life gain is independent of whether a permanent was chosen, so it uses its own step.
 *    With no target chosen the tap/counters steps simply no-op and you still gain X life.
 */
val OmegaHeartlessEvolution = card("Omega, Heartless Evolution") {
    manaCost = "{5}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Artifact Creature — Robot"
    power = 8
    toughness = 8
    oracleText = "Wave Cannon — When Omega enters, for each opponent, tap up to one target nonland " +
        "permanent that opponent controls. Put X stun counters on each of those permanents and you " +
        "gain X life, where X is the number of nonbasic lands you control. (If a permanent with a " +
        "stun counter would become untapped, remove one from it instead.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "permanent",
            TargetPermanent(optional = true, filter = TargetFilter.NonlandPermanentOpponentControls)
        )
        val nonbasicLands = DynamicAmount.Count(
            Player.You,
            Zone.BATTLEFIELD,
            GameObjectFilter.NonbasicLand
        )
        effect = Effects.Composite(
            Effects.Tap(permanent),
            Effects.AddDynamicCounters(Counters.STUN, nonbasicLands, permanent),
            Effects.GainLife(nonbasicLands)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Josu Solano"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a8eb7e6-0c0b-42d0-aa90-2d3d29bc15aa.jpg?1782686412"
    }
}
