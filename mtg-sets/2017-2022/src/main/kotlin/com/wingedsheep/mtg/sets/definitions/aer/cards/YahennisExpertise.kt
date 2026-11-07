package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yahenni's Expertise — Aether Revolt #75 (canonical / earliest real printing, 2017)
 * {2}{B}{B} · Sorcery
 *
 * All creatures get -3/-3 until end of turn.
 * You may cast a spell with mana value 3 or less from your hand without paying its mana cost.
 *
 * - The sweep is Infest's shape: -3/-3 until end of turn on each creature on the battlefield at
 *   resolution ([Effects.ForEachInGroup] over [GroupFilter.AllCreatures]).
 * - The Expertise rider is Sram's Expertise's free-cast hand pipeline: gather the nonland cards in
 *   your hand, keep mana value 3 or less, choose up to one ("you may"), and cast it during this
 *   spell's resolution without paying its mana cost. State-based actions aren't checked until the
 *   whole spell has resolved, so creatures shrunk to 0 toughness are still on the battlefield while
 *   the free spell is cast (per the rulings); a creature cast this way is on the stack, not the
 *   battlefield, when the -3/-3 applies, so it isn't affected.
 */
val YahennisExpertise = card("Yahenni's Expertise") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "All creatures get -3/-3 until end of turn.\nYou may cast a spell with mana value 3 or less " +
        "from your hand without paying its mana cost."

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreatures,
            effect = Effects.ModifyStats(-3, -3, EffectTarget.IterationEntity)
        ) then Effects.Pipeline {
            val inHand = gather(CardSource.FromZone(Zone.HAND, Player.You, filter = GameObjectFilter.Nonland))
            val candidates = filter(inHand, GameObjectFilter.Any.manaValueAtMost(3))
            val chosen = chooseUpTo(1, from = candidates, selectedLabel = "Cast without paying its mana cost")
            run(Effects.CastFromCollectionWithoutPayingCost(chosen))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Daarken"
        flavorText = "\"The Consulate pushed me to my limit, darling, and this is the result.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2f28735-122c-45ba-bde5-decfd9b11b32.jpg?1783936760"
        ruling(
            "2017-04-18",
            "The mana value of a split card is determined by the combined mana cost of its two halves. If an " +
                "expertise spell allows you to cast a split card, you may cast either half or, if that split card " +
                "has fuse, both halves."
        )
        ruling(
            "2017-02-09",
            "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without " +
                "paying its mana cost."
        )
        ruling("2017-02-09", "Effects that allow you to \"cast\" a card don't allow you to play a land card.")
        ruling(
            "2017-02-09",
            "If a creature's toughness is reduced to 0 or less by Yahenni's Expertise, that creature is still on " +
                "the battlefield as you cast your free spell. It can be the target of that spell, although it will " +
                "become an illegal target before that spell resolves. If it has any abilities that trigger on the " +
                "spell being cast, those abilities will trigger."
        )
        ruling(
            "2017-02-09",
            "While you're casting your free spell, the Expertise spell is still on the stack. It will be put into " +
                "its owner's graveyard after the free spell is cast. The free spell can't target the Expertise " +
                "card in your graveyard. It can target the Expertise spell on the stack, but the Expertise spell " +
                "will become an illegal target before the free spell resolves."
        )
        ruling(
            "2017-02-09",
            "Any triggered abilities that trigger while performing the Expertise spell's first effect won't be put " +
                "onto the stack until after you're done casting your free spell. They're put onto the stack at the " +
                "same time as any abilities that triggered while casting that spell regardless of the order in " +
                "which those abilities triggered."
        )
        ruling(
            "2017-02-09",
            "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative " +
                "costs, such as emerge costs. You can, however, pay additional costs. If the card has any " +
                "mandatory additional costs, such as that of Cathartic Reunion, you must pay those to cast the card."
        )
    }
}
