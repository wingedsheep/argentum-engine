package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DonorCards
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfCards
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Steward of the Harvest
 * {3}{G}
 * Creature — Human Druid
 * 3/3
 * When this creature enters, exile up to three target land cards from your graveyard.
 * Creatures you control have all activated abilities of all land cards exiled with this creature.
 *
 * The ETB exiles each chosen land card into this creature's linked-exile pile
 * (`ExileLinkedToSource` per target); the static lends that pile's activated abilities —
 * including a basic land's intrinsic mana ability (CR 305.6) — to every creature you control,
 * this one included.
 */
val StewardOfTheHarvest = card("Steward of the Harvest") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, exile up to three target land cards from your graveyard.\n" +
        "Creatures you control have all activated abilities of all land cards exiled with this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.Land.ownedByYou(), zone = Zone.GRAVEYARD),
            count = 3,
            optional = true
        )
        effect = Effects.ForEachTarget(Effects.ExileLinkedToSource(EffectTarget.ContextTarget(0)))
    }

    staticAbility {
        ability = HasAllActivatedAbilitiesOfCards(
            donors = DonorCards.LINKED_EXILE,
            cardFilter = Filters.Land,
            receivedBy = GroupFilter.AllCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Marie Magny"
        flavorText = "\"Sleep, and return your energy to the lifepool. Sleep, and enrich the land for the next harvest.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e6ddbf5-e282-48ae-ba86-d15589d96e36.jpg?1783907166"
        ruling(
            "2025-04-04",
            "Creatures you control gain only activated abilities of the exiled land cards. They don't gain triggered abilities, static abilities, or keyword abilities that aren't activated abilities."
        )
        ruling(
            "2025-04-04",
            "If an activated ability of a land card exiled with Steward of the Harvest references the card it's printed on by name, treat the version of that ability on each creature you control as though it referenced the creature that has it by name instead."
        )
        ruling(
            "2025-04-04",
            "Land cards with one or more basic land types have the intrinsic activated ability \"{T}: Add [mana symbol],\" even if the text box doesn't actually contain that text or the card has no text box."
        )
    }
}
