package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.dsl.Effects

/**
 * Polymorph
 * {3}{U}
 * Sorcery
 * Destroy target creature. It can't be regenerated. Its controller reveals cards from the top
 * of their library until they reveal a creature card. The player puts that card onto the
 * battlefield, then shuffles all other cards revealed this way into their library.
 */
val Polymorph = card("Polymorph") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. It can't be regenerated. Its controller reveals cards from the top of their library until they reveal a creature card. The player puts that card onto the battlefield, then shuffles all other cards revealed this way into their library."

    spell {
        val t = target("target creature", TargetCreature())
        val controllerOfTarget = Player.ControllerOf("target creature")
        effect = Effects.Composite(
            listOf(
                CantBeRegeneratedEffect(t),
                Effects.Move(t, Zone.GRAVEYARD, byDestruction = true),
                GatherUntilMatchEffect(
                    player = controllerOfTarget,
                    filter = GameObjectFilter.Creature,
                    storeMatch = "found",
                    storeRevealed = "allRevealed"
                ),
                // fromZone/toZone tag this as a zone-transition reveal so the client shows
                // the full reveal overlay including the matched creature (which lands on the
                // battlefield in the same update and would otherwise be filtered out as
                // public info — see web-client gameplayHandlers `isZoneTransitionReveal`).
                RevealCollectionEffect(
                    from = "allRevealed",
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.BATTLEFIELD
                ),
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, player = controllerOfTarget)
                ),
                ShuffleLibraryEffect(EffectTarget.TargetController)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Robert Bliss"
        flavorText = "\"Ahh! Opposable digits!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbae8702-a152-4c53-8a76-691a221f2475.jpg?1562722872"
        ruling("2013-07-01", "If the targeted creature has indestructible, it's still a legal target — it just isn't destroyed. The rest of Polymorph's effect happens as normal.")
        ruling("2009-10-01", "If the targeted creature is an illegal target by the time Polymorph would resolve, the entire spell doesn't resolve. Nothing else happens.")
        ruling("2009-10-01", "If there are no creature cards in the player's library, all the cards in that library are revealed, then the library is shuffled. (The targeted creature remains destroyed.)")
    }
}
