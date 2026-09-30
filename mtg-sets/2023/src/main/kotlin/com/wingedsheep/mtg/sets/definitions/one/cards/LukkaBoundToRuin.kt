package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lukka, Bound to Ruin — Phyrexia: All Will Be One #207
 * {2}{R}{R/G/P}{G} · Legendary Planeswalker — Lukka · Starting loyalty 5
 *
 * Compleated
 * +1: Add {R}{G}. Spend this mana only to cast creature spells or activate abilities of creatures.
 * −1: Create a 3/3 green Phyrexian Beast creature token with toxic 1.
 * −4: Lukka deals X damage divided as you choose among any number of target creatures and/or
 *     planeswalkers, where X is the greatest power among creatures you control as you activate this
 *     ability.
 *
 * The −4's X is defined by its own text and fixed as it is activated (CR 107.3c): `xDefinedAs`
 * binds it as the activation's X, so the division announced then (CR 601.2d) and the damage dealt
 * at resolution both read that number — shrinking your best creature in response changes nothing.
 * No more targets than X may be chosen, since each must be assigned at least 1 damage.
 */
val LukkaBoundToRuin = card("Lukka, Bound to Ruin") {
    manaCost = "{2}{R}{R/G/P}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Lukka"
    startingLoyalty = 5
    oracleText = "Compleated ({R/G/P} can be paid with {R}, {G}, or 2 life. If life was paid, this planeswalker enters with two fewer loyalty counters.)\n" +
        "+1: Add {R}{G}. Spend this mana only to cast creature spells or activate abilities of creatures.\n" +
        "−1: Create a 3/3 green Phyrexian Beast creature token with toxic 1.\n" +
        "−4: Lukka deals X damage divided as you choose among any number of target creatures and/or planeswalkers, where X is the greatest power among creatures you control as you activate this ability."

    keywords(Keyword.COMPLEATED)

    loyaltyAbility(+1) {
        val creaturesOnly = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(cardType = CardType.CREATURE, allowAbilities = true)
        effect = Effects.AddMana(Color.RED, restriction = creaturesOnly) then
            Effects.AddMana(Color.GREEN, restriction = creaturesOnly)
    }

    loyaltyAbility(-1) {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Phyrexian", "Beast"),
            numericKeywords = listOf(KeywordAbility.Numeric(Keyword.TOXIC, 1)),
            imageUri = "https://cards.scryfall.io/normal/front/9/1/919381b0-2d23-4794-b4ff-923c23e18196.jpg?1783918168"
        )
    }

    loyaltyAbility(-4) {
        val greatestPower = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        xDefinedAs = greatestPower
        targets(
            TargetFilter(GameObjectFilter.CreatureOrPlaneswalker),
            minCount = 0,
            unlimited = true,
            dynamicMaxCount = greatestPower,
        )
        effect = Effects.DividedDamage(total = 0, dynamicTotal = DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "207"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6df77017-c4a7-4b79-a16b-26463bd6a96a.jpg?1783918000"
        ruling("2023-02-04", "For Lukka's last ability, you announce the targets and how damage is divided as you activate the ability. Once the ability has been activated, the greatest power among creatures you control no longer matters for this ability. This means once you announce you're activating it, no player can respond to try and lower that power and decrease the damage Lukka can deal.")
        ruling("2023-02-04", "You can't choose more targets than the greatest power among creatures you control as you activate the ability, and each chosen target must receive at least 1 damage.")
        ruling("2023-02-04", "If some of the targets of the last ability become illegal, the original division of damage still applies, but the damage that would have been dealt to illegal targets isn't dealt at all.")
        ruling("2023-02-04", "A hybrid Phyrexian mana symbol contributes 1 toward the mana value of a card, even if life is paid for it. Specifically, Lukka's mana value is always 5.")
        ruling("2023-02-04", "The compleated ability looks only at whether a player chose to pay 2 life for a Phyrexian mana symbol as they were casting the spell. If a player paid life for some other reason while casting the spell, that will not reduce the number of loyalty counters the planeswalker enters the battlefield with.")
        ruling("2023-02-04", "Other replacement effects that would change the number of loyalty counters Lukka enters with will apply as normal.")
    }
}
