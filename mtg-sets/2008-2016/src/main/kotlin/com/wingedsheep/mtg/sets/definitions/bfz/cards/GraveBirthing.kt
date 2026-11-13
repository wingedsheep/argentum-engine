package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Grave Birthing
 * {2}{B}
 * Instant
 * Devoid (This card has no color.)
 * Target opponent exiles a card from their graveyard. You create a 1/1 colorless Eldrazi Scion
 * creature token. It has "Sacrifice this token: Add {C}."
 * Draw a card.
 *
 * The exile is Scrabbling Claws' shape: Gather → Select → Move over the target opponent's
 * graveyard with [Chooser.TargetPlayer], so the opponent picks the card as the spell resolves.
 * An empty graveyard gathers nothing and the selection is skipped, but the Scion and the draw
 * still happen; if the opponent becomes an illegal target the spell fizzles and does nothing.
 * The Scion token is Call the Scions' inline shape.
 */
val GraveBirthing = card("Grave Birthing") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Target opponent exiles a card from their graveyard. You create a 1/1 colorless Eldrazi " +
        "Scion creature token. It has \"Sacrifice this token: Add {C}.\"\n" +
        "Draw a card."

    keywords(Keyword.DEVOID)

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, opponent.asPlayer))
            val chosen = chooseExactly(
                1,
                from = graveyard,
                chooser = Chooser.TargetPlayer,
                prompt = "Exile a card from your graveyard"
            )
            exile(chosen, owner = opponent.asPlayer)
        } then Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Scion"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = Costs.SacrificeSelf,
                    effect = Effects.AddColorlessMana(1),
                    isManaAbility = true,
                    timing = TimingRule.ManaAbility,
                    descriptionOverride = "Sacrifice this token: Add {C}.",
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/b/9/b999a0fe-d2d0-4367-9abb-6ce5f3764f19.jpg?1783938121",
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e996a30-3a4d-4a26-8386-f9eb9c999dc0.jpg?1783938205"
        ruling(
            "2015-08-25",
            "The target opponent chooses which card to exile as Grave Birthing resolves."
        )
        ruling(
            "2015-08-25",
            "You can cast Grave Birthing targeting an opponent with an empty graveyard. Even though " +
                "that player won't exile a card from their graveyard, you'll still get an Eldrazi Scion " +
                "and draw a card."
        )
        ruling(
            "2015-08-25",
            "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. " +
                "It doesn't use the stack and can't be responded to."
        )
    }
}
