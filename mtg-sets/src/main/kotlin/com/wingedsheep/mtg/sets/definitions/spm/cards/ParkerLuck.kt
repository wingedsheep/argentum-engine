package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Parker Luck
 * {2}{B}
 * Enchantment
 *
 * At the beginning of your end step, two target players each reveal the top card of
 * their library. They each lose life equal to the mana value of the card revealed by
 * the other player. Then they each put the card they revealed into their hand.
 *
 * Cross-referenced life loss: the ability targets two players (positional targets 0 and
 * 1). Each player's top card is gathered into its own pipeline collection ("revealedA" /
 * "revealedB") and revealed; then target 0 loses life equal to the *other* player's card's
 * mana value (StoredCardManaValue("revealedB")) and target 1 loses life equal to
 * StoredCardManaValue("revealedA"). Life loss is computed before either card moves, so it
 * reads the revealed top card (an empty library reveals nothing → mana value 0). Finally
 * each revealed card goes to its owner's hand. Player.ContextPlayer(index) and
 * EffectTarget.ContextTarget(index) resolve positionally against the two target players.
 */
val ParkerLuck = card("Parker Luck") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, two target players each reveal the top card of " +
        "their library. They each lose life equal to the mana value of the card revealed by the " +
        "other player. Then they each put the card they revealed into their hand."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        // "two target players" — a single count-2 requirement; the engine enforces the two
        // chosen players are distinct (CR 115.1b). Referenced positionally as targets 0 and 1.
        target("players", TargetPlayer(count = 2))
        effect = Effects.Composite(
            listOf(
                // Both target players reveal the top card of their library.
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.ContextPlayer(0)),
                    storeAs = "revealedA",
                ),
                RevealCollectionEffect(from = "revealedA"),
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.ContextPlayer(1)),
                    storeAs = "revealedB",
                ),
                RevealCollectionEffect(from = "revealedB"),
                // Each loses life equal to the mana value of the OTHER player's revealed card.
                LoseLifeEffect(
                    DynamicAmount.StoredCardManaValue("revealedB"),
                    EffectTarget.ContextTarget(0),
                ),
                LoseLifeEffect(
                    DynamicAmount.StoredCardManaValue("revealedA"),
                    EffectTarget.ContextTarget(1),
                ),
                // Then each puts the card they revealed into their hand.
                MoveCollectionEffect(
                    from = "revealedA",
                    destination = CardDestination.ToZone(Zone.HAND, Player.ContextPlayer(0)),
                ),
                MoveCollectionEffect(
                    from = "revealedB",
                    destination = CardDestination.ToZone(Zone.HAND, Player.ContextPlayer(1)),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Raoul Vitale"
        flavorText = "\"Things have gotta go my way eventually, right?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e375bcf0-7fcb-4fe4-a7e8-a4cbf9b23e3c.jpg?1783905343"
    }
}
