package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Reality Shift
 * {1}{U}
 * Instant
 * Exile target creature. Its controller manifests the top card of their library.
 *
 * "Its controller" is the exiled creature's last-known controller ([Player.ControllerOf] falls back
 * to it once the creature has left), and the manifested card enters face down under that player's
 * control — never the caster's.
 */
val RealityShift = card("Reality Shift") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Exile target creature. Its controller manifests the top card of their library. " +
        "(That player puts the top card of their library onto the battlefield face down as a 2/2 " +
        "creature. If it's a creature card, it can be turned face up any time for its mana cost.)"

    spell {
        val t = target(TargetFilter.Creature)
        val controllerOfTarget = Player.ControllerOf("target creature")
        effect = Effects.Pipeline {
            run(Effects.Exile(t))
            val top = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(1), player = controllerOfTarget))
            move(
                top,
                CardDestination.ToZone(Zone.BATTLEFIELD, player = controllerOfTarget),
                faceDown = FaceDownMode.MANIFEST
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e01367cb-79f4-4ed9-b12c-66f3c30264a0.jpg?1783938704"
        ruling(
            "2014-11-24",
            "Because the permanent is on the battlefield both before and after it's turned face up, turning a " +
                "permanent face up doesn't cause any enters-the-battlefield abilities to trigger."
        )
        ruling(
            "2014-11-24",
            "Unlike a face-down creature that was cast using the morph ability, a manifested creature may still be " +
                "turned face up after it loses its abilities if it's a creature card."
        )
    }
}
