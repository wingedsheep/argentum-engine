package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Esika, God of the Tree // The Prismatic Bridge — Kaldheim #168 (canonical printing)
 * Legendary Creature — God 1/4 // Legendary Enchantment · Modal double-faced card
 *
 * Front: Vigilance; "{T}: Add one mana of any color."; other legendary creatures you control have
 * vigilance and that same mana ability — a [GrantKeyword] and a [GrantActivatedAbility] over
 * other legendary creatures you control.
 *
 * Back: "At the beginning of your upkeep, reveal cards from the top of your library until you
 * reveal a creature or planeswalker card. Put that card onto the battlefield and the rest on the
 * bottom of your library in a random order." With no creature or planeswalker card in the library,
 * the whole library is revealed and goes back on the bottom in a random order (ruling).
 */
private val otherLegendaryCreaturesYouControl =
    GroupFilter(GameObjectFilter.Creature.legendary().youControl(), excludeSelf = true)

private val EsikaGodOfTheTreeFront = card("Esika, God of the Tree") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — God"
    power = 1
    toughness = 4
    oracleText = "Vigilance\n{T}: Add one mana of any color.\n" +
        "Other legendary creatures you control have vigilance and \"{T}: Add one mana of any color.\""

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add one mana of any color."
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.VIGILANCE,
            filter = otherLegendaryCreaturesYouControl
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
                descriptionOverride = "{T}: Add one mana of any color."
            ),
            filter = otherLegendaryCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "168"
        artist = "Johannes Voss"
        flavorText = "\"I know this world, from canopy to roots.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6cd7465-9dd0-473c-ac5e-dd9e2f22f5f6.jpg?1783928221"
        ruling("2021-02-05", "If a creature loses vigilance after it attacks (perhaps because Esika leaves the battlefield), that creature will continue attacking. It won't become tapped.")
    }
}

private val ThePrismaticBridgeBack = card("The Prismatic Bridge") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Enchantment"
    oracleText = "At the beginning of your upkeep, reveal cards from the top of your library until you " +
        "reveal a creature or planeswalker card. Put that card onto the battlefield and the rest on the " +
        "bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val (found, allRevealed) = gatherUntilMatch(GameObjectFilter.CreatureOrPlaneswalker)
            reveal(allRevealed, fromZone = Zone.LIBRARY, toZone = Zone.BATTLEFIELD)
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD))
            toLibraryBottom(exclude(allRevealed, found), order = CardOrder.Random)
        }
        description = "At the beginning of your upkeep, reveal cards from the top of your library until " +
            "you reveal a creature or planeswalker card. Put that card onto the battlefield and the rest " +
            "on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "168"
        artist = "Johannes Voss"
        flavorText = "Starnheim awaits."
        imageUri = "https://cards.scryfall.io/normal/back/f/6/f6cd7465-9dd0-473c-ac5e-dd9e2f22f5f6.jpg?1783928221"
        ruling("2021-02-05", "While resolving the triggered ability of The Prismatic Bridge, if there are no creature or planeswalker cards in your library, you'll simply reveal your entire library, randomize it, and continue playing.")
        ruling("2021-02-05", "If you reveal a double-faced card whose front face is a creature or planeswalker, you'll put it onto the battlefield with its front face up.")
    }
}

val EsikaGodOfTheTree: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = EsikaGodOfTheTreeFront,
    backFace = ThePrismaticBridgeBack,
)
