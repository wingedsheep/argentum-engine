package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rishkar's Expertise — Aether Revolt #123 (canonical / earliest real printing, 2017)
 * {4}{G}{G} · Sorcery
 *
 * Draw cards equal to the greatest power among creatures you control.
 * You may cast a spell with mana value 5 or less from your hand without paying its mana cost.
 *
 * - The draw reads the MAX-power aggregate over your creatures on resolution (The Legend of
 *   Kyoshi's chapter I shape); no creatures, or none with positive power, draws nothing.
 * - The Expertise rider is Sram's Expertise's free-cast hand pipeline with a mana value 5 cap. The
 *   hand is gathered after the draw, so a card just drawn can be the free spell (per the ruling).
 */
val RishkarsExpertise = card("Rishkar's Expertise") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Draw cards equal to the greatest power among creatures you control.\nYou may cast a spell " +
        "with mana value 5 or less from your hand without paying its mana cost."

    spell {
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        ) then Effects.Pipeline {
            val inHand = gather(CardSource.FromZone(Zone.HAND, Player.You, filter = GameObjectFilter.Nonland))
            val candidates = filter(inHand, GameObjectFilter.Any.manaValueAtMost(5))
            val chosen = chooseUpTo(1, from = candidates, selectedLabel = "Cast without paying its mana cost")
            run(Effects.CastFromCollectionWithoutPayingCost(chosen))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Magali Villeneuve"
        flavorText = "\"Control the Great Conduit? You might as well try to divert a river with your hands.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d58ba68-05c1-4cd8-a93d-321cd1739ccb.jpg?1783936738"
        ruling(
            "2017-04-18",
            "The mana value of a split card is determined by the combined mana cost of its two halves. If an " +
                "expertise spell allows you to cast a split card, you may cast either half or, if that split card " +
                "has fuse, both halves."
        )
        ruling(
            "2017-02-09",
            "If you control no creatures with power greater than 0 as Rishkar's Expertise resolves, you draw no " +
                "cards, but you may cast a card with mana value 5 or less from your hand without paying its mana cost."
        )
        ruling(
            "2017-02-09",
            "You may cast one of the cards drawn by Rishkar's Expertise's first effect while performing its second effect."
        )
        ruling("2017-02-09", "The greatest power among creatures you control is determined as Rishkar's Expertise resolves.")
        ruling(
            "2017-02-09",
            "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without " +
                "paying its mana cost."
        )
        ruling("2017-02-09", "Effects that allow you to \"cast\" a card don't allow you to play a land card.")
        ruling(
            "2017-02-09",
            "While you're casting your free spell, the Expertise spell is still on the stack. It will be put into " +
                "its owner's graveyard after the free spell is cast. The free spell can't target the Expertise " +
                "card in your graveyard. It can target the Expertise spell on the stack, but the Expertise spell " +
                "will become an illegal target before the free spell resolves."
        )
        ruling(
            "2017-02-09",
            "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative " +
                "costs, such as emerge costs. You can, however, pay additional costs. If the card has any " +
                "mandatory additional costs, such as that of Cathartic Reunion, you must pay those to cast the card."
        )
    }
}
