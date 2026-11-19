package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Eidolon of Countless Battles — one bonus, read twice: on its own body (meaningful only while it's
 * a creature) and on the creature it enchants once bestowed. A bestowed Eidolon is an Aura and not
 * a creature, so it contributes exactly +1/+1 to its own count either way.
 */
val EidolonOfCountlessBattles = card("Eidolon of Countless Battles") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Spirit"
    power = 0
    toughness = 0
    oracleText = "Bestow {2}{W}{W} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "This creature and enchanted creature each get +1/+1 for each creature you control and +1/+1 for each Aura you control."

    keywordAbility(KeywordAbility.bestow("{2}{W}{W}"))

    val bonus = DynamicAmounts.creaturesYouControl() +
        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Enchantment.withSubtype("Aura")).count()

    staticAbility {
        ability = GrantDynamicStats(filter = GroupFilter.source(), powerBonus = bonus, toughnessBonus = bonus)
    }
    staticAbility {
        ability = GrantDynamicStats(filter = GroupFilter.attachedCreature(), powerBonus = bonus, toughnessBonus = bonus)
    }

    metadata {
        ruling("2014-02-01", "A permanent with bestow is either a creature or an Aura, not both (although it's an enchantment either way). It will contribute just +1/+1 toward the bonus given by Eidolon of Countless Battles.")
        ruling("2014-02-01", "Eidolon of Countless Battles's last ability functions only on the battlefield. In other zones, it is a 0/0 enchantment creature card.")
        ruling("2013-09-15", "On the stack, a spell with bestow is either a creature spell or an Aura spell. It's never both, although it's an enchantment spell in either case.")
        ruling("2013-09-15", "Auras attached to a creature don't become tapped when the creature becomes tapped. Except in some rare cases, an Aura with bestow remains untapped when it becomes unattached and becomes a creature.")
        ruling("2013-09-15", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        ruling("2013-09-15", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2013-09-15", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d16283b-b5bb-48e8-8c97-db5ad74fd544.jpg?1783939583"
    }
}
