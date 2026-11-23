package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Death's Approach
 * {B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets -X/-X, where X is the number of creature cards in its controller's graveyard.
 *
 * "Its controller" is the *enchanted creature's* controller, so the count reads
 * [Player.ControllerOfAffectedEntity] (as So Tiny and Disturbing Conversion do) and follows a
 * control change.
 */
val DeathsApproach = card("Death's Approach") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets -X/-X, where X is the number of creature cards in its controller's graveyard."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        val creatureCards = DynamicAmounts.creatureCardsInYourGraveyard(Player.ControllerOfAffectedEntity)
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = -creatureCards,
            toughnessBonus = -creatureCards
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Terese Nielsen"
        flavorText = "\"It's cold in the grave, and the dead crave your warmth. Who are you to deny them?\"\n—Strava, Dimir mage"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0f69c6f-522d-42c8-be7c-ea6d7ffb6a90.jpg?1783940131"
        ruling(
            "2013-01-24",
            "The value of X will change as the number of creature cards in the enchanted creature's controller's graveyard changes. Any time state-based actions are performed, if that creature's toughness is 0 or less, or damage marked on it is equal to or greater than its toughness, the creature will be put into its owner's graveyard."
        )
    }
}
