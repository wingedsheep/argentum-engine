package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWithEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Shared Fate — Mirrodin #49
 * {4}{U} · Enchantment · Rare
 *
 * If a player would draw a card, that player exiles the top card of one of their opponents'
 * libraries face down instead.
 * Each player may look at cards they exiled with this enchantment, and they may play lands and
 * cast spells from among those cards.
 *
 * Modelling notes:
 * - The whole card is **one** replacement effect, not a replacement plus a static ability, and the
 *   reason is that the replacement already runs as the drawing player. `ReplacementEffectProcessor`
 *   builds the replacement's `EffectContext` with `controllerId = event.affectedPlayerId`, so the
 *   pipeline below is authored from the point of view of whoever was about to draw — "one of
 *   **their** opponents' libraries" and "cards **they** exiled" are both just `Controller` here,
 *   for every player at the table, with no per-player bookkeeping on the enchantment.
 * - That is also why the play permission is granted per exiled card rather than by a
 *   `MayPlayCardsFromExile` static: a static grants to *its controller*, and this card grants to
 *   every player, each over a different set of cards. Granting at exile time partitions the pile by
 *   exiler for free.
 * - [MayPlayExpiry.WhileSourceOnBattlefield] is what makes the second sentence behave like the
 *   static ability it is printed as: every grant dies with *this* enchantment, and a replacement
 *   Shared Fate does not revive them. That is the 2008-08-01 ruling, and it is why the window can't
 *   be `Permanent`. It also can't be `WhileYouControlSource` — opponents hold these permissions and
 *   never control the enchantment.
 * - `appliesTo = DrawEvent(Player.Each)` is the "a player" in the first line: the replacement is
 *   global, not scoped to the controller. It is mandatory (`optional` stays false), per the ruling
 *   that replacing your draws isn't optional — including the corollary that an empty opponents'
 *   library means you simply exile nothing and draw nothing, which falls out of the gather coming
 *   back empty.
 * - Scope: `Player.AnOpponent` resolves to *the* opponent, which is the whole choice in a
 *   two-player game. With three or more players the printed line lets the drawing player pick which
 *   opponent's library to take from; expressing that needs a per-resolution "choose a player" step
 *   the pipeline vocabulary doesn't have yet, so this deterministically takes the first opponent.
 */
val SharedFate = card("Shared Fate") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "If a player would draw a card, that player exiles the top card of one of their " +
        "opponents' libraries face down instead.\n" +
        "Each player may look at cards they exiled with this enchantment, and they may play lands " +
        "and cast spells from among those cards."

    replacementEffect(
        ReplaceDrawWithEffect(
            appliesTo = EventPattern.DrawEvent(player = Player.Each),
            replacementEffect = Effects.Composite(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        count = DynamicAmount.Fixed(1),
                        player = Player.AnOpponent
                    ),
                    storeAs = "sharedFateExiled"
                ),
                MoveCollectionEffect(
                    from = "sharedFateExiled",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    faceDown = FaceDownMode.HIDDEN,
                    linkToSource = true
                ),
                // "…and they may play lands and cast spells from among those cards" — "play", so
                // the permission covers a land drop as well as a cast. Granting it here rather than
                // as a static is what scopes it to the cards *this* player exiled.
                GrantMayPlayFromExileEffect(
                    from = "sharedFateExiled",
                    expiry = MayPlayExpiry.WhileSourceOnBattlefield("this enchantment")
                )
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad0d5b02-ef38-4dab-8ac6-78814ef27b55.jpg?1783944552"

        ruling(
            "2008-08-01",
            "Each Shared Fate tracks only the cards it exiled. If the Shared Fate which was " +
                "responsible for a card being exiled leaves the battlefield, putting another " +
                "Shared Fate onto the battlefield will not allow you to play that card again."
        )
        ruling(
            "2004-12-01",
            "You need to pay the costs of any cards you play from the Exile zone. This could be a " +
                "problem if you don't have the right colors of mana available."
        )
        ruling(
            "2004-12-01",
            "Replacing your draws isn't optional. You can't draw cards from your own library, even " +
                "if all your opponents' libraries are empty."
        )
        ruling(
            "2004-12-01",
            "If more than one Shared Fate is on the battlefield, you choose which one replaces each " +
                "card draw, but you can replace a draw only once."
        )
        ruling(
            "2004-12-01",
            "The cards are exiled, not put onto the players' hands. Players can look at and play " +
                "the exiled cards, but can't do anything else with them (the exiled cards can't be " +
                "discarded or cycled, for example)."
        )
    }
}
