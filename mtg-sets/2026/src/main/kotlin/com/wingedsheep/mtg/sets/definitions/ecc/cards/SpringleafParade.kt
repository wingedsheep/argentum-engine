package com.wingedsheep.mtg.sets.definitions.ecc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
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
 * Springleaf Parade
 * {X}{G}{G}
 * Enchantment
 * When this enchantment enters, create X 1/1 colorless Shapeshifter creature tokens with
 * changeling. (They're every creature type.)
 * Creature tokens you control have "{T}: Add one mana of any color."
 *
 * The ETB reads the cast X through [DynamicAmounts.xValue] (stamped on the enters event, as for
 * Farmer Cotton). The mana grant is [InsidiousRoots]'s Cryptolith Rite shape narrowed to tokens;
 * it covers every creature token you control, not only the Shapeshifters this made.
 */
val SpringleafParade = card("Springleaf Parade") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create X 1/1 colorless Shapeshifter creature tokens with " +
        "changeling. (They're every creature type.)\n" +
        "Creature tokens you control have \"{T}: Add one mana of any color.\""

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.xValue(),
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Shapeshifter"),
            keywords = setOf(Keyword.CHANGELING),
            imageUri = "https://cards.scryfall.io/normal/front/c/2/c2963ce1-f9d8-437a-9489-e0913a8b8d26.jpg?1783904328",
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddManaOfChoice(),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
            ),
            filter = GroupFilter(GameObjectFilter.Creature.token().youControl()),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Cory Godbey"
        flavorText = "\"Come into the light, strange one. All are welcome to bask in Eirdu's glow.\"\n—Kithkin rite-leader"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44265489-645a-43b0-bc1f-726905b06876.jpg?1783904601"
    }
}
