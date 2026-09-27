package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Uba Mask — Champions of Kamigawa #272
 * {4} · Artifact · Rare
 *
 * If a player would draw a card, that player exiles that card face up instead.
 * Each player may play lands and cast spells from among cards they exiled with this artifact
 * this turn.
 *
 * Modelling notes (same shape as Shared Fate):
 * - One global replacement (`DrawEvent(Player.Each)`). The replacement's pipeline runs with the
 *   drawing player as controller, so "that card" is the top of *their* library and the play
 *   permission is granted to *them* — the pile is partitioned by exiler for free.
 * - "this turn" is [MayPlayExpiry.EndOfTurn]: cards not played stay exiled forever (rulings).
 * - The second sentence is a static ability of this artifact, so the permission is also gated on
 *   Uba Mask still being on the battlefield (`SourceInZone(BATTLEFIELD)`, re-checked at every
 *   query). Known limitation: the gate is not a one-way latch, so if this same Uba Mask leaves and
 *   returns within the same turn, that turn's earlier exiles become playable again, although
 *   CR 400.7 makes the returned artifact a new object.
 */
val UbaMask = card("Uba Mask") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "If a player would draw a card, that player exiles that card face up instead.\n" +
        "Each player may play lands and cast spells from among cards they exiled with this " +
        "artifact this turn."

    replacementEffect(
        ReplaceDrawWith(
            appliesTo = EventPattern.DrawEvent(player = Player.Each),
            replacementEffect = Effects.Pipeline {
                val ubaMaskExiled = gather(CardSource.TopOfLibrary(count = 1, player = Player.You))
                exile(ubaMaskExiled, linkToSource = true)
                run(Effects.GrantMayPlayFromExile(
                    from = ubaMaskExiled,
                    expiry = MayPlayExpiry.EndOfTurn,
                    condition = Conditions.SourceInZone(Zone.BATTLEFIELD)
                ))
            }
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "272"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa3ecb4e-d08f-4fac-8842-c3e772b95bd5.jpg?1783944275"

        ruling("2004-12-01", "You can't play cards you exiled with Uba Mask on previous turns.")
        ruling("2004-12-01", "Any cards you don't play just remain exiled when the turn ends.")
    }
}
