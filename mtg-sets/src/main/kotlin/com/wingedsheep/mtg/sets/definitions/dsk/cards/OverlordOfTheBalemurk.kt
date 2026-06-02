package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Overlord of the Balemurk
 * {3}{B}{B}
 * Enchantment Creature — Avatar Horror
 * 5/5
 *
 * Impending 5—{1}{B} (If you cast this spell for its impending cost, it enters with five
 * time counters and isn't a creature until the last is removed. At the beginning of your
 * end step, remove a time counter from it.)
 *
 * Whenever this permanent enters or attacks, mill four cards, then you may return a
 * non-Avatar creature card or a planeswalker card from your graveyard to your hand.
 *
 * Impending is wired by the `impending(n, cost)` DSL helper: the alternative cost, the
 * "isn't a creature while it has a time counter" type-removing static ability, and the
 * "remove a time counter at the beginning of your end step" trigger. The engine places
 * the five time counters when the spell is cast for its impending cost.
 *
 * The "enters or attacks" ability is one effect referenced by two triggered abilities
 * (an enters-the-battlefield trigger and an attacks trigger). Per the Scryfall ruling,
 * the returned card may be any eligible card in your graveyard — not only one milled by
 * this resolution — so the optional return gathers the whole graveyard.
 */
val OverlordOfTheBalemurk = card("Overlord of the Balemurk") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Avatar Horror"
    oracleText = "Impending 5—{1}{B} (If you cast this spell for its impending cost, it enters with five time counters and isn't a creature until the last is removed. At the beginning of your end step, remove a time counter from it.)\n" +
        "Whenever this permanent enters or attacks, mill four cards, then you may return a non-Avatar creature card or a planeswalker card from your graveyard to your hand."
    power = 5
    toughness = 5

    impending(5, "{1}{B}")

    // "Mill four cards, then you may return a non-Avatar creature card or a planeswalker
    // card from your graveyard to your hand." Shared by the enters and attacks triggers.
    val returnableFilter = GameObjectFilter.Creature.notSubtype(Subtype.AVATAR) or GameObjectFilter.Planeswalker
    val millAndReturn: Effect = Effects.Composite(
        listOf(
            // Mill four cards.
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4), Player.You),
                storeAs = "balemurkMilled"
            ),
            MoveCollectionEffect(
                from = "balemurkMilled",
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You)
            ),
            // ...then you may return a non-Avatar creature or planeswalker card from your
            // graveyard to your hand (any eligible card, not just the milled ones).
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, returnableFilter),
                storeAs = "balemurkReturnable"
            ),
            SelectFromCollectionEffect(
                from = "balemurkReturnable",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "balemurkToReturn",
                showAllCards = true,
                prompt = "You may return a non-Avatar creature or planeswalker card to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            ),
            MoveCollectionEffect(
                from = "balemurkToReturn",
                destination = CardDestination.ToZone(Zone.HAND)
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = millAndReturn
        description = "Whenever this permanent enters, mill four cards, then you may return a non-Avatar creature card or a planeswalker card from your graveyard to your hand."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = millAndReturn
        description = "Whenever this permanent attacks, mill four cards, then you may return a non-Avatar creature card or a planeswalker card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "113"
        artist = "Babs Webb"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b911653-7b96-4cf3-a907-13c5c53a14f7.jpg?1726286269"
        ruling("2024-09-20", "You may return any non-Avatar creature card or any planeswalker card from your graveyard to your hand with the last ability, not just one you milled while resolving that ability.")
        ruling("2024-09-20", "If you choose to pay the impending cost rather than the mana cost, you're still casting the spell. It goes on the stack and can be responded to, countered, and so on.")
        ruling("2024-09-20", "If an object enters as a copy of a permanent that was cast with its impending cost, it won't enter with time counters, and it will be a creature.")
    }
}
