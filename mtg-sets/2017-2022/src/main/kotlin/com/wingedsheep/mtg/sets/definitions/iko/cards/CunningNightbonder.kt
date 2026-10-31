package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Cunning Nightbonder
 * {U/B}{U/B}
 * Creature — Human Rogue
 * 2/2
 * Flash
 * Spells with flash you cast cost {1} less to cast and can't be countered.
 *
 * Thryx, the Sudden Storm's shape with a flash filter instead of a mana-value one:
 * - The discount is a [ModifySpellCost] over spells you cast that have flash.
 * - The "can't be countered" half is a [GrantCantBeCountered] over spells with flash you control
 *   that were cast (`cast()`), so a copy or a stolen spell isn't protected (ruling). It applies to
 *   such a spell already on the stack when Nightbonder enters, though that spell got no discount.
 */
val CunningNightbonder = card("Cunning Nightbonder") {
    manaCost = "{U/B}{U/B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "Flash\nSpells with flash you cast cost {1} less to cast and can't be countered."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withKeyword(Keyword.FLASH)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    staticAbility {
        ability = GrantCantBeCountered(
            filter = GameObjectFilter.Any.withKeyword(Keyword.FLASH).youControl().cast()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Ekaterina Burmak"
        flavorText = "\"We are each other's shadow, fleeting yet always present.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adb202e7-f9a4-4785-840d-18aa6aa663b4.jpg?1783931012"
        ruling("2020-04-17", "Spells with flash that you control but didn't cast (perhaps because they're copies of a spell or because you took control of them from another player) can be countered.")
        ruling("2020-04-17", "Cunning Nightbonder's last ability doesn't apply to itself while it's a spell.")
        ruling("2020-04-17", "Cunning Nightbonder's cost reduction applies only to generic mana in a spell's cost. It can't reduce a spell's colored mana requirements. For example, your first Cunning Nightbonder won't normally reduce the cost of your second since it has no generic mana in its cost.")
        ruling("2020-04-17", "A spell or ability that counters spells can still target spells with flash you cast. When that spell or ability resolves, your spell won't be countered, but any additional effects of the countering spell or ability will still happen.")
        ruling("2020-04-17", "If Cunning Nightbonder enters the battlefield while a spell with flash that you cast is on the stack, that spell can't be countered, even though you don't get the {1} discount.")
    }
}
