package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Haktos the Unscarred
 * {R}{R}{W}{W}
 * Legendary Creature — Human Warrior
 * 6/1
 * Haktos attacks each combat if able.
 * As Haktos enters, choose 2, 3, or 4 at random.
 * Haktos has protection from each mana value other than the chosen number.
 *
 * Lavabrink Venturer's shape: an as-enters `ChoiceType.NUMBER` replacement — drawn `atRandom`, so
 * nobody is asked — writes the number, and one conditional self-grant per possible number turns on
 * `ProtectionScope.ManaValueOtherThan`. With no number recorded no grant is live, as the ruling says.
 */
val HaktosTheUnscarred = card("Haktos the Unscarred") {
    manaCost = "{R}{R}{W}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Warrior"
    power = 6
    toughness = 1
    oracleText = "Haktos attacks each combat if able.\n" +
        "As Haktos enters, choose 2, 3, or 4 at random.\n" +
        "Haktos has protection from each mana value other than the chosen number."

    staticAbility {
        ability = MustAttack()
    }

    replacementEffect(
        EntersWithChoice(ChoiceType.NUMBER, minValue = 2, maxValue = 4, atRandom = true)
    )

    for (number in 2..4) {
        staticAbility {
            condition = Conditions.CastChoiceIs(ChoiceSlot.CHOSEN_NUMBER, number.toString())
            ability = GrantProtectionFromToGroup(ProtectionScope.ManaValueOtherThan(number), GroupFilter.source())
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "218"
        artist = "Ryan Pancoast"
        flavorText = "\"Many have sought my weakness. All have failed.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6fe5cbef-0f4e-4d05-9a94-4739e09d7a9c.jpg?1783931521"
        ruling("2020-01-24", "If Haktos can't attack for any reason (such as being tapped or having come under that player's control that turn), then it doesn't attack. If there's a cost associated with having it attack, you aren't forced to pay that cost, so it doesn't have to attack in that case either.")
        ruling("2020-01-24", "Haktos has protection from each possible mana value other than the chosen value, not just protection from the two options that weren't randomly chosen.")
        ruling("2020-01-24", "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value. If a card or permanent in any other zone has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-01-24", "The mana value of a token that isn't a copy of another object is 0. A token that is a copy of another object has the same mana value as that object.")
        ruling("2020-01-24", "A number is chosen randomly for Haktos before it enters the battlefield. There's no point at which players can target it before it has gained the appropriate protection abilities.")
        ruling("2020-01-24", "If Haktos somehow has no choice made for its ability, its last ability grants it no protection abilities.")
    }
}
