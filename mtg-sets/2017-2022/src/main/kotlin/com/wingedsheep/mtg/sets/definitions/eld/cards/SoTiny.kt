package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * So Tiny
 * {U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature gets -2/-0. It gets -6/-0 instead as long as its controller has seven or more
 * cards in their graveyard.
 *
 * "Its controller" is the *enchanted creature's* controller, so the count reads
 * [Player.ControllerOfAffectedEntity] (as Disturbing Conversion does) and follows a control change.
 *
 * The "-2, or -6 instead at seven or more" step is written arithmetically rather than with
 * `DynamicAmounts.conditional`: `min(1, max(0, graveyard - 6))` is exactly 1 when the graveyard holds
 * seven or more cards and 0 otherwise, so the bonus is `-2 - 4 * step`. The arithmetic nodes thread
 * the projector's intermediate projected state through to the controller lookup, which a
 * `Conditional`'s inner condition evaluation does not.
 */
val SoTiny = card("So Tiny") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature gets -2/-0. It gets -6/-0 instead as long as its controller has seven or more cards in their graveyard."

    keywords(Keyword.FLASH)
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        val graveyard = DynamicAmounts.cardsInYourGraveyard(Player.ControllerOfAffectedEntity)
        val sevenOrMore = DynamicAmounts.min(
            DynamicAmounts.fixed(1),
            DynamicAmounts.nonNegative(graveyard - 6)
        )
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.fixed(-2) - sevenOrMore * 4,
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Randy Vargas"
        flavorText = "His sword sounded like a silver chime on the glass jar, and the sprite laughed with delight."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/421650f2-1b34-4a36-9675-9424997c9d0b.jpg?1783932650"
        ruling(
            "2019-10-04",
            "So Tiny continuously checks the enchanted creature's controller's graveyard to determine just how tiny the enchanted creature is."
        )
    }
}
