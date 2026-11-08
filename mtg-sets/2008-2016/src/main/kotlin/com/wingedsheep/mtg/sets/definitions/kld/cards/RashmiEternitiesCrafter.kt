package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rashmi, Eternities Crafter — Kaladesh #184
 * {2}{G}{U} · Legendary Creature — Elf Druid · 2/3 · Mythic
 *
 * Whenever you cast your first spell each turn, reveal the top card of your library. You may cast
 * it without paying its mana cost if it's a spell with lesser mana value. If you don't cast it, put
 * it into your hand.
 *
 * - "Your first spell each turn" is `castsNth(1)` (Vial Smasher's trigger): it counts every spell
 *   you cast this turn, so a spell cast before Rashmi was on the battlefield (Rashmi herself
 *   included) uses up the trigger for the turn.
 * - "That spell's mana value" is [DynamicAmounts.triggeringSpellManaValue], captured on the cast
 *   event: {X} counts as the chosen X, and it stays right when the triggering spell is countered
 *   before the trigger resolves (both 2016-09-20 rulings). "Lesser" is "at most that minus one".
 * - The free cast is Discover the Impossible's `chooseSpell` tail: the mana-value filter is tested
 *   against the face that would be cast, and land faces are never offered (a land isn't a spell).
 *   Choosing nothing is the "may" decline.
 * - "If you don't cast it, put it into your hand" — a land, a too-expensive spell, a decline, or a
 *   cast that can't begin (no legal targets): whatever is still on top of the library goes to hand.
 *   A card that was cast has left the library for the stack.
 */
val RashmiEternitiesCrafter = card("Rashmi, Eternities Crafter") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elf Druid"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast your first spell each turn, reveal the top card of your library. " +
        "You may cast it without paying its mana cost if it's a spell with lesser mana value. " +
        "If you don't cast it, put it into your hand."

    triggeredAbility {
        trigger = Triggers.you.castsNth(1)
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You), revealed = true)
            val chosen = chooseSpell(
                from = revealed,
                filter = GameObjectFilter.Any
                    .manaValueAtMostDynamic(DynamicAmounts.triggeringSpellManaValue() - 1),
                showAllCards = true,
                prompt = "You may cast the revealed card without paying its mana cost if it's a spell " +
                    "with lesser mana value. Otherwise it goes to your hand.",
                selectedLabel = "Cast for free",
                remainderLabel = "Put into hand",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(from = chosen))
            // "If you don't cast it, put it into your hand." — a cast card has left the library.
            toHand(filter(revealed, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY)))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "184"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3ce37555-49a2-4112-95b4-f3376b55b45b.jpg?1783937168"
        ruling("2016-09-20", "If the revealed card is a land card, you put it into your hand.")
        ruling("2016-09-20", "Rashmi's ability triggers when you cast your first spell each turn, not just on your turn.")
        ruling("2016-09-20", "Rashmi has to be on the battlefield at the moment you cast your first spell. If that spell is Rashmi itself, Rashmi's ability can't trigger. If that spell causes Rashmi to leave the battlefield as an additional cost to cast it, Rashmi's ability can't trigger.")
        ruling("2016-09-20", "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value. For cards in your library with {X} in their mana costs, X is considered to be 0.")
        ruling("2016-09-20", "If Rashmi's ability resolves and the spell that caused it to trigger has been countered, use that spell's mana value as it last existed on the stack to determine whether or not you may cast the top card of your library.")
        ruling("2016-09-20", "If you cast the top card of your library, you do so as part of the resolution of Rashmi's ability. You can't wait to cast it later in the turn. Timing permissions based on the card's type are ignored, but other restrictions (such as \"Cast [this card] only during combat\") are not.")
        ruling("2016-09-20", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
