package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.scavengeAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Varolz, the Scar-Striped
 * {1}{B}{G}
 * Legendary Creature — Troll Warrior
 * 2/2
 *
 * Each creature card in your graveyard has scavenge. The scavenge cost is equal to its mana cost.
 * Sacrifice another creature: Regenerate Varolz.
 *
 * One granted scavenge ability whose cost is each card's own mana cost (`scavengeAbility()` with
 * no cost is `AbilityCost.OwnManaCost`), as on Young Deathclaws.
 */
val VarolzTheScarStriped = card("Varolz, the Scar-Striped") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Troll Warrior"
    power = 2
    toughness = 2
    oracleText = "Each creature card in your graveyard has scavenge. The scavenge cost is equal to its mana cost. " +
        "(Exile a creature card from your graveyard and pay its mana cost: Put a number of +1/+1 counters " +
        "equal to that card's power on target creature. Scavenge only as a sorcery.)\n" +
        "Sacrifice another creature: Regenerate Varolz."

    staticAbility {
        ability = GrantActivatedAbility(
            scavengeAbility(),
            GroupFilter(GameObjectFilter.Creature.ownedByYou()),
            recipientZone = Zone.GRAVEYARD,
        )
    }

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.Regenerate(EffectTarget.Self)
        description = "Sacrifice another creature: Regenerate Varolz."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c3ae3db-c14a-4ffc-805c-a3a51da9370d.jpg?1783940019"
        ruling("2013-04-15", "Exiling the creature card with scavenge is part of the cost of activating the scavenge ability. Once the ability is activated and the cost is paid, it's too late to stop the ability from being activated by trying to remove the creature card from the graveyard.")
        ruling("2013-04-15", "While Varolz, the Scar-Striped is on the battlefield, the scavenge cost of a creature card in your graveyard is equal to its mana cost, including any colored mana requirements.")
        ruling("2013-04-15", "Abilities that reduce the cost to cast a creature spell or alternative costs you can pay rather than a card's mana cost don't apply to activating a card's scavenge ability.")
        ruling("2013-04-15", "The number of counters that a card's scavenge ability puts on a creature is based on the card's power as it last existed in the graveyard.")
        ruling("2013-04-15", "If the creature card you scavenge has {X} in its mana cost, X is 0.")
        ruling("2013-04-15", "If a creature card has multiple instances of scavenge, you can activate either ability (but not both, as the card will be exiled when you activate one of them). Varolz's ability doesn't affect the scavenge cost of a creature's other scavenge ability.")
    }
}
