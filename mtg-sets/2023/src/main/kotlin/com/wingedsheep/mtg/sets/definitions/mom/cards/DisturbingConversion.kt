package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Disturbing Conversion
 * {1}{U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * When this Aura enters, each player mills two cards.
 * Enchanted creature gets -X/-0, where X is the number of cards in its controller's graveyard.
 *
 * Fear of Death with the count keyed to the *enchanted creature's* controller:
 * [Player.ControllerOfAffectedEntity] reads the controller of the permanent the static is
 * modifying, so enchanting an opponent's creature counts their graveyard, not yours.
 */
val DisturbingConversion = card("Disturbing Conversion") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When this Aura enters, each player mills two cards.\n" +
        "Enchanted creature gets -X/-0, where X is the number of cards in its controller's graveyard."

    keywords(Keyword.FLASH)
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline { mill(2, Player.Each) }
    }

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = -DynamicAmounts.cardsInYourGraveyard(Player.ControllerOfAffectedEntity),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Anna Mitura-Laskowska"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a63d530-27c3-4cd7-ac37-c62b5a1afbc1.jpg?1783917039"
    }
}
