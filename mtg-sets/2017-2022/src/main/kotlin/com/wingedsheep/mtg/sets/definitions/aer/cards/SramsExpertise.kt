package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sram's Expertise — Aether Revolt #24 (canonical / earliest real printing, 2017)
 * {2}{W}{W} · Sorcery
 *
 * Create three 1/1 colorless Servo artifact creature tokens.
 * You may cast a spell with mana value 3 or less from your hand without paying its mana cost.
 *
 * - Three Servos via [Effects.CreateToken] (`artifactToken = true`); their art comes from the set's
 *   `tokenArt` Servo entry.
 * - The Expertise rider is Coveted Prize / Electrodominance's free-cast hand pipeline: gather the
 *   nonland cards in your hand ("a spell" — a land can't be cast), keep mana value 3 or less, choose
 *   up to one ("you may"), and cast it during this spell's resolution without paying its mana cost,
 *   so card-type timing is ignored. The Servos are already on the battlefield by then.
 */
val SramsExpertise = card("Sram's Expertise") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create three 1/1 colorless Servo artifact creature tokens.\nYou may cast a spell with " +
        "mana value 3 or less from your hand without paying its mana cost."

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            count = 3,
            artifactToken = true,
        ) then Effects.Pipeline {
            val inHand = gather(CardSource.FromZone(Zone.HAND, Player.You, filter = GameObjectFilter.Nonland))
            val candidates = filter(inHand, GameObjectFilter.Any.manaValueAtMost(3))
            val chosen = chooseUpTo(1, from = candidates, selectedLabel = "Cast without paying its mana cost")
            run(Effects.CastFromCollectionWithoutPayingCost(chosen))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Kieran Yanner"
        flavorText = "\"It's important to always have the right tool for the job. This one is for picking my teeth.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c6d47b0-4c19-4c19-9e21-54cd87a5e34d.jpg?1783936776"
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
