package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Relive the Past
 * {5}{G}{W}
 * Sorcery
 * Return up to one target artifact card, up to one target land card, and up to one target non-Aura
 * enchantment card from your graveyard to the battlefield. They are 5/5 Elemental creatures in
 * addition to their other types.
 */
val ReliveThePast = card("Relive the Past") {
    manaCost = "{5}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Sorcery"
    oracleText = "Return up to one target artifact card, up to one target land card, and up to one " +
        "target non-Aura enchantment card from your graveyard to the battlefield. They are 5/5 " +
        "Elemental creatures in addition to their other types."

    spell {
        target(TargetFilter.ArtifactInYourGraveyard, optional = true)
        target(TargetFilter(GameObjectFilter.Land.ownedByYou(), zone = Zone.GRAVEYARD), optional = true)
        target(
            TargetFilter(
                GameObjectFilter.Enchantment.notSubtype(Subtype.AURA).ownedByYou(),
                zone = Zone.GRAVEYARD,
            ),
            optional = true,
        )
        // One simultaneous return, and each card is already a 5/5 Elemental creature as it enters.
        effect = Effects.Pipeline {
            move(
                gather(CardSource.ChosenTargets),
                CardDestination.ToZone(Zone.BATTLEFIELD),
                entersWith = EntersWithCharacteristics.creature(5, 5, "Elemental"),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Randy Vargas"
        flavorText = "\"Many have tried to subjugate Ravnica. All have failed. All will fail.\"\n" +
            "—Museum of Ravnican History"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20948cd2-e40c-4648-832f-ab0f1cc21610.jpg?1783912839"

        ruling(
            "2024-02-02",
            "An artifact creature, land creature, or enchantment creature returned this way will be " +
                "a 5/5 creature and will be an Elemental in addition to its other creature types.",
        )
        ruling(
            "2024-02-02",
            "An Equipment without reconfigure that's also a creature can't be attached to anything. " +
                "You can activate its equip ability, but it won't become attached.",
        )
        ruling(
            "2024-02-02",
            "If another effect causes one of the returned permanents to become a creature and sets " +
                "its power and toughness as it does so, that creature will have that power and " +
                "toughness; it won't be 5/5. Notably, crewing a Vehicle does not set its power and " +
                "toughness, so a Vehicle will remain a 5/5 creature if you crew it.",
        )
    }
}
