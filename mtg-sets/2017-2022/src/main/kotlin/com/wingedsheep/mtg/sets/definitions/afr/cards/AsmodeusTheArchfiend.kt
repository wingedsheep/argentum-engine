package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Asmodeus the Archfiend — Adventures in the Forgotten Realms #88
 * {4}{B}{B} · Legendary Creature — Devil God 6/6
 *
 * Binding Contract — If you would draw a card, exile the top card of your library face down instead.
 * {B}{B}{B}: Draw seven cards.
 * {B}: Return all cards exiled with Asmodeus to their owner's hand and you lose that much life.
 *
 * Each card of a multi-card draw is replaced on its own, so `{B}{B}{B}` exiles seven cards. The
 * exile links each card to Asmodeus (`linkToSource`), which is the pile its last ability reads; a
 * new Asmodeus object has no link to an old pile. No look grant: nobody may look at the face-down
 * cards (first ruling). The life loss counts the cards that actually moved.
 */
val AsmodeusTheArchfiend = card("Asmodeus the Archfiend") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Devil God"
    power = 6
    toughness = 6
    oracleText = "Binding Contract — If you would draw a card, exile the top card of your library face down instead.\n" +
        "{B}{B}{B}: Draw seven cards.\n" +
        "{B}: Return all cards exiled with Asmodeus to their owner's hand and you lose that much life."

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(1))
                move(
                    top,
                    CardDestination.ToZone(Zone.EXILE),
                    faceDown = FaceDownMode.HIDDEN,
                    linkToSource = true,
                )
            }
        )
    )

    activatedAbility {
        cost = Costs.Mana("{B}{B}{B}")
        effect = Effects.DrawCards(7)
        description = "{B}{B}{B}: Draw seven cards."
    }

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.Pipeline {
            val pile = gather(CardSource.FromLinkedExile())
            val returned = moveTracked(pile, CardDestination.ToZone(Zone.HAND))
            run(Effects.LoseLife(returned.count, EffectTarget.Controller))
        }
        description = "{B}: Return all cards exiled with Asmodeus to their owner's hand and you lose that much life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5e6b864-58e7-43b9-9d79-1d0361340960.jpg?1783926501"

        ruling(
            "2021-07-23",
            "If a card is exiled face-down and no player has been given permission to look at it, " +
                "no player is allowed to look at it as long as it remains exiled face-down."
        )
        ruling(
            "2021-07-23",
            "If your library is empty, no cards will be exiled but you won't lose the game due to " +
                "drawing from an empty library."
        )
        ruling(
            "2021-07-23",
            "If Asmodeus the Archfiend leaves the battlefield before you activate its last ability, " +
                "any cards exiled by its replacement effect remain exiled face down for the rest of the " +
                "game. If you somehow return the same Asmodeus the Archfiend card to the battlefield, it " +
                "will be a different object with no connection to those face-down cards."
        )
    }
}
