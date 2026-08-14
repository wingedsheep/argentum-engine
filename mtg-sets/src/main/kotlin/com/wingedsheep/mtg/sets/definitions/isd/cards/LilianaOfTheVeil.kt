package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChoosePileEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Liliana of the Veil — Innistrad #105
 * {1}{B}{B} · Legendary Planeswalker — Liliana · Starting loyalty 3
 *
 * +1: Each player discards a card.
 * −2: Target player sacrifices a creature.
 * −6: Separate all permanents target player controls into two piles. That player sacrifices all
 *     permanents in the pile of their choice.
 *
 * Modeling notes:
 *
 *  - **The +1 is symmetric and untargeted** — [Effects.EachPlayerDiscards] owns the APNAP loop and
 *    the simultaneous discard the ruling calls for ("first the player whose turn it is chooses a
 *    card in hand without revealing it, then each other player in turn order does the same. Then
 *    all the chosen cards are discarded at the same time"). Liliana's controller discards too; this
 *    is not "each opponent".
 *  - **The −2 targets a *player*, not a creature.** [Targets.Player] plus
 *    [Effects.Sacrifice] — the targeted player chooses which of their creatures dies, so
 *    hexproof/shroud/protection on the creatures is irrelevant. Modelling this as "destroy target
 *    creature" would be a different (and much worse) card.
 *  - **The −6 is a two-player pile split** built from the generic collection pipeline rather than a
 *    bespoke effect: gather every permanent the target player controls → *Liliana's controller*
 *    partitions them ([Chooser.Controller]) → *the target player* picks a pile
 *    ([Chooser.TargetPlayer]) → that pile is sacrificed. Note the two deciders are different
 *    players, which is the whole tension of the ability.
 *      - The partition is [SelectionMode.ChooseAnyNumber] with `storeRemainder`, so selecting none
 *        or all is legal — "a pile can be empty", and choosing an empty pile sacrifices nothing
 *        (the move step is a no-op on an empty collection).
 *      - `useTargetingUI` puts the partition on the battlefield instead of in a modal overlay: the
 *        splitter needs to see counters, Auras, and tapped state to split meaningfully, and the
 *        ruling explicitly contemplates splitting a creature away from the Aura enchanting it.
 *      - [MoveType.Sacrifice] rather than a plain graveyard move — it emits
 *        `PermanentsSacrificedEvent` (so sacrifice triggers see it) and routes each permanent to
 *        its *owner's* graveyard per CR 701.21a, which matters for stolen permanents.
 *  - **All permanents, not just nonland** — the printed text says "all permanents", so the gather
 *    is unfiltered [CardSource.ControlledPermanents]; lands are fair game.
 */
val LilianaOfTheVeil = card("Liliana of the Veil") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Liliana"
    startingLoyalty = 3
    oracleText = "+1: Each player discards a card.\n" +
        "−2: Target player sacrifices a creature.\n" +
        "−6: Separate all permanents target player controls into two piles. That player " +
        "sacrifices all permanents in the pile of their choice."

    // +1: Each player discards a card.
    loyaltyAbility(+1) {
        effect = Effects.EachPlayerDiscards(1)
    }

    // −2: Target player sacrifices a creature.
    loyaltyAbility(-2) {
        val player = target("target player", Targets.Player)
        effect = Effects.Sacrifice(GameObjectFilter.Creature, count = 1, target = player)
    }

    // −6: Separate all permanents target player controls into two piles. That player sacrifices
    //     all permanents in the pile of their choice.
    loyaltyAbility(-6) {
        target("target player", Targets.Player)
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.ControlledPermanents(Player.ContextPlayer(0)),
                    storeAs = "their_permanents"
                ),
                SelectFromCollectionEffect(
                    from = "their_permanents",
                    selection = SelectionMode.ChooseAnyNumber,
                    chooser = Chooser.Controller,
                    storeSelected = "pileA",
                    storeRemainder = "pileB",
                    selectedLabel = "Pile 1",
                    remainderLabel = "Pile 2",
                    prompt = "Separate their permanents into two piles. The permanents you " +
                        "select form Pile 1; the rest form Pile 2.",
                    useTargetingUI = true,
                    alwaysPrompt = true
                ),
                ChoosePileEffect(
                    pileA = "pileA",
                    pileB = "pileB",
                    pileALabel = "Pile 1",
                    pileBLabel = "Pile 2",
                    chooser = Chooser.TargetPlayer,
                    storeChosenAs = "sacrificedPile",
                    storeOtherAs = "keptPile",
                    prompt = "Choose a pile. You sacrifice all permanents in the pile you choose."
                ),
                MoveCollectionEffect(
                    from = "sacrificedPile",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Sacrifice
                )
            ),
            descriptionOverride = "Separate all permanents target player controls into two piles. " +
                "That player sacrifices all permanents in the pile of their choice."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "105"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac506c17-adc8-49c6-9d8d-43db7cb1ec9d.jpg?1783940954"

        ruling("2022-09-09", "You can activate Liliana's first ability even if some or all players will be unable to discard a card.")
        ruling("2022-09-09", "When Liliana's first ability resolves, first the player whose turn it is chooses a card in hand without revealing it, then each other player in turn order does the same. Then all the chosen cards are discarded at the same time.")
        ruling("2022-09-09", "When Liliana's third ability resolves, you put each permanent the player controls into one of the two piles. For example, you could put a creature into one pile and an Aura enchanting that creature into the other pile.")
        ruling("2022-09-09", "A pile can be empty. If the player chooses an empty pile, no permanents will be sacrificed.")
    }
}
