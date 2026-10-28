package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Neriv, Crackling Vanguard — Tarkir: Dragonstorm Commander #6
 * {2}{R}{W}{B} · Legendary Creature — Spirit Dragon · 4/4
 *
 * Flying, deathtouch
 * When Neriv enters, create two 1/1 red Goblin creature tokens.
 * Whenever Neriv attacks, exile a number of cards from the top of your library equal to the number
 * of differently named tokens you control. During any turn you attacked with a commander, you may
 * play those cards.
 *
 * The count is `distinctNames()` over the tokens you control, read as the trigger resolves. The
 * exiled cards get a lasting play permission ([MayPlayExpiry.Permanent], the Goblin Researcher
 * shape) gated on [Conditions.YouAttackedWithCreaturesThisTurn] over commanders — re-evaluated on
 * every play query against the per-turn record of the attackers you declared. Being a commander
 * is an attribute of the card that survives zone changes, so the gate stays open for the rest of
 * a turn in which your commander attacked even after it died or left combat; tokens put onto the
 * battlefield attacking were never declared and don't count.
 */
val NerivCracklingVanguard = card("Neriv, Crackling Vanguard") {
    manaCost = "{2}{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Legendary Creature — Spirit Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying, deathtouch\n" +
        "When Neriv enters, create two 1/1 red Goblin creature tokens.\n" +
        "Whenever Neriv attacks, exile a number of cards from the top of your library equal to the " +
        "number of differently named tokens you control. During any turn you attacked with a " +
        "commander, you may play those cards."

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/e/2/e265ca24-96c0-4654-a8f3-bbffe288970a.jpg?1783906784"
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val exiled = gather(
                CardSource.TopOfLibrary(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Token).distinctNames()
                )
            )
            exile(exiled)
            run(
                Effects.GrantMayPlayFromExile(
                    from = exiled,
                    expiry = MayPlayExpiry.Permanent,
                    condition = Conditions.YouAttackedWithCreaturesThisTurn(
                        filter = GameObjectFilter.Any.commander(),
                        atLeast = 1
                    ),
                )
            )
        }
        description = "Whenever Neriv attacks, exile a number of cards from the top of your library " +
            "equal to the number of differently named tokens you control. During any turn you " +
            "attacked with a commander, you may play those cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac552ea3-f521-4c42-a846-21a73364e0ca.jpg?1783907190"
        ruling("2025-04-04", "The effect that creates a token may set the name of that token. A token that's a copy of something else has the name of the object it's a copy of, unless otherwise specified. All other tokens have a name that matches the subtypes of the token that was created plus the word \"Token.\" For example, an effect that says \"Create a 1/1 green Elf creature token\" creates a token that's name is \"Elf Token.\"")
        ruling("2025-04-04", "To determine the number of differently named tokens you control, count each token you control once, but only if its English name isn't the same as another token you've already counted this way. For example, if you control two tokens named \"Elf Token\" and one token named \"Goblin Token,\" you exile two cards from the top of your library.")
    }
}
