package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithToken
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fisher's Talent {2}{G}{U}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * At the beginning of your upkeep, look at the top card of your library. You may reveal it if
 * it's a land card. Create a 1/1 blue Fish creature token if you revealed it this way. Then draw
 * a card.
 * {G}{U}: Level 2
 * If you would create a Fish token, create a 3/3 blue Shark creature token instead.
 * {2}{G}{U}: Level 3
 * If you would create a Shark token, create an 8/8 blue Octopus creature token instead.
 *
 * Levels 2 and 3 are token substitutions that chain (CR 616.2): at level 3 a Fish becomes a Shark,
 * and that Shark becomes an Octopus. They replace any Fish or Shark token you'd create, not only
 * this Class's own.
 */
val FishersTalent = card("Fisher's Talent") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "At the beginning of your upkeep, look at the top card of your library. You may reveal it if " +
        "it's a land card. Create a 1/1 blue Fish creature token if you revealed it this way. Then " +
        "draw a card.\n" +
        "{G}{U}: Level 2\n" +
        "If you would create a Fish token, create a 3/3 blue Shark creature token instead.\n" +
        "{2}{G}{U}: Level 3\n" +
        "If you would create a Shark token, create an 8/8 blue Octopus creature token instead."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            // Look at the top card — it stays on top either way.
            val looked = gather(CardSource.TopOfLibrary(1))
            // "You may reveal it if it's a land card": only a land can be chosen; choosing is the reveal.
            val revealed = chooseUpTo(
                1,
                from = looked,
                filter = GameObjectFilter.Land,
                showAllCards = true,
                prompt = "You may reveal the top card of your library if it's a land card",
                selectedLabel = "Reveal"
            )
            reveal(revealed, revealToSelf = false)
            run(Effects.If(
                condition = whenMatches(revealed, GameObjectFilter.Land),
                then = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Fish"),
                    imageUri = "https://cards.scryfall.io/normal/front/d/e/de0d6700-49f0-4233-97ba-cef7821c30ed.jpg?1721431109"
                ),
            ))
            run(Effects.DrawCards(1))
        }
        description = "At the beginning of your upkeep, look at the top card of your library. You may " +
            "reveal it if it's a land card. Create a 1/1 blue Fish creature token if you revealed it " +
            "this way. Then draw a card."
    }

    classLevel(2, "{G}{U}") {
        replacementEffect(
            ReplaceTokenCreationWithToken(
                tokens = listOf(Effects.CreateToken(
                    power = 3,
                    toughness = 3,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Shark"),
                    imageUri = "https://cards.scryfall.io/normal/front/f/9/f9424ef2-d271-4929-83e0-12775420bac3.jpg?1783909757"
                )),
                appliesTo = EventPattern.TokenCreationEvent(
                    controller = Player.You,
                    tokenFilter = GameObjectFilter.Any.withSubtype(Subtype.FISH)
                )
            )
        )
    }

    classLevel(3, "{2}{G}{U}") {
        replacementEffect(
            ReplaceTokenCreationWithToken(
                tokens = listOf(Effects.CreateToken(
                    power = 8,
                    toughness = 8,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Octopus"),
                    imageUri = "https://cards.scryfall.io/normal/front/9/f/9fc10475-5eaf-47ea-a45c-3948a632f365.jpg?1783909757"
                )),
                appliesTo = EventPattern.TokenCreationEvent(
                    controller = Player.You,
                    tokenFilter = GameObjectFilter.Any.withSubtype(Subtype.SHARK)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Allen Douglas"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47f0e0b9-d137-46ac-a6e4-13a529e30bb5.jpg?1783910727"
        ruling("2024-07-26", "You don't have to reveal the card if it's a land card. (Maybe you're not in the mood for fish. That's okay.)")
        ruling("2024-07-26", "The level 2 and level 3 class abilities of Fisher's Talent will replace any Fish or Shark tokens you create, not just ones created by the abilities of Fisher's Talent.")
        ruling("2024-07-26", "Gaining a level won't remove abilities that a Class had at a previous level.")
    }
}
