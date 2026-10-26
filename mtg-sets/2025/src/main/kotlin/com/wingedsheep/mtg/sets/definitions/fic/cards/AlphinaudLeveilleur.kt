package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Alphinaud Leveilleur (FIC #33)
 * {3}{U}
 * Legendary Creature — Elf Wizard
 * 2/4
 * Partner with Alisaie Leveilleur (When this creature enters, target player may put Alisaie
 * Leveilleur into their hand from their library, then shuffle.)
 * Vigilance
 * Eukrasia — Whenever you cast your second spell each turn, draw a card.
 *
 * "Partner with" is the deck-construction keyword plus the enters trigger it implies, written out as
 * on Chakram Retriever. Eukrasia is an ability word over Clarion Spirit's `castsNth(2)` trigger,
 * which counts spells cast before Alphinaud entered.
 */
val AlphinaudLeveilleur = card("Alphinaud Leveilleur") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Elf Wizard"
    power = 2
    toughness = 4
    oracleText = "Partner with Alisaie Leveilleur (When this creature enters, target player may put " +
        "Alisaie Leveilleur into their hand from their library, then shuffle.)\n" +
        "Vigilance\n" +
        "Eukrasia — Whenever you cast your second spell each turn, draw a card."

    keywordAbility(KeywordAbility.partnerWith("Alisaie Leveilleur"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.ForEachPlayer(
            player.asPlayer,
            listOf(
                Effects.May(
                    Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Any.named("Alisaie Leveilleur"),
                        count = 1,
                        destination = SearchDestination.HAND,
                        reveal = true,
                        shuffleAfter = true
                    )
                )
            )
        )
    }

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.castsNth(2)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65331e86-1ff3-45e4-b789-7afec4ca9bd5.jpg?1783906363"

        ruling("2025-06-06", "\"Partner with [name]\" represents two abilities. The first is a triggered ability: \"When this permanent enters, target player may search their library for a card named [name], reveal it, put it into their hand, then shuffle.\"")
        ruling("2025-06-06", "Note that the target player searches their library (which may be affected by effects such as that of Stranglehold) and that the card they find is revealed, even though these words aren't included in the ability's reminder text.")
        ruling("2025-06-06", "The triggered ability of the \"partner with\" keyword still triggers in a Commander game. If your other commander has somehow ended up in your library, you can find it. You can also target another player, whether or not they have that card in their library.")
        ruling("2025-06-06", "The second ability represented by the \"partner with [name]\" keyword modifies the rules for deck construction in the Commander variant and has no function outside of that variant. If a legendary creature card with \"partner with [name]\" is designated as your commander, the named legendary creature card can also be designated as your commander.")
        ruling("2025-06-06", "Once the game begins, your two commanders are tracked separately. If you cast one, you won't have to pay an additional {2} the first time you cast the other. A player loses the game after having been dealt 21 damage from one of them, not from both of them combined. Command Beacon's effect puts one into your hand from the command zone, not both.")
        ruling("2025-06-06", "Alphinaud Leveilleur's last ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack.")
        ruling("2025-06-06", "Spells that were cast before Alphinaud Leveilleur entered count. If Alphinaud Leveilleur was the first spell you cast this turn, the next spell you cast this turn is your second spell.")
    }
}
