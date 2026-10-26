package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Alisaie Leveilleur (FIC #9)
 * {2}{W}
 * Legendary Creature — Elf Wizard
 * 3/2
 * Partner with Alphinaud Leveilleur (When this creature enters, target player may put Alphinaud
 * Leveilleur into their hand from their library, then shuffle.)
 * First strike
 * Dualcast — The second spell you cast each turn costs {2} less to cast.
 *
 * "Partner with" is the deck-construction keyword plus the enters trigger it implies, written out as
 * on Chakram Retriever (the keyword doesn't synthesise the trigger). Dualcast is an ability word over
 * Highspire Bell-Ringer's static: a generic-only reduction gated on the second spell cast this turn,
 * which counts spells cast before Alisaie entered.
 */
val AlisaieLeveilleur = card("Alisaie Leveilleur") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Elf Wizard"
    power = 3
    toughness = 2
    oracleText = "Partner with Alphinaud Leveilleur (When this creature enters, target player may put " +
        "Alphinaud Leveilleur into their hand from their library, then shuffle.)\n" +
        "First strike\n" +
        "Dualcast — The second spell you cast each turn costs {2} less to cast."

    keywordAbility(KeywordAbility.partnerWith("Alphinaud Leveilleur"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.ForEachPlayer(
            player.asPlayer,
            listOf(
                Effects.May(
                    Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Any.named("Alphinaud Leveilleur"),
                        count = 1,
                        destination = SearchDestination.HAND,
                        reveal = true,
                        shuffleAfter = true
                    )
                )
            )
        )
    }

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any),
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.NthOfTypePerTurn(2),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba96f831-02cb-4a74-a2e4-7f76444e3cd7.jpg?1783906371"

        ruling("2025-06-06", "\"Partner with [name]\" represents two abilities. The first is a triggered ability: \"When this permanent enters, target player may search their library for a card named [name], reveal it, put it into their hand, then shuffle.\"")
        ruling("2025-06-06", "Note that the target player searches their library (which may be affected by effects such as that of Stranglehold) and that the card they find is revealed, even though these words aren't included in the ability's reminder text.")
        ruling("2025-06-06", "The triggered ability of the \"partner with\" keyword still triggers in a Commander game. If your other commander has somehow ended up in your library, you can find it. You can also target another player, whether or not they have that card in their library.")
        ruling("2025-06-06", "The second ability represented by the \"partner with [name]\" keyword modifies the rules for deck construction in the Commander variant and has no function outside of that variant. If a legendary creature card with \"partner with [name]\" is designated as your commander, the named legendary creature card can also be designated as your commander.")
        ruling("2025-06-06", "Once the game begins, your two commanders are tracked separately. If you cast one, you won't have to pay an additional {2} the first time you cast the other. A player loses the game after having been dealt 21 damage from one of them, not from both of them combined. Command Beacon's effect puts one into your hand from the command zone, not both.")
        ruling("2025-06-06", "The cost reduction applies only to generic mana in the total cost of the second spell you cast each turn.")
        ruling("2025-06-06", "Spells that were cast before Alisaie Leveilleur entered count. If Alisaie Leveilleur was the first spell you cast this turn, the next spell you cast this turn is your second spell.")
    }
}
