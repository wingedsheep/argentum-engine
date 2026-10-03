package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Path of Annihilation — Modern Horizons 3 #165 (uncommon)
 * {3}{G} · Enchantment
 *
 * Devoid
 * When this enchantment enters, create two 0/1 colorless Eldrazi Spawn creature tokens with
 * "Sacrifice this token: Add {C}."
 * Eldrazi you control have "{T}: Add one mana of any color."
 * Whenever you cast a creature spell with mana value 7 or greater, you gain 4 life.
 */
val PathOfAnnihilation = card("Path of Annihilation") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Devoid (This card has no color.)\n" +
        "When this enchantment enters, create two 0/1 colorless Eldrazi Spawn creature tokens with " +
        "\"Sacrifice this token: Add {C}.\"\n" +
        "Eldrazi you control have \"{T}: Add one mana of any color.\"\n" +
        "Whenever you cast a creature spell with mana value 7 or greater, you gain 4 life."

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateEldraziSpawn(2)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Eldrazi").youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.manaValueAtLeast(7))
        effect = Effects.GainLife(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Ben Hill"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28493c98-a11c-4bcc-bf55-412c884ffa7a.jpg?1783911258"
    }
}
