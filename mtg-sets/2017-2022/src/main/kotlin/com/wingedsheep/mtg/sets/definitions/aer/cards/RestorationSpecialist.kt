package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Restoration Specialist
 * {1}{W}
 * Creature — Dwarf Artificer
 * 2/1
 * {W}, Sacrifice this creature: Return up to one target artifact card and up to one target
 * enchantment card from your graveyard to your hand.
 *
 * Two independent optional ("up to one") graveyard target slots — Retrieve's shape on an
 * activated ability with Dedicated Martyr's `{W}` + sacrifice-self cost. Choosing no targets at
 * all is legal (per the ruling), so the ability can be activated just to sacrifice the creature.
 */
val RestorationSpecialist = card("Restoration Specialist") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Artificer"
    power = 2
    toughness = 1
    oracleText = "{W}, Sacrifice this creature: Return up to one target artifact card and up to one " +
        "target enchantment card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.SacrificeSelf)
        val artifact = target(TargetFilter.ArtifactInYourGraveyard, optional = true)
        val enchantment = target(
            TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        effect = Effects.ReturnToHand(artifact) then Effects.ReturnToHand(enchantment)
        description = "{W}, Sacrifice this creature: Return up to one target artifact card and up to one " +
            "target enchantment card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "David Palumbo"
        flavorText = "Dwarves' tools are priceless heirlooms handed down through generations."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63682db3-1a56-4d8e-a9b7-04465a577518.jpg?1783936777"
        ruling(
            "2017-02-09",
            "You can activate Restoration Specialist's ability choosing no targets at all if you wish."
        )
    }
}
