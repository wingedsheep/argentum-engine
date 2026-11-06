package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Baral's Expertise — Aether Revolt #29 (canonical / earliest real printing, 2017)
 * {3}{U}{U} · Sorcery
 *
 * Return up to three target artifacts and/or creatures to their owners' hands.
 * You may cast a spell with mana value 4 or less from your hand without paying its mana cost.
 *
 * - "Up to three target artifacts and/or creatures" is one optional three-slot requirement over
 *   artifact-or-creature, and the bounce runs once per chosen target ([Effects.ForEachTarget]) so an
 *   illegal target on resolution doesn't take the others with it (Into the Void's shape).
 * - The Expertise rider is Sram's Expertise's free-cast hand pipeline with mana value 4: gather the
 *   nonland cards in your hand, keep mana value 4 or less, choose up to one ("you may"), and cast it
 *   during this spell's resolution without paying its mana cost. The hand is gathered after the
 *   bounce, so a card this spell just returned to your hand can be the free spell (per the rulings).
 */
val BaralsExpertise = card("Baral's Expertise") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return up to three target artifacts and/or creatures to their owners' hands.\nYou may cast " +
        "a spell with mana value 4 or less from your hand without paying its mana cost."

    spell {
        targets(TargetFilter.CreatureOrArtifact, count = 3, optional = true)
        effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0))) then Effects.Pipeline {
            val inHand = gather(CardSource.FromZone(Zone.HAND, Player.You, filter = GameObjectFilter.Nonland))
            val candidates = filter(inHand, GameObjectFilter.Any.manaValueAtMost(4))
            val chosen = chooseUpTo(1, from = candidates, selectedLabel = "Cast without paying its mana cost")
            run(Effects.CastFromCollectionWithoutPayingCost(chosen))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Todd Lockwood"
        flavorText = "\"Get out of my way.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d273f5b7-b3a3-485a-acc8-34e10a504646.jpg?1783936775"
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
        ruling(
            "2017-02-09",
            "The card that you cast may be an artifact or creature that was returned to your hand by this spell."
        )
        ruling(
            "2017-02-09",
            "If the Expertise spell you cast has any targets, and those targets become illegal before the spell " +
                "resolves, the spell doesn't resolve and none of its effects happen. You won't get to cast a free spell."
        )
        ruling("2017-02-09", "Effects that allow you to \"cast\" a card don't allow you to play a land card.")
        ruling(
            "2017-02-09",
            "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative " +
                "costs, such as emerge costs. You can, however, pay additional costs. If the card has any " +
                "mandatory additional costs, such as that of Cathartic Reunion, you must pay those to cast the card."
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
    }
}
