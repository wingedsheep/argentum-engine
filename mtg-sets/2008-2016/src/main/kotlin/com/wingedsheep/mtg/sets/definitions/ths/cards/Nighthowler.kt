package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nighthowler — a printed 0/0, not a characteristic-defining star/star: the bonus is a battlefield-only
 * static (per the ruling, a 0/0 in every other zone). Eidolon of Countless Battles shape — one count of
 * creature cards in all graveyards ([Player.Each], Lhurgoyf's reading), applied to its own body (only
 * meaningful while it's a creature) and to the creature it enchants once bestowed.
 */
val Nighthowler = card("Nighthowler") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Horror"
    power = 0
    toughness = 0
    oracleText = "Bestow {2}{B}{B} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "This creature and enchanted creature each get +X/+X, where X is the number of creature cards in all graveyards."

    keywordAbility(KeywordAbility.bestow("{2}{B}{B}"))

    val x = DynamicAmounts.count(Player.Each, Zone.GRAVEYARD, GameObjectFilter.Creature)

    staticAbility {
        ability = GrantDynamicStats(filter = GroupFilter.source(), powerBonus = x, toughnessBonus = x)
    }
    staticAbility {
        ability = GrantDynamicStats(filter = GroupFilter.attachedCreature(), powerBonus = x, toughnessBonus = x)
    }

    metadata {
        ruling("2013-09-15", "Nighthowler's last ability functions only from the battlefield. In other zones, Nighthowler is a 0/0 enchantment creature.")
        ruling("2013-09-15", "On the stack, a spell with bestow is either a creature spell or an Aura spell. It's never both, although it's an enchantment spell in either case.")
        ruling("2013-09-15", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2013-09-15", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2013-09-15", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        ruling("2013-09-15", "Auras attached to a creature don't become tapped when the creature becomes tapped. Except in some rare cases, an Aura with bestow remains untapped when it becomes unattached and becomes a creature.")
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2527a490-6c56-41ba-949c-c78905a128ba.jpg?1783939774"
    }
}
