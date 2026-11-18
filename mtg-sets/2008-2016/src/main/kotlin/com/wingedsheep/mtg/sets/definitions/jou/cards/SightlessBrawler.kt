package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sightless Brawler — "can't attack alone" twice: on its own body while it's a creature, and on the
 * creature it enchants once bestowed (read off the Aura through `GroupFilter.attachedCreature()`).
 */
val SightlessBrawler = card("Sightless Brawler") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Human Warrior"
    power = 3
    toughness = 2
    oracleText = "Bestow {4}{W} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "This creature can't attack alone.\n" +
        "Enchanted creature gets +3/+2 and can't attack alone."

    keywordAbility(KeywordAbility.bestow("{4}{W}"))

    staticAbility {
        ability = CantAttackUnlessCoAttacker(coAttackerFilter = GameObjectFilter.Creature)
    }
    staticAbility { ability = ModifyStats(3, 2) }
    staticAbility {
        ability = CantAttackUnlessCoAttacker(
            coAttackerFilter = GameObjectFilter.Creature,
            filter = GroupFilter.attachedCreature(),
        )
    }

    metadata {
        ruling("2014-04-26", "Sightless Brawler or the creature it enchants can be declared as an attacker only if another creature is declared as an attacker at the same time.")
        ruling("2014-04-26", "If you control more than one creature that can't attack alone, they can both attack together, even if no other creatures attack.")
        ruling("2014-04-26", "Although Sightless Brawler or the creature it enchants can't attack alone, other attacking creatures don't have to attack the same player or planeswalker. For example, Sightless Brawler could attack an opponent and another creature could attack a planeswalker that opponent controls.")
        ruling("2014-04-26", "If a creature that can't attack alone also must attack if able, its controller must attack with it and another creature if able.")
        ruling("2014-04-26", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "24"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e2fcd27-c068-468f-9825-2825c0cf217d.jpg?1783939456"
    }
}
