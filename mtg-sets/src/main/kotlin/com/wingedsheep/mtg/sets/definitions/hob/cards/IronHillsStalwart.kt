package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Iron Hills Stalwart
 * {4}{R}
 * Creature — Dwarf Warrior
 * 4/5
 * Reach, trample
 * When this creature enters, attach target Equipment you control to up to one target creature you
 * control.
 *
 * Blacksmith's Talent level 2 on an ETB: [Effects.AttachTargetEquipmentToCreature] takes the two
 * targets explicitly rather than assuming the source is the Equipment. The Equipment target is
 * mandatory, the creature target is "up to one" (`optional = true`) — choosing no creature makes
 * the ability unattach nothing and simply do nothing, which is why the Equipment target can't also
 * be optional.
 */
val IronHillsStalwart = card("Iron Hills Stalwart") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Warrior"
    oracleText = "Reach, trample\n" +
        "When this creature enters, attach target Equipment you control to up to one target creature you control."
    power = 4
    toughness = 5

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val equipment = target(
            "Equipment you control",
            TargetPermanent(
                filter = TargetFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl())
            )
        )
        val creature = target(
            "creature you control",
            TargetPermanent(
                filter = TargetFilter(GameObjectFilter.Creature.youControl()),
                optional = true
            )
        )
        effect = Effects.AttachTargetEquipmentToCreature(equipment, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Michele Giorgi"
        flavorText = "Their fires caught the attention of huge bats flapping and whirring round their ears."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46daa9ac-0ac7-4df9-b9d2-e03ab5b56c72.jpg?1785497126"
    }
}
