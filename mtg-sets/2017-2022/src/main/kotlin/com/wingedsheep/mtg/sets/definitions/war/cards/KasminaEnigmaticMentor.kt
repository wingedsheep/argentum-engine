package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kasmina, Enigmatic Mentor
 * {3}{U}
 * Legendary Planeswalker — Kasmina
 * Starting Loyalty: 5
 *
 * Spells your opponents cast that target a creature or planeswalker you control cost {2} more to cast.
 * −2: Create a 2/2 blue Wizard creature token. Draw a card, then discard a card.
 */
val KasminaEnigmaticMentor = card("Kasmina, Enigmatic Mentor") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Kasmina"
    startingLoyalty = 5
    oracleText = "Spells your opponents cast that target a creature or planeswalker you control cost {2} more to cast.\n" +
        "−2: Create a 2/2 blue Wizard creature token. Draw a card, then discard a card."

    // The tax applies once however many of your creatures/planeswalkers the spell targets.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCastTargeting(
                GroupFilter(GameObjectFilter.CreatureOrPlaneswalker.youControl())
            ),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    // −2: Create a 2/2 blue Wizard creature token. Draw a card, then discard a card.
    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Wizard"),
            imageUri = "https://cards.scryfall.io/normal/front/7/b/7b0b95ce-4821-4955-a27c-93471240f54b.jpg?1783933354"
        ) then Patterns.Hand.loot()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c77d9454-a78c-4063-ad66-46b2f5a030aa.jpg?1783933460"

        ruling("2019-05-03", "To determine the total cost of an opponent's spell that targets a creature or planeswalker you control, start with the mana cost or alternative cost that player is paying, add any cost increases (such as that of Kasmina's effect), then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
        ruling("2019-05-03", "Spells that target more than one creature and/or planeswalker you control cost only {2} more to cast.")
        ruling("2019-05-03", "You can't do anything in between drawing a card and discarding a card, including casting the card you drew.")
    }
}
