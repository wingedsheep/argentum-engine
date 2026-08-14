package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Chimney Imp — Mirrodin #59
 * {4}{B} · Creature — Imp · 1/2
 *
 * Flying
 * When this creature dies, target opponent puts a card from their hand on top of their library.
 *
 * The dies trigger is the Gather → Select → Move pipeline over the *opponent's* hand:
 * `Player.ContextPlayer(0)` is the declared target, and [Chooser.TargetPlayer] hands the
 * decision to that opponent rather than to the Imp's controller — the printed text says
 * "*target opponent* puts a card", so they pick, and there is no reveal step. That distinction
 * is the whole difference between this and a
 * [Duress][com.wingedsheep.mtg.sets.definitions.usg.cards.Duress]-style effect built on the
 * same three primitives with `Chooser.Controller`.
 *
 * [ZonePlacement.Top] on a `Zone.LIBRARY` destination scoped to the same
 * `Player.ContextPlayer(0)` is "on top of *their* library". The net effect is a Time Ebb on a
 * card in hand: they lose no cards, but their next draw is already spent.
 *
 * Edge cases the pipeline handles by construction: an empty hand gathers nothing, so the
 * selection is skipped and the trigger resolves as a no-op (the ability still targeted legally
 * — an opponent is a legal target regardless of hand size); and the Imp is already in the
 * graveyard when this resolves, so nothing here reads its battlefield state.
 */
val ChimneyImp = card("Chimney Imp") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Imp"
    power = 1
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature dies, target opponent puts a card from their hand on top of their library."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Dies
        target = TargetOpponent()
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "impHand"
                ),
                SelectFromCollectionEffect(
                    from = "impHand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.TargetPlayer,
                    storeSelected = "impTucked",
                    prompt = "Choose a card to put on top of your library"
                ),
                MoveCollectionEffect(
                    from = "impTucked",
                    destination = CardDestination.ToZone(
                        Zone.LIBRARY,
                        Player.ContextPlayer(0),
                        ZonePlacement.Top
                    )
                )
            )
        )
        description = "When this creature dies, target opponent puts a card from their hand " +
            "on top of their library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a426922-5e96-48f3-b696-f5dc99258943.jpg?1783944549"
    }
}
