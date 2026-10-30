package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dawn Evangel
 * {2}{W}
 * Enchantment Creature — Human Cleric
 * 2/3
 * Whenever a creature dies, if an Aura you controlled was attached to it, return target creature
 * card with mana value 2 or less from your graveyard to your hand.
 *
 * The intervening "if" counts the Auras you controlled on the dead creature from last-known
 * information ([DynamicAmounts.aurasAttachedToTriggering]) — the unattached-Aura state-based action
 * has already put them in the graveyard, where they are their owners' rather than yours.
 */
val DawnEvangel = card("Dawn Evangel") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Human Cleric"
    power = 2
    toughness = 3
    oracleText = "Whenever a creature dies, if an Aura you controlled was attached to it, return target " +
        "creature card with mana value 2 or less from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.aurasAttachedToTriggering(Player.You),
            ComparisonOperator.GTE,
            1,
        )
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(2))
        effect = Effects.ReturnToHand(creatureCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Livia Prima"
        flavorText = "Is there a greater promise than the glory of the sunrise?"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7e152ae-43ec-4b9d-a43f-786c2f6c7366.jpg?1783931601"
        ruling("2020-01-24", "Dawn Evangel's ability can target the card whose death caused the ability to trigger if that card's mana value is 2 or less.")
        ruling("2020-01-24", "Dawn Evangel's ability triggers when any player's creature dies, so long as that creature had an Aura you controlled attached to it.")
        ruling("2020-01-24", "If Dawn Evangel dies while enchanted by an Aura you control, its ability triggers.")
        ruling("2020-01-24", "If Dawn Evangel dies at the same time as another creature enchanted by an Aura you control, its ability triggers.")
        ruling("2020-01-24", "If a card in a graveyard has {X} in its mana cost, X is considered to be 0.")
    }
}
