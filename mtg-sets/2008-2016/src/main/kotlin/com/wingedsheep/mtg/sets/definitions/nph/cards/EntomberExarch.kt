package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Entomber Exarch
 * {2}{B}{B}
 * Creature — Phyrexian Cleric
 * 2/2
 * When this creature enters, choose one —
 * • Return target creature card from your graveyard to your hand.
 * • Target opponent reveals their hand. You choose a noncreature card from it. That player discards that card.
 *
 * A [ModalEffect.chooseOne] enters trigger, each mode carrying its own target (as on its cycle-mate
 * Deceiver Exarch). The first mode is Gravedigger's move; the second is Duress's reveal → choose →
 * discard pipeline with the filter widened to any noncreature card (lands included).
 */
val EntomberExarch = card("Entomber Exarch") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Cleric"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, choose one —\n" +
        "• Return target creature card from your graveyard to your hand.\n" +
        "• Target opponent reveals their hand. You choose a noncreature card from it. That player discards that card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Return target creature card from your graveyard to your hand.") {
                val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.Move(creatureCard, Zone.HAND)
            },
            mode("Target opponent reveals their hand. You choose a noncreature card from it. That player discards that card.") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Pipeline {
                    run(Effects.RevealHand(opponent))
                    val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                    val toDiscard = chooseExactly(
                        1,
                        from = opponentHand,
                        chooser = Chooser.Controller,
                        filter = GameObjectFilter.Noncreature,
                        prompt = "Choose a noncreature card to discard",
                        alwaysPrompt = true,
                        showAllCards = true
                    )
                    discard(toDiscard, opponent.asPlayer)
                }
            }
        )
        description = "When this creature enters, choose one — Return target creature card from your " +
            "graveyard to your hand; or target opponent reveals their hand, you choose a noncreature card " +
            "from it, and that player discards that card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f58020e-6d4d-474d-8d4b-cfb7d5a5e9a8.jpg"
    }
}
