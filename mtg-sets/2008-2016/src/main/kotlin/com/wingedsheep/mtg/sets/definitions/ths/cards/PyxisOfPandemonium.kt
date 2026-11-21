package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Pyxis of Pandemonium — Theros #220
 * {1} · Artifact
 *
 * {T}: Each player exiles the top card of their library face down.
 * {7}, {T}, Sacrifice this artifact: Each player turns face up all cards they own exiled with this
 * artifact, then puts all permanent cards among them onto the battlefield.
 *
 * "Exiled with this artifact" is a linked-exile pile (CR 607): the first ability writes every
 * player's top card onto this Pyxis's own pile ([FaceDownMode.HIDDEN] — nobody may look at them,
 * the 2013-09-15 ruling), and the second reads that pile back with `CardSource.FromLinkedExile`,
 * even though the Pyxis was sacrificed as part of the cost (the Endless Sands shape). A new Pyxis
 * is a new object with an empty pile.
 *
 * Every card in the pile is owned by some player, so "each player turns face up all cards they
 * own" turns the whole pile face up in place; the non-permanent cards stay in exile, face up. Each
 * player puts *their own* permanent cards onto the battlefield, so they enter under their owner's
 * control.
 */
val PyxisOfPandemonium = card("Pyxis of Pandemonium") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Each player exiles the top card of their library face down.\n" +
        "{7}, {T}, Sacrifice this artifact: Each player turns face up all cards they own exiled " +
        "with this artifact, then puts all permanent cards among them onto the battlefield."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            // Exiled unseen: nobody looks at the cards on the way (the 2013-09-15 ruling).
            val tops = gather(
                CardSource.TopOfLibrary(count = 1, player = Player.Each),
                lookAudience = LookAudience.None,
            )
            exile(tops, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
        }
        description = "{T}: Each player exiles the top card of their library face down."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{7}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val pyxisExiled = gather(CardSource.FromLinkedExile())
            turnFaceUp(pyxisExiled)
            move(
                pyxisExiled,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                underOwnersControl = true,
                filter = GameObjectFilter.Permanent,
            )
        }
        description = "{7}, {T}, Sacrifice this artifact: Each player turns face up all cards they " +
            "own exiled with this artifact, then puts all permanent cards among them onto the " +
            "battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbc6a246-f32a-4dc0-9785-4038804f372f.jpg?1783939717"
        ruling(
            "2013-09-15",
            "Neither player can look at cards exiled with Pyxis of Pandemonium until those cards " +
                "are turned face up."
        )
        ruling(
            "2013-09-15",
            "If Pyxis of Pandemonium leaves the battlefield, and later another Pyxis of " +
                "Pandemonium enters the battlefield, it is a new object (even if the two were " +
                "represented the same card). Cards exiled by the original one can't be turned " +
                "face up or put onto the battlefield by the second one."
        )
    }
}
