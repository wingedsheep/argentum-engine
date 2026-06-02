package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.dsl.Effects


/**
 * Sunbird's Invocation
 * {5}{R}
 * Enchantment
 * Whenever you cast a spell from your hand, reveal the top X cards of your library,
 * where X is that spell's mana value. You may cast a spell with mana value X or less
 * from among cards revealed this way without paying its mana cost. Put the rest on
 * the bottom of your library in a random order.
 *
 * Composed entirely from atomic pipeline primitives so the mechanic stays inspectable
 * (and reusable for any future "reveal top X, maybe cast one ≤ X, bottom the rest"
 * card). The four steps mirror the oracle text — the revealed cards stay in the
 * library throughout (no exile staging), matching the printed wording:
 *
 *   1. **Reveal** the top X cards (X = triggering spell's mana value). `GatherCardsEffect`
 *      emits a single `CardsRevealedEvent` and leaves the cards on top of the library.
 *   2. **Select up to one** card to cast. Eligibility = nonland with mana value ≤ X;
 *      `showAllCards = true` shows the lands too, but they're not selectable (greyed
 *      out in the UI), and the player can confirm without picking.
 *   3. **Bottom the rest** (everything except the chosen card) in a random order — the
 *      `MoveCollectionEffect` shuffles within the same zone (library → library bottom).
 *   4. **Cast the chosen card** directly from the library via
 *      [com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect],
 *      which grants a `MayPlayPermission` scoped to that single card;
 *      `CastZoneResolver.isInExileWithPlayPermission` honours library-resident
 *      grants. If the cast pauses for targets / X / mode prompts, the pipeline
 *      pauses with it.
 */
val SunbirdsInvocation = card("Sunbird's Invocation") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a spell from your hand, reveal the top X cards of your library, where X is that spell's mana value. You may cast a spell with mana value X or less from among cards revealed this way without paying its mana cost. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)),
        )
        val triggeringSpellManaValue = DynamicAmount.EntityProperty(
            EntityReference.Triggering,
            EntityNumericProperty.ManaValue,
        )
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        count = triggeringSpellManaValue,
                        player = Player.You,
                    ),
                    storeAs = "revealed",
                    revealed = true,
                ),
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland
                        .manaValueAtMostEntity(EntityReference.Triggering),
                    storeSelected = "chosen",
                    storeRemainder = "toBottom",
                    showAllCards = true,
                    prompt = "You may cast one of the revealed cards without paying its mana cost.",
                    selectedLabel = "Cast for free",
                    remainderLabel = "Put on bottom",
                ),
                MoveCollectionEffect(
                    from = "toBottom",
                    destination = CardDestination.ToZone(
                        Zone.LIBRARY,
                        placement = ZonePlacement.Bottom,
                    ),
                    order = CardOrder.Random,
                ),
                CastFromCollectionWithoutPayingCostEffect(from = "chosen"),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Christine Choi"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ee52480-8faf-4418-af70-fd999096e3bb.jpg?1562551811"
        ruling("2017-09-29", "If a revealed card in your library has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2017-09-29", "If the spell's mana value is 0, you do nothing as the ability of Sunbird's Invocation resolves.")
        ruling("2017-09-29", "Casting Sunbird's Invocation won't cause its own ability to trigger.")
        ruling("2017-09-29", "The ability of Sunbird's Invocation resolves before the spell that caused it to trigger. It will resolve even if that spell is countered. If you cast a spell as part of the resolution of the ability, that spell resolves before the spell that caused the ability to trigger.")
        ruling("2017-09-29", "If you cast one of the revealed cards, you do so as part of the resolution of the triggered ability. You can't wait to cast it later in the turn. Timing permissions based on the card's type are ignored, but other restrictions (such as \"Cast [this card] only during combat\") are not.")
        ruling("2017-09-29", "If you cast a card \"without paying its mana cost,\" you can't pay any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, such as that of Costly Plunder, those must be paid to cast the card.")
    }
}
