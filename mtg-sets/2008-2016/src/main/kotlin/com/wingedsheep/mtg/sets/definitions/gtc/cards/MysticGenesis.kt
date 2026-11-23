package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mystic Genesis
 * {2}{G}{U}{U}
 * Instant
 *
 * Counter target spell. Create an X/X green Ooze creature token, where X is that spell's mana value.
 *
 * Spell Swindle's shape: "that spell's mana value" is read while the spell is still on the stack,
 * where an {X} counts as the value chosen for it, so it is stored before the counter and paid out
 * after (once countered, the card is in a graveyard with X at 0). The Ooze is not conditional on the
 * counter succeeding — a spell that can't be countered stays put and you still get the token
 * (ruling 2017-03-14); only an illegal target on resolution stops the whole spell.
 */
val MysticGenesis = card("Mystic Genesis") {
    manaCost = "{2}{G}{U}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Counter target spell. Create an X/X green Ooze creature token, where X is that spell's mana value."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.StoreNumber(SPELL_MANA_VALUE, DynamicAmounts.manaValueOf(spell)) then
            Effects.CounterSpell() then
            Effects.CreateDynamicToken(
                dynamicPower = DynamicAmounts.storedNumber(SPELL_MANA_VALUE),
                dynamicToughness = DynamicAmounts.storedNumber(SPELL_MANA_VALUE),
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Ooze"),
                imageUri = "https://cards.scryfall.io/normal/front/d/1/d1821a53-1799-4162-b0b6-f8ab84a55c30.jpg?1783909274",
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Mike Bierek"
        flavorText = "\"The Simic can grow anything out of a puddle of sludge. I just hope they never join forces with the Golgari.\"\n—Teysa Karlov, Grand Envoy of Orzhov"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae1dd1ac-1a1e-485d-a11f-d1323a69f95e.jpg?1783940104"
        ruling(
            "2017-03-14",
            "If the target spell is an illegal target when Mystic Genesis tries to resolve, Mystic Genesis " +
                "won't resolve and none of its effects will happen. You won't get an Ooze token."
        )
        ruling(
            "2017-03-14",
            "You may target a spell that can't be countered. When Mystic Genesis resolves, the target spell " +
                "will be unaffected, but you'll still get an Ooze token."
        )
    }
}

private const val SPELL_MANA_VALUE = "mysticGenesisManaValue"
