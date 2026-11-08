package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mind's Dilation — Eldritch Moon #70
 * {5}{U}{U} · Enchantment · Mythic
 *
 * Whenever an opponent casts their first spell each turn, that player exiles the top card of their
 * library. If it's a nonland card, you may cast it without paying its mana cost.
 *
 * - "Their first spell each turn" is `anOpponent.castsNth(1)` (The Queen of Dale's trigger), which
 *   counts that opponent's casts this turn — so a spell cast before Mind's Dilation entered still
 *   counts, and the trigger doesn't fire for their second spell.
 * - The payoff is Hidetsugu and Kairi's "exile the top card, may cast it free" pipeline walked over
 *   the triggering opponent's library (Grenzo's `TopOfLibrary(1, Player.TriggeringPlayer)`). The
 *   cast happens during resolution, by Mind's Dilation's controller; a land, or a declined cast,
 *   stays exiled.
 */
val MindsDilation = card("Mind's Dilation") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent casts their first spell each turn, that player exiles the top card " +
        "of their library. If it's a nonland card, you may cast it without paying its mana cost."

    triggeredAbility {
        trigger = Triggers.anOpponent.castsNth(1)
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1, Player.TriggeringPlayer))
            exile(top, Player.TriggeringPlayer)
            val nonland = filter(top, GameObjectFilter.Nonland)
            val toCast = chooseUpTo(
                1,
                from = nonland,
                prompt = "You may cast it without paying its mana cost",
                selectedLabel = "Cast without paying its mana cost",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(toCast))
        }
        description = "Whenever an opponent casts their first spell each turn, that player exiles the top " +
            "card of their library. If it's a nonland card, you may cast it without paying its mana cost."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "70"
        artist = "Iain McCaig"
        flavorText = "Infinite wisdom cannot be contained within a single mind."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3d6522a-b9a0-4185-b5ba-c20fa1b772eb.jpg?1783937494"
        ruling("2016-07-13", "If you cast the exiled card, you do so as part of the resolution of Mind's Dilation's ability. You can't wait to cast it later in the turn. Timing permissions based on the card's type are ignored, but other restrictions (such as \"Cast [this card] only during combat\") are not.")
        ruling("2016-07-13", "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative costs, such as emerge costs. You can, however, pay additional costs, such as escalate costs. If the card has any mandatory additional costs, you must pay those.")
        ruling("2016-07-13", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2016-07-13", "If you cast the card this way, it will resolve before the spell that caused Mind's Dilation's ability to trigger.")
        ruling("2016-07-13", "If you cast an instant or sorcery card this way, it goes to its owner's graveyard as normal. It doesn't return to exile.")
        ruling("2016-07-13", "If you don't cast the card, perhaps because you choose not to or because it's a land card, it remains exiled.")
    }
}
