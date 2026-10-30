package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Swift Reconfiguration — Neon Dynasty Commander #10 (canonical printing)
 * {W} · Enchantment — Aura
 *
 * Flash
 * Enchant creature or Vehicle
 * Enchanted permanent is a Vehicle artifact with crew 5 and it loses all other card types.
 * (It's not a creature unless it's crewed.)
 *
 * The Sugar Coat shape: a layer-4 [TransformPermanent] sets the card types to just artifact and
 * the subtypes to just Vehicle (an Equipment loses its Equipment subtype, a creature its creature
 * types), and a [GrantKeyword] adds a crew 5 ability beside any crew it already had. Abilities
 * are kept. Crewing it later is a newer type-changing effect, so it becomes an artifact creature
 * again with its own printed power and toughness until end of turn; a creature that stops being
 * one while attacking or blocking is removed from combat by the state-based check.
 */
val SwiftReconfiguration = card("Swift Reconfiguration") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature or Vehicle\n" +
        "Enchanted permanent is a Vehicle artifact with crew 5 and it loses all other card types. " +
        "(It's not a creature unless it's crewed.)"

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))

    staticAbility {
        ability = TransformPermanent(
            setCardTypes = setOf("ARTIFACT"),
            setSubtypes = setOf(Subtype.VEHICLE.value),
        )
    }

    staticAbility {
        ability = GrantKeyword(KeywordAbility.grantedCrew(5))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Nicholas Gregory"
        flavorText = "On the bright side, the bear now had cupholders."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/975dcfab-0281-4fee-92aa-021ea6c524c7.jpg?1783923997"
        ruling(
            "2022-02-18",
            "The permanent keeps all of its abilities. Notably, if Swift Reconfiguration enchants a " +
                "Vehicle, that Vehicle gains crew 5 in addition to any crew abilities it already had."
        )
        ruling(
            "2022-02-18",
            "If the enchanted permanent was attacking or blocking (or being attacked if it's a " +
                "planeswalker) when Swift Reconfiguration becomes attached to it, that permanent is " +
                "removed from combat."
        )
        ruling(
            "2022-02-18",
            "If Swift Reconfiguration becomes attached to a creature that is also an Equipment, that " +
                "permanent becomes a Vehicle and loses the Equipment subtype. If it is attached to a " +
                "creature, it becomes unattached from that creature."
        )
    }
}
