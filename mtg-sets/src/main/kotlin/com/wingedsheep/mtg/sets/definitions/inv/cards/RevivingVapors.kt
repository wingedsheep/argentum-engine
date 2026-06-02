package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Reviving Vapors
 * {2}{W}{U}
 * Instant
 *
 * Reveal the top three cards of your library and put one of them into your hand.
 * You gain life equal to that card's mana value. Put all other cards revealed this
 * way into your graveyard.
 *
 * Composed from the atomic reveal pipeline: Gather (top 3, revealed) → Select exactly 1
 * (caster's pick, remainder stored) → Move chosen to hand → gain life equal to its mana
 * value via [DynamicAmount.StoredCardManaValue] → move the remainder to the graveyard.
 */
val RevivingVapors = card("Reviving Vapors") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Reveal the top three cards of your library and put one of them into your hand. " +
        "You gain life equal to that card's mana value. Put all other cards revealed this way " +
        "into your graveyard."

    spell {
        effect = Effects.Composite(
            listOf(
                // 1. Reveal the top three cards of your library.
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(3), Player.You),
                    storeAs = "revealed",
                    revealed = true
                ),
                // 2. Put one of them into your hand (rest become the remainder).
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "chosen",
                    storeRemainder = "rest",
                    prompt = "Choose a card to put into your hand",
                    alwaysPrompt = true
                ),
                // 3. Move the chosen card to your hand.
                MoveCollectionEffect(
                    from = "chosen",
                    destination = CardDestination.ToZone(Zone.HAND, Player.You)
                ),
                // 4. You gain life equal to that card's mana value.
                Effects.GainLife(DynamicAmount.StoredCardManaValue("chosen"), EffectTarget.Controller),
                // 5. Put all other cards revealed this way into your graveyard.
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "265"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47a23c32-e122-400b-b252-e636ea2e684b.jpg?1562909595"
    }
}
